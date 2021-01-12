package fr.univtours.info;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;

public class AggregateQuery extends AbstractEDAsqlQuery{




    public AggregateQuery(Connection conn, String table, Set<DatasetDimension> dimensions, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;
        this.sql= "select ";
        String groupby="";
        for (DatasetDimension d :dimensions){
            groupby = groupby + d.name + ", ";
        }
        if(groupby.length()==0){ // empty set<dim>
            sql=sql + " " + agg + "(" + m.name + ") from " + table + ";";
        }
        else{
            //groupby=groupby.substring(0,groupby.length()-1);
            //System.out.println(groupby);
            sql=sql + groupby + " " + agg+ "(" + m.name + ") from " + table +" group by  ";
            groupby=groupby.substring(0,groupby.length()-2);
            //System.out.println(groupby);
            sql=sql + groupby + ";";
            //+ "avg(" + m + ") from " + table +" group by" + ";";
        }

        // System.out.println(sql);
        this.explain = "explain analyze " + sql;

    }


    @Override
    public Set<DatasetDimension> getGroupby() {
        return null;
    }

    @Override
    public DatasetMeasure getMeasure() {
        return null;
    }

    @Override
    public String getFunction() {
        return null;
    }
}
