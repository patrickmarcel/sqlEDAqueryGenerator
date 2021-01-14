package fr.univtours.info.metadata;

import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.*;
import java.util.Set;
import java.util.TreeSet;

public class DatasetDimension extends DatasetAttribute{

    Set<String> activeDomain;

    @Override
    public void setActiveDomain() throws SQLException {
        activeDomain=new TreeSet<String>();

        String sql="select distinct " + name + " from " + table + ";";
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(sql) ;

        ResultSetMetaData rmsd = rs.getMetaData();
        rs.beforeFirst();
        ResultSetIterator rsit=new ResultSetIterator(rs);
        Object[] tab=null;
        while(rsit.hasNext()) { // move to last for getting execution time
            tab=rsit.next();
            //System.out.println(tab[0].toString());
            activeDomain.add(tab[0].toString());
        }
        pstmt.close();
        rs.close();
    }



    public DatasetDimension(String name, Connection conn, String table){
        super(name, conn, table);
    }

    public Set<String> getActiveDomain(){
        return activeDomain;
    }

    public int getDistinct(String value){
        return 0;
    }
}
