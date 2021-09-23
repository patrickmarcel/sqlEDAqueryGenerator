package fr.univtours.info.dataset.metadata;

import fr.univtours.info.dataset.DBConfig;
import lombok.Getter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetStats {
    String table;
    //size of active domain of dimensions
    @Getter
    HashMap<DatasetDimension, Integer> adSize;
    //width (for strings)
    HashMap<DatasetDimension, Integer> avgWidth;
    //absolute frequency of values of dimensions
    @Getter
    HashMap<DatasetDimension, HashMap<String, Integer>> frequency;
    @Getter
    int rows;
    private List<DatasetDimension> dimensions;
    private List<DatasetMeasure> measures;

    public DatasetStats(DBConfig config) throws SQLException {

        table = config.getTable();
        dimensions = config.getDimensions();
        measures = config.getMeasures();
        Connection conn = config.getConnection();

        // Pre compute stats
        adSize = new HashMap<>();
        frequency = new HashMap<>();
        avgWidth = new HashMap<>();

        // strings
        for (DatasetDimension dim : dimensions) {
            String sql = "select avg_width from pg_stats where tablename = '" + table + "' and attname = '"+dim.getPrettyName()+"';";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            rs.next();
            avgWidth.put(dim, rs.getInt(1));
            st.close();
        }

        // Active domain size
        for (DatasetDimension dim : dimensions) {
            String sql = "select count(distinct " + dim.getName() + ") from " + table + ";";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            rs.next();
            adSize.put(dim, rs.getInt(1));
            st.close();
        }
        // Absolute frequency
        for (DatasetDimension dim : dimensions) {
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

        //conn.close();
    }

    public long estimateAggregateSize(Collection<DatasetDimension> groupBy){
        int len = groupBy.stream().mapToInt(d -> adSize.get(d)).reduce(Math::multiplyExact).getAsInt();
        return 64L *measures.size()*len + dimensions.stream().mapToInt(d -> 64*len*avgWidth.get(d)).sum();
    }
}
