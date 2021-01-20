package fr.univtours.info.metadata;

import fr.univtours.info.Config;
import fr.univtours.info.DBservices;
import lombok.Getter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatasetStats {
    String table;
    //size of active domain of dimensions
    @Getter
    HashMap<DatasetDimension, Integer> adSize;
    //absolute frequency of values of dimensions
    @Getter
    HashMap<DatasetDimension, HashMap<String, Integer>> frequency;
    @Getter
    int rows;

    public DatasetStats() throws SQLException, IOException {
        Config config = Config.readProperties();
        table = config.getTable();
        List<DatasetDimension> theDimensions;
        theDimensions = config.getDimensions();


        // Pre compute stats
        adSize = new HashMap<>();
        frequency = new HashMap<>();

        DBservices db = new DBservices();
        Connection conn = db.connectToPostgresql();

        // Active domain size
        for (DatasetDimension dim : theDimensions) {
            String sql = "select count(distinct " + dim.getName() + ") from " + table + ";";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            rs.next();
            adSize.put(dim, rs.getInt(1));
            st.close();
        }
        // Absolute frequency
        for (DatasetDimension dim : theDimensions) {
            HashMap<String, Integer> tmp = new HashMap<>();
            String sql = "select " + dim.getName() + ", count(*) from " + table + " group by " + dim.getName() + ";";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            while (rs.next()) {
                tmp.put(rs.getString(1), rs.getInt(2));
            }
            st.close();
            frequency.put(dim, tmp);
        }
        //Count rows
        String sql = "select count(*) from " + table + ";";
        Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = st.executeQuery(sql) ;
        rs.next();
        rows = rs.getInt(1);
        st.close();

        conn.close();
    }
}
