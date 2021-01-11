package fr.univtours.info;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;

public class AggregateQuery extends AbstractEDAsqlQuery{


    String attribute;


    public AggregateQuery(Connection conn, String table, Set<DatasetDimension> dimensions, DatasetMeasure m){
        this.conn=conn;
        this.table=table;
        this.attribute=attribute;
        this.sql= "select ";
        String groupby="";
        for (DatasetDimension d :dimensions){
            groupby = groupby + d.name + ", ";
        }
        if(groupby.length()==0){ // empty set<dim>
            sql=sql + " avg(" + m.name + ") from " + table + ";";
        }
        else{
            //groupby=groupby.substring(0,groupby.length()-1);
            //System.out.println(groupby);
            sql=sql + groupby + " avg(" + m.name + ") from " + table +" group by ";
            groupby=groupby.substring(0,groupby.length()-2);
            System.out.println(groupby);
            sql=sql + groupby + ";";
            //+ "avg(" + m + ") from " + table +" group by" + ";";
        }

        System.out.println(sql);
        this.explain = "explain analyze " + sql;

    }

    @Override
    public void execute() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(this.sql) ;
        this.resultset=rs;
        rs.next();
        //this.count=rs.getInt(1);

    }


}
