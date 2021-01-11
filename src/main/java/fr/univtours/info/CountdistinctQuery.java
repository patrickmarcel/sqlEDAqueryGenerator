package fr.univtours.info;

import java.sql.*;
import org.apache.commons.dbutils.ResultSetIterator;


public class CountdistinctQuery extends AbstractEDAsqlQuery {


    String attribute;


    public CountdistinctQuery(Connection conn, String table, String attribute){
        this.conn=conn;
        this.table=table;
        this.attribute=attribute;
        this.sql= "select count(distinct " + attribute + ") from " + table +";";
        this.explain = "explain analyze " + sql;

    }

    @Override
    public void execute() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(this.sql) ;
        this.resultset=rs;
        rs.next();
        this.count=rs.getInt(1);

    }



}
