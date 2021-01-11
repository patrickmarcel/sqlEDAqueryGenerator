package fr.univtours.info;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

public class SiblingAssessQuery extends AbstractEDAsqlQuery{



    public SiblingAssessQuery(Connection conn, String table, DatasetDimension assessed, String val1, String val2, DatasetDimension reference, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;

        this.groupby=new HashSet<DatasetDimension>();
        this.groupby.add(assessed);
        this.groupby.add(reference);

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



}


