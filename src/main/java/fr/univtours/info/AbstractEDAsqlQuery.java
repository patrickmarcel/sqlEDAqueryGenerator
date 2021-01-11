package fr.univtours.info;

import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.*;
import java.util.Set;

public abstract class AbstractEDAsqlQuery implements EDAsqlQuery {

    Connection conn;
    ResultSet resultset;
    ResultSet explainResultset;
    ResultSet explainAnalyzeResultset;
    String table;

    String sql;
    String explain ;
    String explainAnalyze ;

    int count=0;
    float actualCost =0;
    double interest=0;

    Set<DatasetDimension> dimensions;
    DatasetMeasure measure;
    String function;

    @Override
    public Set<DatasetDimension> getDimensions(){
        return this.dimensions;
    };

    @Override
    public DatasetMeasure getMeasure(){
        return measure;
    }

    public String getFunction(){
        return function;
    }

    @Override
    public float getActualCost() {
        return actualCost;
    }

    @Override
    public double getInterest() {
        return interest;
    }

    @Override
    public float getDistance(EDAsqlQuery other) {
        return 0;
    }

    public int getCount(){
        return count;
    }


    public void explainAnalyze() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(this.explainAnalyze) ;
        this.explainAnalyzeResultset=rs;

        ResultSetMetaData rmsd = rs.getMetaData();
        rs.beforeFirst();
        ResultSetIterator rsit=new ResultSetIterator(rs);
        Object[] tab=null;
        while(rsit.hasNext()) { // move to last for getting execution time
            tab=rsit.next();
        }
        String last= tab[0].toString();
        String tmp1=last.split("Execution Time: ")[1];
        String[] tmp2=tmp1.split(" ms");
        this.actualCost = Float.parseFloat(tmp2[0]);

    }



    public void explain() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        //System.out.println(this.explain);
        ResultSet rs = pstmt.executeQuery(this.explain) ;
        this.explainResultset=rs;




    }


    @Override
    public void execute() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(this.sql) ;
        this.resultset=rs;
        rs.next();


    }

    public void computeInterest() throws Exception {
       // this.interestWithPlan();
        this.interestWithZscore();
    }

    /**
     * compute interestingness using the number of lines in the query plan
     */
    void interestWithPlan() throws Exception {
        this.explain();

        if (this.explainResultset != null)        {
            this.explainResultset.last();    // moves cursor to the last row
            this.interest = this.explainResultset.getRow(); // get row id
        }

    }

    public abstract void interestWithZscore() throws Exception;

    public void print(){
        System.out.println(sql);
    }


    @Override
    public void printResult() throws SQLException {
        ResultSetIterator rsit=new ResultSetIterator(resultset);
        Object[] tab=null;
        resultset.beforeFirst();
        while(rsit.hasNext()) { // move to last for getting execution time
            tab=rsit.next();
            for(int i=0;i<tab.length;i++){
                System.out.print(tab[i] + " ") ;
            }

        }

    }

}
