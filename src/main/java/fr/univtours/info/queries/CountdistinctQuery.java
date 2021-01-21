package fr.univtours.info.queries;

import fr.univtours.info.metadata.DatasetDimension;

import java.sql.*;
import java.util.HashSet;


public class CountdistinctQuery extends AbstractEDAsqlQuery {

    private final DatasetDimension attrib;


    public CountdistinctQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.dimensions =new HashSet<>();
        this.dimensions.add(attribute);
        this.attrib = attribute;
        this.measure=null;
        this.function="count distinct";


        //this.explain = "explain  " + sql;
        //this.explainAnalyze = "explain analyze " + sql;

    }

    @Override
    protected String getSqlInt() {
       return  "select count(distinct " + attrib.getName() + ") from " + table +";";
    }

    @Override
    public void execute() {
        super.execute();
        try {
            resultset.beforeFirst();
            resultset.next();
            this.count=resultset.getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    @Override
    public void interestFromResult() throws Exception {

    }


}
