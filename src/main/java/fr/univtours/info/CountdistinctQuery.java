package fr.univtours.info;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.dbutils.ResultSetIterator;


public class CountdistinctQuery extends AbstractEDAsqlQuery {


    ;


    public CountdistinctQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.groupby=new HashSet<DatasetDimension>();
        this.groupby.add(attribute);
        this.measure=null;
        this.function="count distinct";

        this.sql= "select count(distinct " + attribute.name + ") from " + table +";";
        this.explain = "explain  " + sql;
        this.explainAnalyze = "explain analyze " + sql;

    }

    @Override
    public void execute() throws Exception{
        super.execute();
        this.count=resultset.getInt(1);

    }



}
