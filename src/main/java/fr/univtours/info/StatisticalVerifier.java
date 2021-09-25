package fr.univtours.info;

import com.alexscode.utilities.math.BenjaminiHochbergFDR;
import com.alexscode.utilities.math.Permutations;
import com.google.common.base.Stopwatch;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import org.apache.commons.math3.stat.StatUtils;

import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.univtours.info.Insight.*;
import static fr.univtours.info.Insight.VARIANCE_GREATER;

public class StatisticalVerifier {
    public static int n_threshold = 10;

    /**
     * Prototype for checking one insight at a time (only mean)
     * @param i the insight to check
     * @param ds the dataset object to test on
     * @return the p value ofr the insight
     */
    @Deprecated
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
    public static List<Insight> check(List<Insight> insights, Dataset ds, double sigLevel, int permNb, double sampleRatio, DBConfig config, boolean keep_transitive) {
        Map<DatasetDimension, Dataset> samples = new HashMap<>();
        Connection sample_db;
        if (sampleRatio < 1.0) {
            try {
                Class.forName(config.getSampleDriver());
                sample_db = DriverManager.getConnection(config.getSampleURL(), config.getSampleUser(), config.getSamplePassword());
                for (DatasetDimension d : ds.getTheDimensions()) {
                    if (MainTAP.USE_UNIFORM_SAMPLING)
                        samples.put(d, ds.computeUniformSample(sampleRatio, sample_db));
                    else
                        samples.put(d, ds.computeStatisticalSample(d, (int) (ds.getTableSize() * sampleRatio), sample_db));
                }
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        List<Insight> output = new ArrayList<>();

        // Group insights per dimensions
        for (var perDimMap : insights.parallelStream().collect(Collectors.groupingByConcurrent(Insight::getDim)).entrySet()) {
            List<Insight> insightsForD = perDimMap.getValue();
            DatasetDimension thisDimension = perDimMap.getKey();
            // This gets us insights grouped by measure and dimension
            for (var kv : insightsForD.parallelStream().collect(Collectors.groupingByConcurrent(Insight::getMeasure)).entrySet()) {
                DatasetMeasure thisMeasure = kv.getKey();
                System.out.println("[INFO] Working on " + thisDimension + "/" + thisMeasure + " | Size = " + kv.getValue().size() * pprint.length);
                Stopwatch stopwatch = Stopwatch.createStarted();
                // Handle sampling if needed
                List<Insight> thisDimAndMeasure = null;
                if (sampleRatio < 1.0)
                    thisDimAndMeasure = check(kv.getValue(), samples.get(thisDimension), thisDimension, thisMeasure, permNb);
                else
                    thisDimAndMeasure = check(kv.getValue(), ds, thisDimension, thisMeasure, permNb);
                System.out.println("[VERIF][TIME][s] p-values " + stopwatch.elapsed(TimeUnit.SECONDS));
                // FDR compensation for multiple testing problem
                double[] p = thisDimAndMeasure.stream().mapToDouble(Insight::getP).toArray();
                BenjaminiHochbergFDR corrector = new BenjaminiHochbergFDR(p);
                p = corrector.getAdjustedPvalues();
                for (int i = 0; i < p.length; i++) thisDimAndMeasure.get(i).setP(p[i]);
                thisDimAndMeasure.removeIf(insight -> insight.getP() > sigLevel);

                //Identify Triangles for transitivity elimination
                if (!keep_transitive) {
                    stopwatch = Stopwatch.createStarted();
                    thisDimAndMeasure.parallelStream()
                            .filter(insight -> insight.type == MEAN_SMALLER || insight.type == MEAN_GREATER || insight.type == VARIANCE_SMALLER || insight.type == VARIANCE_GREATER)
                            .collect(Collectors.groupingByConcurrent(Insight::getType)).entrySet().parallelStream().forEach(e -> {
                        var list = e.getValue();
                        Set<String> things = list.stream().map(i -> i.selA + "_" + i.selB).collect(Collectors.toSet());
                        for (Insight ac : list) {
                            thisDimension.getActiveDomain().parallelStream().forEach(b -> {
                                if (things.contains(ac.getSelA() + "_" + b) && things.contains(b + "_" + ac.getSelB()))
                                    ac.setP(-1); //mark for delete
                            });
                        }
                    });
                    thisDimAndMeasure.removeIf(insight -> insight.getP() == -1);// delete
                    System.out.println("[VERIF][TIME][s] triangles " + stopwatch.elapsed(TimeUnit.SECONDS));
                }
                output.addAll(thisDimAndMeasure);
            }
        }

        if (sampleRatio < 1.0){
            samples.values().forEach(Dataset::drop);
        }

        return output;
    }

    /**
     * Internal check in bulk (same dimension/measure)
     * @param insights insights on the same measure and dimension
     */
    private static List<Insight> check(List<Insight> insights, Dataset ds, DatasetDimension dd, DatasetMeasure dm, int permNb){
        HashMap<String, List<Double>> cache = new HashMap<>();

        try (Statement st = ds.getConn().createStatement()){
            ResultSet rs = st.executeQuery("select " + dd.getName() + ", " + dm.getName() + " from " + ds.getTable() + ";");
            while (rs.next()) {
                String key = rs.getString(1);
                double val = rs.getDouble(2);
                cache.computeIfAbsent(key, i_ -> new ArrayList<>());
                cache.get(key).add(val);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


        List<Insight> toAdd;
        // Switch to parallel processing if more large number of insights are available
        if (insights.size() > 50){
            toAdd = insights.parallelStream()
                .map(in -> {
                    //Sample is too small
                    if (cache.get(in.selA) == null || cache.get(in.selB) == null) {
                        in.setP(1);
                        System.err.println("[Warning] Sample too small for " + in.getDim() + " values '" + in.selA + "' and/or '" + in.selB + "'.");
                        return List.of(in);
                    }
                    return computeMeanAndVariance(in, cache.get(in.selA).stream().mapToDouble(d -> d).toArray(), cache.get(in.selB).stream().mapToDouble(d -> d).toArray(), permNb);
                })
                .flatMap(Collection::stream).collect(Collectors.toList());
        } else {
            toAdd = new ArrayList<>();
            for (Insight in : insights) {
                // Check that no dimensions is floating point if crash in here
                toAdd.addAll(computeMeanAndVariance(in, cache.get(in.selA).stream().mapToDouble(d -> d).toArray(), cache.get(in.selB).stream().mapToDouble(d -> d).toArray(), permNb));

            }
        }

        return toAdd;
    }


    private static List<Insight> computeMeanAndVariance(Insight i, double[] a, double[] b, int permNb) {
        List<Insight> added = new ArrayList<>(10);

        //Sample size too small
        if (a.length <= n_threshold || b.length <= n_threshold){
            i.setP(1.0);
            return List.of();
        }

        var perm_stats = Permutations.meanAndvariance(a, b, permNb);
        double[] meanStats = perm_stats.getA();
        double[] varStats = perm_stats.getB();

        // variance Smaller
        double base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        int count = 0;
        for (int j = 0; j < permNb; j++) {
            if (varStats[j] > base_stat)
                count++;
        }
        i.setType(Insight.VARIANCE_SMALLER);
        added.add(i);
        i.setP((count)/((double) permNb));

        // variance higher
        base_stat = -base_stat;
        count = 0;
        for (int j = 0; j < permNb; j++) {
            if (-varStats[j] > base_stat)
                count++;
        }
        Insight tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.VARIANCE_GREATER);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));

        // variance equals
        base_stat = Math.abs(base_stat);
        count = permNb;
        for (int j = 0; j < permNb; j++) {
            if (Math.abs(varStats[j]) > base_stat)
                count--;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.VARIANCE_EQUALS);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));


        // Mean Smaller
        base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        count = 0;
        for (int j = 0; j < permNb; j++) {
            if (meanStats[j] > base_stat)
                count++;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_SMALLER);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));

        // Mean higher
        base_stat = -base_stat;
        count = 0;
        for (int j = 0; j < permNb; j++) {
            if (-meanStats[j] > base_stat)
                count++;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_GREATER);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));

        // Mean equals
        base_stat = Math.abs(base_stat);
        count = permNb;
        for (int j = 0; j < permNb; j++) {
            if (Math.abs(meanStats[j]) > base_stat)
                count--;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_EQUALS);
        added.add(tmp);
        tmp.setP((count)/((double) permNb));


        return added;
    }


    /**
     * Finds relevant mean relation between samples and associated p-value by permutation testing
     * @param i the base insight
     * @param a reference sample
     * @param b other sample
     * @param permNb number of permutations to perform usually 1000 or higher
     * @return
     */
    private static List<Insight> compute_mean(Insight i, double[] a, double[] b, int permNb) {
        List<Insight> added = new ArrayList<>(2);

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
