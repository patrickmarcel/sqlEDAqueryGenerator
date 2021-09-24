package fr.univtours.info.dataset.metadata;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

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
        activeDomain = new HashSet<>();

        String sql = "select distinct " + name + " from " + table + ";";
        final Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery(sql) ;
        rs.beforeFirst();

        while(rs.next()) {
            activeDomain.add(String.valueOf(rs.getObject(1)));
        }
        pstmt.close();
        rs.close();
    }



    public DatasetDimension(String name, Connection conn, String table){
        super(name, conn, table);
        prettyName = name;
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
        return "D{" + prettyName +'}';
    }
}
