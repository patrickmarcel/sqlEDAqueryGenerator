package fr.univtours.info;

import java.sql.Connection;
import java.util.HashSet;

public class SiblingAssessQuery extends AbstractEDAsqlQuery{

    DatasetDimension assessed;
    DatasetDimension reference;

    public SiblingAssessQuery(Connection conn, String table, DatasetDimension assessed, String val1, String val2, DatasetDimension reference, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;

        this.assessed=assessed;
        this.reference=reference;

        this.dimensions =new HashSet<DatasetDimension>();
        this.dimensions.add(assessed);
        this.dimensions.add(reference);

        this.measure=m;
        this.function=agg;

        this.sql= "select t1."+assessed.name+", t1." + reference.name + ", " +
                "       t1.measure1, t2.measure2 as benchmark " +
                "from " +
                "  (select "+assessed.name+", "+reference.name+", " + agg+ "(" + m.name + ") as measure1 " +
                "   from "+ table +"     " +
                "   where  "+assessed.name+" = '"+val1+"'" +
                "   group by "+assessed.name+", "+reference.name+") t1," +

                "  (select "+assessed.name+", "+reference.name + "," + agg + "(" + m.name +") as measure2 " +
                "   from "+ table +" " +
                "   where "+assessed.name+" = '"+val2+"'" +
                "   group by "+assessed.name+", "+reference.name + ") t2 " +
                "where t1."+reference.name+" = t2."+reference.name+"; ";

        //System.out.println(sql);
        this.explain = "explain  " + sql;
        this.explainAnalyze = "explain analyze " + sql;

    }

    public float getDistance(EDAsqlQuery other){
        //super.getDistance(other);
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
    public DatasetDimension getAssessed() {
        return assessed;
    }

    @Override
    public DatasetDimension getReference() {
        return reference;
    }

    @Override
    public void interestWithZscore() throws Exception {
        throw new UnsupportedOperationException();
    }


}


