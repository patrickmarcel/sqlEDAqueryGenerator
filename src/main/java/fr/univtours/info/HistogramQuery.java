package fr.univtours.info;

import java.sql.Connection;
import java.util.Set;

public class HistogramQuery extends AbstractEDAsqlQuery {



    public HistogramQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.sql= "select " + attribute.name + ",count(*) from " + table +" group by " + attribute.name+ ";";
        this.explain = "explain analyze " + sql;
        // System.out.println(sql);
        this.explain = "explain analyze " + sql;

    }


}
