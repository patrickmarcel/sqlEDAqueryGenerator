package fr.univtours.info.queries;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import fr.univtours.info.optimize.time.CostModelProvider;
import fr.univtours.info.optimize.time.TimeableOp;
import fr.univtours.info.optimize.tsp.Measurable;
import lombok.Getter;
import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.*;
import java.util.Set;

public abstract class AbstractEDAsqlQuery implements TimeableOp, Measurable {

    Connection conn;
    ResultSet resultset;
    ResultSet explainResultset;
    ResultSet explainAnalyzeResultset;

    String table;

    private String sql;
    //protected String explain ;
    //protected String explainAnalyze ;

    int count=0;
    @Getter
    float actualCost =0;
    float explainCost=0;
    double interest=0;

    Set<DatasetDimension> dimensions;
    @Getter
    DatasetMeasure measure;
    @Getter
    String function;

    @Override
    public long estimatedTime() {
        return CostModelProvider.getModelFor(this).estimateCost(this);
    }

    @Override
    public long actualTime() {
        return (long) actualCost;
    }

    protected abstract String getSqlInt();
    public String getSql(){
        if (sql == null)
            sql = this.getSqlInt();
        return sql;
    }

    public Set<DatasetDimension> getDimensions(){
        return this.dimensions;
    }

    public float getEstimatedCost() {
        return explainCost;
    }

    public void setEstimatedCost(float estimation){
        this.explainCost=estimation;
    }

    public double getInterest() {
        return interest;
    }


    public DatasetDimension getAssessed() {
        return null;
    }

    public DatasetDimension getReference() {
        return null;
    }


    public abstract float getDistance(AbstractEDAsqlQuery other);

    public int getCount(){
        return count;
    }



    public void explainAnalyze() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery("explain analyze " + this.getSql()) ;
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

        pstmt.close();
        rs.close();
    }



    public void explain() throws Exception{
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        //System.out.println(this.explain);
        ResultSet rs = pstmt.executeQuery("explain  " + this.getSql()) ;
        this.explainResultset=rs;

        rs.beforeFirst();
        rs.next();

        String s1 = rs.getString("QUERY PLAN");
        String[] s2 = s1.split("=");
        String[] s3 = s2[1].split("\\.\\.");
        this.explainCost = Float.parseFloat(s3[0]);

        pstmt.close();
        rs.close();
    }



    public void execute() {
        final Statement pstmt;
        try {
            pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = pstmt.executeQuery(this.getSql()) ;
            this.resultset=rs;
            rs.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }



    }

    public void computeInterest() throws Exception {
       // this.interestWithPlan();
        this.interestFromResult();
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

    public abstract void interestFromResult() throws Exception;

    public void print(){
        System.out.println(this.getSql());
    }



    public void printResult() throws SQLException {
        if (resultset == null)
            this.execute();

        System.out.println("--- Result Set ---");
        resultset.beforeFirst();
        ResultSetMetaData rsmd = resultset.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        boolean first = true;
        while (resultset.next()) {
            if (first){
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    System.out.print(rsmd.getColumnName(i));
                }
                System.out.println();
                first = false;
            }
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = resultset.getString(i);
                System.out.print(columnValue);
            }
            System.out.println();
        }
        System.out.println("--- Result Set END ---");

    }

    @Override
    public double dist(Object other) {
        return getDistance((AbstractEDAsqlQuery) other);
    }
}
