package fr.univtours.info.queries;

import com.alexscode.utilities.collection.Pair;
import com.alexscode.utilities.math.FTest;
import fr.univtours.info.DBUtils;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AssessQuery implements TimeableOp, Measurable {

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

    private String sql;

    @Getter
    long actualCost = 0;
    @Setter
    long explainCost = -1;
    @Getter @Setter
    double interest = 0;

    @Getter
    DatasetMeasure measure;
    @Getter
    String function;

    @Getter
    final String val1, val2;
    @Getter @Setter
    String testComment;


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
    public long estimatedTime() {
        if (explainCost == -1)
            explain();
        return explainCost;
    }

    @Override
    public long actualTime() {
        return (long) actualCost;
    }

    public String getSql(){
        if (sql == null)
            sql = this.getSqlInt();
        return sql;
    }

    public void explainAnalyze() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery("explain analyze " + this.getSql()) ;

        ResultSetMetaData rmsd = rs.getMetaData();
        rs.beforeFirst();
        ResultSetIterator rsit=new ResultSetIterator(rs);
        Object[] tab=null;
        while(rsit.hasNext()) { // move to last for getting execution time
            tab=rsit.next();
        }
        String last= tab[0].toString();
        String tmp1=last.split("Execution Time: ")[1];
        String[] tmp2=tmp1.split(" ms");
        this.actualCost = (long) Float.parseFloat(tmp2[0]);

        pstmt.close();
        rs.close();
    }



    public void explain() {
        try (Statement pstmt = conn.createStatement()) {
            // For classic dbms
            if (DBConfig.DIALECT != 2) {
                ResultSet rs = pstmt.executeQuery("explain  " + this.getSql());

                rs.next();

                String s1 = rs.getString("QUERY PLAN");
                String[] s2 = s1.split("=");
                String[] s3 = s2[1].split("\\.\\.");
                this.explainCost = (long) Float.parseFloat(s3[0]);
                rs.close();
            }
            // For MonetDB
            else {
                String line = "";
                ResultSet rs = pstmt.executeQuery("explain  " + this.getSql());
                while (rs.next()){
                    line = rs.getString(1);
                    if (line.contains("#total") && line.contains("time=")){
                        this.explainCost = 1 + Long.parseLong(line.split("time=")[1].replace("usec", "").stripTrailing())/1000;
                    }
                }
            }
        } catch (SQLException e){
            System.err.println("[ERROR] Failed to fetch query plan for " + this);
        }

    }


    @Deprecated
    public ResultSet execute() {
        final Statement pstmt;
        try {
            pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = pstmt.executeQuery(this.getSql()) ;
            this.resultset=rs;
            rs.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return resultset;

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


    public void print(){
        System.out.println(this.getSql());
    }

    public void printResult() throws SQLException {
        if (resultset == null)
            this.execute();

        System.out.println("--- Result Set ---");
        resultset.beforeFirst();
        ResultSetMetaData rsmd = resultset.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        boolean first = true;
        while (resultset.next()) {
            if (first){
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    System.out.print(rsmd.getColumnName(i));
                }
                System.out.println();
                first = false;
            }
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = resultset.getString(i);
                System.out.print(columnValue);
            }
            System.out.println();
        }
        System.out.println("--- Result Set END ---");
        resultset.close();

    }

    public int support() {
        int size;
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("select count(*) from " + table + " where " + assessed.getName() + " = '" + val2.replaceAll("'", "''") + "' or " + assessed.getName() + " = '" + val1.replaceAll("'", "''") + "';");
            rs.next();
            size = rs.getInt(1);
            rs.close();

        } catch (SQLException e){
            System.err.println("[ERROR] Couldn't fetch support for " + this);
            return 0;
        }
        return size;
    }

    @Override
    public double dist(Object other) {
        return getDistance((AssessQuery) other);
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
                diffs += 3;
            else
                diffs += 4;
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
        return "\nComparing " + assessed.getName() + " \"" + val1 + "\" vs \"" + val2 + "\" on " + convivialNames.get(function) + " of " + measure.getName() + " grouped by " + reference.getName();// + "\n\\n" + testComment;
    }

    public String getDiffs(AssessQuery previous){
        StringBuilder sb = new StringBuilder("\r\n\\n Differences from Previous Query: ");
        if (!measure.getName().equals(previous.measure.getName()))
            sb.append(previous.measure.getName()).append(" -> ").append(measure.getName()).append(" | ");
        if (!function.equals(previous.function))
            sb.append(previous.function).append(" -> ").append(function).append(" | ");
        if (!previous.getAssessed().getName().equals(getAssessed().getName()))
            sb.append(previous.getAssessed().getName()).append(" -> ").append(assessed.getName()).append(" | ");
        if (!val1.equals(previous.val1))
            sb.append(previous.val1).append(" -> ").append(val1).append(" | ");
        if (!val2.equals(previous.val2))
            sb.append(previous.val2).append(" -> ").append(val2).append(" | ");
        if(!reference.getName().equals(previous.reference.getName()))
            sb.append(previous.reference.getName()).append(" -> ").append(reference.getName()).append(" | ");
        return sb.toString();
    }


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
