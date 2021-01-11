package fr.univtours.info;

import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public abstract class AbstractEDAsqlQuery implements EDAsqlQuery {

    Connection conn;
    ResultSet resultset;
    String table;

    String sql;
    String explain ;

    int count=0;
    float cost=0;



    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public float getInterest() {
        return 0;
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
        ResultSet rs = pstmt.executeQuery(this.explain) ;
        //this.resultset=rs;

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
        this.cost= Float.parseFloat(tmp2[0]);

    }


}
