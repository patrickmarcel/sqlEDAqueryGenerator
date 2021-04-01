package fr.univtours.info.queries;

import com.alexscode.utilities.collection.Pair;
import com.alexscode.utilities.math.FTest;
import fr.univtours.info.DBUtils;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.inference.TTest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

public class SiblingAssessQuery extends AbstractEDAsqlQuery{





    @Getter
    final String val1, val2;
    @Getter @Setter
    String testComment;

    public SiblingAssessQuery(Connection conn, String table, DatasetDimension assessed, String val1, String val2, DatasetDimension reference, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;

        this.assessed=assessed;
        this.reference=reference;

        this.dimensions =new HashSet<>();
        this.dimensions.add(assessed);
        this.dimensions.add(reference);

        this.measure=m;
        this.function=agg;

        this.val1 = val1;
        this.val2 = val2;
    }


    @Override
    protected String getSqlInt() {
        return "select t1." + reference.getName() + ",\n" +
                "       t1.measure1 as \"" + m1PrettyName() + "\", t2.measure2 as \"" + m2PrettyName() + "\" \n" +
                "from\n" +
                "  (select "+assessed.getName()+", "+reference.getName()+", " + this.function + "(" + this.measure.getName() + ") as measure1\n" +
                "   from "+ table +"\n" +
                "   where  "+assessed.getName()+" = '"+val1+"'\n" +
                "   group by "+assessed.getName()+", "+reference.getName()+") t1,\n" +
                "  (select "+assessed.getName()+", "+reference.getName() + "," + this.function + "(" + this.measure.getName() +") as measure2\n" +
                "   from "+ table +"\n" +
                "   where "+assessed.getName()+" = '"+val2+"'\n" +
                "   group by "+assessed.getName()+", "+reference.getName() + ") t2\n" +
                "where t1."+reference.getName()+" = t2."+reference.getName()+";";
    }

    public String m1PrettyName(){
        return convivialNames.get(getFunction()) + "(" + measure.getName() + ") for " + assessed.getName() + " = " + val1;
    }

    public String m2PrettyName(){
        return convivialNames.get(getFunction()) + "(" + measure.getName() + ") for " + assessed.getName() + " = " + val2;
    }

    @Override
    public String getDescription(){
        return "\nComparing " + assessed.getName() + " \"" + val1 + "\" vs \"" + val2 + "\" on " + convivialNames.get(function) + " " + measure.getName() + "\n\\n" + testComment;
    }

    public String getDiffs(SiblingAssessQuery previous){
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

    @Override
    public DatasetDimension getAssessed() {
        return assessed;
    }

    @Override
    public DatasetDimension getReference() {
        return reference;
    }

    @Override
    public float getDistance(AbstractEDAsqlQuery other) {
        return getDistanceHamming(other);/*
        float result=0;
        if(this.function.compareTo(other.getFunction())!=0)  result ++;
        if(this.measure!=other.measure) result+=2;
        if(this.assessed!=other.getAssessed()) {
            if(DBUtils.checkAimpliesB(this.assessed, other.getAssessed() , conn, table)
                    || DBUtils.checkAimpliesB(other.getAssessed(), this.assessed , conn, table)){
                result+=3;
            }
            else {
                result+=4;
            }
            if(this.reference==other.getReference()) {
                if(this.val1.compareTo(((SiblingAssessQuery)other).val1)!=0){
                    result+=5;
                }
                if(this.val2.compareTo(((SiblingAssessQuery)other).val2)!=0){
                    result+=6;
                }
            }
            if(this.reference!=other.getReference()) result+=7;
        }
        return result;*/
    }

    public float getDistanceHamming(AbstractEDAsqlQuery other) {
        SiblingAssessQuery o = (SiblingAssessQuery) other;
        float diffs = 0;
        // Agg function changed ?
        if(this.function.compareTo(o.getFunction())!=0)  diffs += 1;
        // Measure changed ?
        if(this.measure!=o.measure) diffs += 1;

        // if we select on same dimension check differences on predicates
        // else we assume they have changed
        float sel_unit_weight = 0.5f;
        if(this.assessed.equals(o.getAssessed())) {
            if(this.val1.compareTo(o.val1)!=0){
                diffs += sel_unit_weight;
            }
            if(this.val2.compareTo(o.val2)!=0){
                diffs += sel_unit_weight;
            }
        } else{
            diffs += sel_unit_weight * 2 ;

            if (DBUtils.checkAimpliesB(this.assessed, o.assessed, conn, table) || DBUtils.checkAimpliesB(o.assessed, this.assessed, conn, table))
                diffs += 0.5;
            else
                diffs += 1;
        }
        // Group by dimension
        if (!this.reference.equals(o.reference)){
            if (DBUtils.checkAimpliesB(this.reference, o.reference, conn, table) || DBUtils.checkAimpliesB(o.reference, this.reference, conn, table))
                diffs += 0.5;
            else
                diffs += 1;
        }

        return diffs;
    }

    @Override
    public void interestFromResult() throws Exception {
        //PLACEHOLDER
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


