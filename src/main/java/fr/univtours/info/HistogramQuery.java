package fr.univtours.info;

import java.sql.Connection;
import java.util.HashSet;

public class HistogramQuery extends AbstractEDAsqlQuery {



    public HistogramQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.dimensions =new HashSet<DatasetDimension>();
        this.dimensions.add(attribute);
        this.measure=null;
        this.function="count";

        this.sql= "select " + attribute.name + ",count(*) from " + table +" group by " + attribute.name+ ";";
        this.explain = "explain  " + sql;
        // System.out.println(sql);
        this.explainAnalyze = "explain analyze " + sql;

    }


    @Override
    public void interestWithZscore() throws Exception {

    }
}
