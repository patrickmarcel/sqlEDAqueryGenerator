package fr.univtours.info;

import java.sql.*;
import org.apache.commons.dbutils.ResultSetIterator;


public class CountdistinctQuery extends AbstractEDAsqlQuery {


    ;


    public CountdistinctQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.sql= "select count(distinct " + attribute.name + ") from " + table +";";
        this.explain = "explain analyze " + sql;

    }

    @Override
    public void execute() throws Exception{
        super.execute();
        this.count=resultset.getInt(1);

    }



}
