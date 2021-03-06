package fr.univtours.info.dataset.metadata;

import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.*;
import java.util.Set;
import java.util.TreeSet;

public class DatasetDimension extends DatasetAttribute{

    Set<String> activeDomain;
    boolean time;
    boolean all;

    public boolean isTime() {
        return time;
    }

    public boolean isAll() {
        return all;
    }

    @Override
    public void computeActiveDomain() throws SQLException {
        activeDomain = new TreeSet<String>();//TODO check this might prove inefficient

        String sql = "select distinct " + name + " from " + table + ";";
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
        if (activeDomain == null) {
            try {
                computeActiveDomain();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return activeDomain;
    }

    @Override
    public String toString() {
        return "D{" + name +"}";
    }
}
