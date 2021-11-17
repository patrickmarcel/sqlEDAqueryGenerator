package fr.univtours.info.dataset.metadata;

import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
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

    public DatasetStats(Dataset ds) throws SQLException {

        table = ds.getTable();
        dimensions = ds.getTheDimensions();
        measures = ds.getTheMeasures();
        Connection conn = ds.getConn();
        rows = ds.getTableSize();

        // Pre compute stats
        adSize = new HashMap<>();
        frequency = new HashMap<>();
        avgWidth = new HashMap<>();

        // strings width
        for (DatasetDimension dim : dimensions) {
            String sql = "select avg_width from pg_stats where tablename = '" + table + "' and attname = '"+dim.getPrettyName()+"';";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            rs.next();
            try {
                avgWidth.put(dim, rs.getInt(1));
            } catch (SQLException e){
                //close statement
                st.close();
                //run analyse
                String analyse = "analyse " + table + ";";
                boolean status = conn.createStatement().execute(analyse);
                System.err.println("\n[ERROR] An analyse command was issued on table " + table + " statistics are necessary for TAP sampling please wait a few second before restarting TAP");
                System.exit(2);
            }
            st.close();
        }

        // Absolute frequency - Active domain size
        for (DatasetDimension dim : dimensions) {
            HashMap<String, Integer> tmp = new HashMap<>();
            String sql = "select " + dim.getName() + ", count(*) from " + table + " group by " + dim.getName() + ";";
            Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = st.executeQuery(sql) ;
            while (rs.next()) {
                tmp.put(String.valueOf(rs.getObject(1)), rs.getInt(2));
            }
            st.close();
            frequency.put(dim, tmp);
            adSize.put(dim, tmp.size());
            dim.setActiveDomain(tmp.keySet());
        }
    }

    public long estimateAggregateSize(Collection<DatasetDimension> groupBy){
        long len = groupBy.stream().mapToLong(d -> adSize.get(d)).reduce(Math::multiplyExact).getAsLong();
        return 64L *measures.size()*len + dimensions.stream().mapToLong(d -> 64L * len * avgWidth.get(d)).sum();
    }
}
