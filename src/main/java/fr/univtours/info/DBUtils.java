package fr.univtours.info;

import fr.univtours.info.dataset.metadata.DatasetDimension;

import java.sql.*;
import java.util.HashMap;

public class DBUtils {
    public static HashMap<String, Boolean> cache = new HashMap<>();

    /**
     * Check if The functional dependency A ==> B holds on a given table
     * @param A The left attribute
     * @param B The right Attribute
     * @param db_conn a database connection
     * @param table the table containing columns A and B
     * @return true if A implies B
     */
    public static boolean checkAimpliesB(DatasetDimension A, DatasetDimension B, Connection db_conn, String table){
        if (cache.get(A.getName() + B.getName() + table) != null)
            return cache.get(A.getName() + B.getName() + table);
        String sql = "select count(*) from (select "+A.getName()+", count(distinct "+B.getName()+") from \""+table+"\" group by "+A.getName()+" having  count(distinct "+B.getName()+")>1) as T;";
        //System.out.println(sql);
        try {
            final Statement pstmt = db_conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = pstmt.executeQuery(sql) ;
            rs.next();
            int cnt = rs.getInt(1);
            pstmt.close();
            rs.close();
            cache.put(A.getName() + B.getName() + table, cnt == 0);
            return cnt == 0;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }

    }
}
