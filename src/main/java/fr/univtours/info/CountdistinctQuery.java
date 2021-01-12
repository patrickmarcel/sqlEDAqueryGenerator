package fr.univtours.info;

import java.sql.*;
import java.util.HashSet;


public class CountdistinctQuery extends AbstractEDAsqlQuery {




    public CountdistinctQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.dimensions =new HashSet<DatasetDimension>();
        this.dimensions.add(attribute);
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

    @Override
    public void interestWithZscore() throws Exception {

    }


}
