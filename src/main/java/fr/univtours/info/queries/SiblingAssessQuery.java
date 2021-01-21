package fr.univtours.info.queries;

import com.alexscode.utilities.math.Distribution;
import com.google.common.collect.Streams;
import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import lombok.Getter;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SiblingAssessQuery extends AbstractEDAsqlQuery{

    DatasetDimension assessed;
    DatasetDimension reference;
    @Getter
    private final String val1, val2;

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

    public float getDistance(AbstractEDAsqlQuery other){
        int result=0;
        //System.out.println(this.sql + "  other: " + other.sql);
        //System.out.println(this.assessed + "  other: " + other.assessed);
        if(this.assessed!=other.getAssessed()) result=result+1;
        if(this.reference!=other.getReference()) result=result+1;
        if(this.measure!=other.getMeasure()) result=result+1;
        if(this.function.compareTo(other.getFunction())!=0) result=result+1;
        return result;
    }

    @Override
    protected String getSqlInt() {
        return "select t1."+assessed.getName()+", t1." + reference.getName() + ", " +
                "       t1.measure1, t2.measure2 as benchmark " +
                "from " +
                "  (select "+assessed.getName()+", "+reference.getName()+", " + this.function + "(" + this.measure.getName() + ") as measure1 " +
                "   from "+ table +"     " +
                "   where  "+assessed.getName()+" = '"+val1+"'" +
                "   group by "+assessed.getName()+", "+reference.getName()+") t1," +

                "  (select "+assessed.getName()+", "+reference.getName() + "," + this.function + "(" + this.measure.getName() +") as measure2 " +
                "   from "+ table +" " +
                "   where "+assessed.getName()+" = '"+val2+"'" +
                "   group by "+assessed.getName()+", "+reference.getName() + ") t2 " +
                "where t1."+reference.getName()+" = t2."+reference.getName()+"; ";
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
    public void interestFromResult() throws SQLException {
        ArrayList<Double> left = new ArrayList<>();
        ArrayList<Double> right = new ArrayList<>();
        Distribution<Integer> p = new Distribution<>();
        Distribution<Integer> q = new Distribution<>();
        resultset.beforeFirst();
        int row = 0;
        while (resultset.next()) {
            p.setProba(row, (double) resultset.getFloat("measure1"));
            left.add((double) resultset.getFloat("measure1"));
            q.setProba(row, (double) resultset.getFloat("measure2"));
            right.add((double) resultset.getFloat("measure2"));
            row++;
        }

        PearsonsCorrelation corr = new PearsonsCorrelation();
        double correlation = corr.correlation(right.stream().mapToDouble(i -> i).toArray(), left.stream().mapToDouble(i -> i).toArray());

        p.normalize();
        q.normalize();
        System.out.println("Kullback " + Distribution.kullbackLeiblerDirty(p, q) + " | Correlation " + correlation );
        interest = Distribution.kullbackLeiblerDirty(p, q);
    }



}


