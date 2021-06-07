package fr.univtours.info;

import com.alexscode.utilities.math.Permutations;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import org.apache.commons.math3.stat.StatUtils;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticalVerifier {

    public static double check(Insight i, Dataset ds){
        double[] a, b;
        //Load
        try {
            String sqlA = "select " + i.getMeasure().getName() + " as measure from " + ds.getTable() + " where " + i.getDim().getName() + " = '" + i.getSelA() + "';";
            String sqlB = "select " + i.getMeasure().getName() + " as measure from " + ds.getTable() + " where " + i.getDim().getName() + " = '" + i.getSelB() + "';";

            ArrayList<Double> A = new ArrayList<>();
            ArrayList<Double> B = new ArrayList<>();

            Connection conn = ds.getConn();
            Statement pstmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            ResultSet rs = pstmt.executeQuery(sqlA);
            rs.next();
            int rowA = 0;
            while (rs.next()) {
                double m1 = rs.getDouble(1);
                A.add(m1);
                rowA++;
            }

            rs = pstmt.executeQuery(sqlB);
            rs.next();

            int rowB = 0;
            while (rs.next()) {
                double m1 = rs.getDouble(1);
                B.add(m1);
                rowB++;
            }

            a = new double[rowA];
            b = new double[rowB];
            for (int j = 0; j < rowA; j++)
                a[j] = A.get(j);
            for (int j = 0; j < rowB; j++)
                b[j] = B.get(j);

        } catch (SQLException e){
            System.err.println("[Error] impossible to run fetch query");
            return 1.0;
        }

        double base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        double[] perm_stats = Permutations.mean(a, b, 500);
        int count = 0;
        for (int j = 0; j < 500; j++) {
            if (perm_stats[j] > base_stat)
                count++;
        }
        i.setP((count)/((double) 500));
        return i.getP();
    }

    /**
     * Checks a series of insights on a dataset optimizing loading of data and number of queries to the database,
     * p value is return through the insights object internal state change.
     * @param insights The list of insights
     * @param ds The dataset to check insights on
     */
    public static List<Insight> check(List<Insight> insights, Dataset ds, double sigLevel, int permNb) {
        List<List<Insight>> perDim = new ArrayList<>();
        ds.getTheDimensions().forEach(d -> perDim.add(new ArrayList<>()));
        insights.forEach(i -> perDim.get(ds.getTheDimensions().indexOf(i.getDim())).add(i));

        for (List<Insight> insightsForD : perDim){
            Map<String, List<Insight>> perMeas = insightsForD.stream().collect(Collectors.groupingBy(insight -> insight.getMeasure().getName()));
            for (Map.Entry<String, List<Insight>> kv : perMeas.entrySet()){
                insights.addAll(check(kv.getValue(), ds, kv.getValue().get(0).getDim(), kv.getValue().get(0).getMeasure(), permNb));
            }
        }
        //TODO Quick fix find out why duplicates ...
        return new ArrayList<>(insights.stream().filter(i -> i.getP() < sigLevel).collect(Collectors.toSet()));
    }

    /**
     * Internal check in bulk (same dimension/measure)
     * @param insights insights on the same measure and dimension
     * @param ds
     * @param dd
     * @param dm
     */
    private static List<Insight> check(List<Insight> insights, Dataset ds, DatasetDimension dd, DatasetMeasure dm, int permNb){
        HashMap<String, List<Double>> cache = new HashMap<>();
        try (Statement st = ds.getConn().createStatement()){
            ResultSet rs = st.executeQuery("select " + dd.getName() + ", " + dm.getName() + " from " + ds.getTable() + ";");
            rs.next();
            while (rs.next()) {
                String key = rs.getString(1);
                double val = rs.getDouble(2);
                cache.computeIfAbsent(key, i_ -> new ArrayList<>());
                cache.get(key).add(val);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        List<Insight> toAdd = new ArrayList<>();
        for (int i = 0; i < insights.size(); i++) {
            Insight in = insights.get(i);
            toAdd.addAll(compute_mean(in, cache.get(in.selA).stream().mapToDouble(d -> d).toArray(), cache.get(in.selB).stream().mapToDouble(d -> d).toArray(), permNb));
        }
        insights.addAll(toAdd);
        return insights;
    }

    private static List<Insight> compute_mean(Insight i, double[] a, double[] b, int permNb) {
        List<Insight> added = new ArrayList<>(2);
        int n_threshold = 5;
        if (a.length <= n_threshold || b.length <= n_threshold){
            i.setP(1.0);
            return List.of();
        }

        // Mean Smaller
        double base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        double[] perm_stats = Permutations.mean(a, b, permNb);
        int count = 0;
        for (int j = 0; j < permNb; j++) {
            if (perm_stats[j] > base_stat)
                count++;
        }
        i.setP((count)/((double) permNb));

        // Mean higher
        base_stat = -base_stat;
        count = 0;
        for (int j = 0; j < permNb; j++) {
            if (-perm_stats[j] > base_stat)
                count++;
        }
        Insight tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_GREATER);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));

        // Mean equals
        base_stat = Math.abs(base_stat);
        count = permNb;
        for (int j = 0; j < permNb; j++) {
            if (Math.abs(perm_stats[j]) > base_stat)
                count--;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_EQUALS);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));

        if ( ( boolToInt(added.get(1).getP()  < 0.05) + boolToInt(added.get(0).getP() < 0.05) + boolToInt(i.getP() < 0.05)) > 1)
            System.out.println("calling debugger");
        return added;
    }

    private static int boolToInt(boolean b) {
        return Boolean.compare(b, false);
    }

}
