package fr.univtours.info.queries;

import com.alexscode.utilities.collection.Pair;
import com.alexscode.utilities.math.FTest;
import fr.univtours.info.DBUtils;
import fr.univtours.info.Insight;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.optimize.time.CostModelProvider;
import fr.univtours.info.optimize.time.TimeableOp;
import fr.univtours.info.optimize.tsp.Measurable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.dbutils.ResultSetIterator;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.inference.TTest;

import java.sql.*;
import java.util.*;

public class AssessQuery extends Query implements TimeableOp {

    static HashMap<String, String> convivialNames;
    static {
        convivialNames = new HashMap<>();
        convivialNames.put("avg", "Average");
        convivialNames.put("sum", "Sum");
        convivialNames.put("min", "Minima");
        convivialNames.put("max", "Maxima");
        convivialNames.put("stddev", "Standard Deviation");
    }

    Connection conn;
    ResultSet resultset;

    @Getter
    DatasetDimension assessed;
    @Getter
    DatasetDimension reference;

    String table;



    int support = -1;

    @Getter
    DatasetMeasure measure;
    @Getter
    String function;

    @Getter
    final String val1, val2;
    @Getter @Setter
    String testComment;

    @Getter @Setter
    Collection<Insight> insights;


    public AssessQuery(Connection conn, String table, DatasetDimension assessed, String val1, String val2, DatasetDimension reference, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;

        this.assessed=assessed;
        this.reference=reference;

        this.measure=m;
        this.function=agg;

        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssessQuery)) return false;
        AssessQuery that = (AssessQuery) o;
        return getAssessed().equals(that.getAssessed()) && getReference().equals(that.getReference()) && table.equals(that.table) && getMeasure().equals(that.getMeasure()) && getFunction().equals(that.getFunction()) && getVal1().equals(that.getVal1()) && getVal2().equals(that.getVal2());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssessed(), getReference(), table, getMeasure(), getFunction(), getVal1(), getVal2());
    }



    protected String getSqlInt() {
        return "select t1." + reference.getName() + ",\n" +
                "       t1.measure1 as \"" + m1PrettyName() + "\", t2.measure2 as \"" + m2PrettyName() + "\" \n" +
                "from\n" +
                "  (select "+assessed.getName()+", "+reference.getName()+", " + this.function + "(" + this.measure.getName() + ") as measure1\n" +
                "   from "+ table +"\n" +
                "   where  "+assessed.getName()+" = '"+ val1.replaceAll("'", "''") +"'\n" +
                "   group by "+assessed.getName()+", "+reference.getName()+") t1,\n" +
                "  (select "+assessed.getName()+", "+reference.getName() + "," + this.function + "(" + this.measure.getName() +") as measure2\n" +
                "   from "+ table +"\n" +
                "   where "+assessed.getName()+" = '" + val2.replaceAll("'", "''") + "'\n" +
                "   group by "+assessed.getName()+", "+reference.getName() + ") t2\n" +
                "where t1."+reference.getName()+" = t2."+reference.getName()+" order by " + reference.getName() + ";";
    }


    //This is way too slow use stats instead
    @Deprecated
    public int support(Connection conn) {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("select count(*) from " + table + " where " + assessed.getName() + " = '" + val2.replaceAll("'", "''") + "' or " + assessed.getName() + " = '" + val1.replaceAll("'", "''") + "';");
            rs.next();
            support = rs.getInt(1);
            rs.close();

        } catch (SQLException e){
            System.err.println("[ERROR] Couldn't fetch support for " + this);
            return -1;
        }
        return support;
    }

    public int support(DatasetStats stats){
        support = stats.getFrequency().get(this.assessed).get(val1) + stats.getFrequency().get(this.assessed).get(val2);
        return support;
    }

    public double dist(AssessQuery other) {
        return getDistance(other);
    }

    public float getDistance(AssessQuery other) {
        return getDistanceHamming(other);
    }

    public float getDistanceHamming(AssessQuery other) {
        int diffs = 0;
        // Agg function changed ?
        if(this.function.compareTo(other.getFunction())!=0)  diffs += 1;
        // Measure changed ?
        if(this.measure!= other.measure) diffs += 2;

        // if we select on same dimension check differences on predicates
        // else we assume they have changed
        int sel_unit_weight = 4;
        if(this.assessed.equals(other.getAssessed())) {
            if(this.val1.compareTo(other.val1)!=0){
                diffs += sel_unit_weight;
            }
            if(this.val2.compareTo(other.val2)!=0){
                diffs += sel_unit_weight;
            }
        } else{
            diffs += sel_unit_weight * 2 ;

            if (DBUtils.checkAimpliesB(this.assessed, other.assessed, conn, table) || DBUtils.checkAimpliesB(other.assessed, this.assessed, conn, table))
                diffs += 1;
            else
                diffs += 2;
        }
        // Group by dimension
        if (!this.reference.equals(other.reference)){
            if (DBUtils.checkAimpliesB(this.reference, other.reference, conn, table) || DBUtils.checkAimpliesB(other.reference, this.reference, conn, table))
                diffs += 6;
            else
                diffs += 8;
        }

        return diffs;
    }


    public String m1PrettyName(){
        return convivialNames.get(getFunction()) + "(" + measure.getPrettyName() + ") for " + assessed.getPrettyName() + " = " + val1;
    }


    public String m2PrettyName(){
        return convivialNames.get(getFunction()) + "(" + measure.getPrettyName() + ") for " + assessed.getPrettyName() + " = " + val2;
    }


    public String getDescription(){
        return "\nFor measure " + convivialNames.get(function) + " of " + measure.getName() +
                "\nComparing " + assessed.getName() + " " + val1 + " vs " + val2 +
                "\ngrouped by " + reference.getName();// + "\n\\n" + testComment;
    }


    public String getDiffs(AssessQuery previous){
        StringBuilder sb = new StringBuilder("\r\n\\n Changing: ");
        if (!measure.getName().equals(previous.measure.getName()))
            sb.append(previous.measure.getName()).append(" by ").append(measure.getName()).append(" and ");
        if (!function.equals(previous.function))
            sb.append(previous.function).append("by ").append(function).append(" and ");
        if (!previous.getAssessed().getName().equals(getAssessed().getName()))
            sb.append(previous.getAssessed().getName()).append(" by ").append(assessed.getName()).append(" and ");
        if (!val1.equals(previous.val1))
            sb.append(previous.val1).append(" by ").append(val1).append(" and ");
        if (!val2.equals(previous.val2))
            sb.append(previous.val2).append(" by ").append(val2).append(" and ");
        if(!reference.getName().equals(previous.reference.getName()))
            sb.append(previous.reference.getName()).append(" by ").append(reference.getName()).append(" and ");
        return sb.toString();
    }

    /**
     *            OLD Stuff - Only needed for backward compatibility with edbt 22
     */

    /**
     * Assess queries hold directly a connection object for historic reasons
     */
    @Override
    public void explain(){
        explainInt(this.conn);
    }

    /**
     * Only used for edbt 22
     * @param t
     */
    public void setExplainCost(long t){
        this.explainCost = t;
    }

    /**
     * Only used for edbt 22
     * @param t
     */
    public void setActualCost(long t){
        this.actualCost = t;
    }


    @Deprecated
    public Pair<Double, Double> pearsonTest(boolean close) throws SQLException {
        ArrayList<Double> left = new ArrayList<>();
        ArrayList<Double> right = new ArrayList<>();
        resultset.beforeFirst();
        int row = 0;
        while (resultset.next()) {
            double m1 = resultset.getDouble(2);
            left.add(m1);
            double m2 = resultset.getDouble(3);
            right.add(m2);
            row++;
        }
        if (close)
            resultset.close();
        if (row == 0){
            interest = 0;
            return new Pair<>(0., 1.);
        }
        double correlation, pvalue = 1;
        try {
            if (right.size() != left.size())
                throw new IllegalArgumentException("arrays must have same length");
            double[][] matrix = new double[left.size()][2];
            for (int i = 0; i < left.size(); i++) {
                matrix[i] = new double[]{left.get(i), right.get(i)};
            }
            PearsonsCorrelation corr = new PearsonsCorrelation(matrix);
            correlation = corr.getCorrelationMatrix().getEntry(0, 1);
            pvalue = corr.getCorrelationPValues().getEntry(0, 1);

        } catch (IllegalArgumentException e){
            //System.out.println("--- Offending query ---\n" + getSql());
            correlation = Double.NaN;
        }

        if (Double.isNaN(correlation) || Double.isNaN(pvalue)) {
            correlation = 0d;
            pvalue = 1d;
        }

        return new Pair<>(correlation, pvalue);
    }

    @Deprecated
    public double TTest(boolean close) throws SQLException {
        ArrayList<Double> left = new ArrayList<>();
        ArrayList<Double> right = new ArrayList<>();
        resultset.beforeFirst();
        int row = 0;
        while (resultset.next()) {
            double m1 = resultset.getDouble(2);
            left.add(m1);
            double m2 = resultset.getDouble(3);
            right.add(m2);
            row++;
        }
        if (close)
            resultset.close();
        if (row == 0){
            interest = 0;
            return 1d;
        }
        double pvalue = 1d;
        try {
            if (right.size() != left.size())
                throw new IllegalArgumentException("arrays must have same length");
            double[] x = left.stream().mapToDouble(i -> i).toArray();
            double[] y = right.stream().mapToDouble(i -> i).toArray();
            TTest tTest = new TTest();
            pvalue = tTest.tTest(x, y);

        } catch (IllegalArgumentException e){
            //System.out.println("--- Offending query ---\n" + getSql());
        }

        if (Double.isNaN(pvalue)) {
            pvalue = 1d;
        }

        return pvalue;
    }


    @Deprecated
    public double FTest(boolean close) throws SQLException {
        ArrayList<Double> left = new ArrayList<>();
        ArrayList<Double> right = new ArrayList<>();
        resultset.beforeFirst();
        int row = 0;
        while (resultset.next()) {
            double m1 = resultset.getDouble(2);
            left.add(m1);
            double m2 = resultset.getDouble(3);
            right.add(m2);
            row++;
        }
        if (close)
            resultset.close();
        if (row == 0){
            interest = 0;
            return 1d;
        }
        double pvalue = 1d;
        try {
            if (right.size() != left.size())
                throw new IllegalArgumentException("arrays must have same length");
            double[] x = left.stream().mapToDouble(i -> i).toArray();
            double[] y = right.stream().mapToDouble(i -> i).toArray();
            FTest test = new FTest(x, y);
            pvalue = test.getPValue();

        } catch (IllegalArgumentException e){
            //System.out.println("--- Offending query ---\n" + getSql());
        }

        if (Double.isNaN(pvalue)) {
            pvalue = 1d;
        }

        return pvalue;
    }


}
