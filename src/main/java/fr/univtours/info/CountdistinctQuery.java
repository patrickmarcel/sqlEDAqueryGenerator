package fr.univtours.info;

import java.sql.*;
import java.util.Set;

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
