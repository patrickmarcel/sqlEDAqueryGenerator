package fr.univtours.info;

import com.alexscode.utilities.math.*;
import com.google.common.base.Stopwatch;
import com.google.common.math.BigIntegerMath;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import org.apache.commons.math3.stat.StatUtils;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.univtours.info.Insight.*;

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
                Dataset sample = null;
                if (MainTAP.USE_UNIFORM_SAMPLING) sample = ds.computeUniformSample(sampleRatio, sample_db);
                for (DatasetDimension d : ds.getTheDimensions()) {
                    if (MainTAP.USE_UNIFORM_SAMPLING)
                        samples.put(d, sample);
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
                output.addAll(thisDimAndMeasure);
            }
        }

        if (sampleRatio < 1.0 && !MainTAP.USE_UNIFORM_SAMPLING) samples.values().forEach(Dataset::drop);

        return output;
    }

    /**
     * Internal check in bulk (same dimension/measure)
     * @param insights insights on the same measure and dimension
     */
    //the parametric test counter
    static AtomicInteger tParam = new AtomicInteger(0);
    //the non-parametric test counter
    static AtomicInteger tPermu = new AtomicInteger(0);
    //the total test counter
    static AtomicInteger tTotal = new AtomicInteger(0);

    private static List<Insight> check(List<Insight> insights, Dataset ds, DatasetDimension dd, DatasetMeasure dm, int permNb){
        Stopwatch stopwatch = Stopwatch.createStarted();
        HashMap<String, double[]> cache = new HashMap<>();
        //Fetch samples
        try (Statement st = ds.getConn().createStatement()){
            ResultSet rs = st.executeQuery("select " + dd.getName() + ", " + dm.getName() + " from \"" + ds.getTable() + "\" order by "+dd.getName()+";");
            String prev = null;
            List<Double> tmp = null;
            while (rs.next()) {
                String key = rs.getString(1);
                double val = rs.getDouble(2);
                if (!Objects.equals(key, prev)){
                    if (tmp != null)
                        cache.put(prev, tmp.stream().mapToDouble(Double::doubleValue).toArray());
                    tmp = new ArrayList<>();
                    prev = key;
                }
                tmp.add(val);
            }
            if (tmp != null)
                cache.put(prev, tmp.stream().mapToDouble(Double::doubleValue).toArray());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        System.out.println("[VERIF][TIME][ms] dim cache " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        // Use cuda for RNG if present
        ThreadLocal<Random> rng;
        if (MainTAP.CUDA_PRESENT)
            rng = ThreadLocal.withInitial(CudaRand::new);
        else
            rng = ThreadLocal.withInitial(ThreadLocalRandom::current);

        List<Insight> resultList = insights.stream().parallel()
                .filter(in -> cache.containsKey(in.selA) && cache.containsKey(in.selB) && cache.get(in.selA).length >= n_threshold && cache.get(in.selA).length >= n_threshold)
                .flatMap(in -> {
                    //boolean usePerm = true;
                    //TODO Logic to check if parametric test is possible goes here
                    // in : the insight
                    // cache.get(in.selA) : gets you the left sample
                    // cache.get(in.selB) : gets you the right sample

                    //Initialisation des tests de normalités
                    NormalityTest a = new NormalityTest(cache.get(in.selA));
                    NormalityTest b = new NormalityTest(cache.get(in.selB));

                   if ( NormalityTest.isNormal(a, b)){ // Check if the samples are considered normal

                      tParam.incrementAndGet();
                      tTotal.incrementAndGet();
                       // Use parametric tests (Welch's t-test)
                        return Arrays.stream(WelchTTest.MeanTest(in, a, b));
                    } else {
                       tPermu.incrementAndGet();
                       tTotal.incrementAndGet();
                       // Use non-parametric tests (permutation test)
                       return Arrays.stream(computeMeanAndVariance(in, cache.get(in.selA), cache.get(in.selB), permNb, rng.get()));
                   }
                }
                )
                .collect(Collectors.toList());

        System.out.println("pourcentage d'exécution des tests paramétriques : " + (double) tParam.get()/tTotal.get() *(100) );
        System.out.println("pourcentage d'exécution des tests non paramétriques : " + (double) tPermu.get()/tTotal.get() *(100));
        return resultList;
    }

    private static Insight[] computeMeanAndVariance(Insight i, double[] a, double[] b, int permutations, Random rd) {
        double[] meanDiffs = new double[permutations];
        double[] varDiffs = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        final int fullSize = ab.length;
        if (BigIntegerMath.binomial(fullSize, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(fullSize, a.length).intValue();
        }

        // Very low probability of drawing the same permutation twice ....
        //ThreadLocalRandom rd = null;
        PowerSet ps = null;
        boolean safe = fullSize <= 20;
        if (safe)
            ps = new PowerSet(ab);
        else {
            //rd = ThreadLocalRandom.current();
        }

        for (int i1 = 0; i1 < permutations; ++i1) {
            double mua = 0, mub = 0; // mean
            double muasq = 0, mubsq = 0; // mean of squares
            int countA = 0, countB = 0; // count

            if (safe) {
                BitSet pa;
                pa = ps.getNewRandomElementOFSize_new(a.length);
                for (int j1 = 0; j1 < fullSize; j1++) {
                    if (pa.get(j1)){
                        countA++;
                        mua += ab[j1];
                        muasq += ab[j1]*ab[j1];
                    } else {
                        countB ++;
                        mub += ab[j1];
                        mubsq += ab[j1]*ab[j1];
                    }
                }
            } else {
                boolean[] pa = new boolean[fullSize];
                for (int iter = 0; iter < a.length; iter++){
                    int pos = rd.nextInt(fullSize);
                    while (pa[pos]){
                        pos = rd.nextInt(fullSize);
                    }
                    pa[pos] = true;
                }
                for (int j1 = 0; j1 < fullSize; j1++) {
                    if (pa[j1]){
                        countA++;
                        mua += ab[j1];
                        muasq += ab[j1]*ab[j1];
                    } else {
                        countB ++;
                        mub += ab[j1];
                        mubsq += ab[j1]*ab[j1];
                    }
                }
            }

            //Compute means
            mub = mub/countB;
            mua = mua/countA;
            mubsq = mubsq/countB;
            muasq = muasq/countA;
            // Compute stat: var(b) - var(a)
            varDiffs[i1] =  (mubsq - (mub*mub)) - (muasq - (mua*mua));
            //Compute stat: E[b] - E[a]
            meanDiffs[i1] =  (mub/countB) - (mua/countA);
        }

        Insight[] added = new Insight[6];
        int addPos = 0;
        // variance Smaller
        double base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        int count = 0;
        for (int j = 0; j < permutations; j++) {
            if (varDiffs[j] > base_stat)
                count++;
        }
        i.setType(Insight.VARIANCE_SMALLER);
        added[addPos++] = i;
        i.setP((count)/((double) permutations));

        // variance higher
        base_stat = -base_stat;
        count = 0;
        for (int j = 0; j < permutations; j++) {
            if (-varDiffs[j] > base_stat)
                count++;
        }
        Insight tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.VARIANCE_GREATER);
        added[addPos++] = tmp;
        tmp.setP((count)/((double) permutations));

        // variance equals
        base_stat = Math.abs(base_stat);
        count = permutations;
        for (int j = 0; j < permutations; j++) {
            if (Math.abs(varDiffs[j]) > base_stat)
                count--;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.VARIANCE_EQUALS);
        added[addPos++] = tmp;
        tmp.setP((count)/((double) permutations));

        // Mean Smaller
        base_stat = StatUtils.mean(b) - StatUtils.mean(a);
        count = 0;
        for (int j = 0; j < permutations; j++) {
            if (meanDiffs[j] > base_stat)
                count++;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_SMALLER);
        added[addPos++] = tmp;
        tmp.setP((count)/((double) permutations));

        // Mean higher
        base_stat = -base_stat;
        count = 0;
        for (int j = 0; j < permutations; j++) {
            if (-meanDiffs[j] > base_stat)
                count++;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_GREATER);
        added[addPos++] = tmp;
        tmp.setP((count)/((double) permutations));

        // Mean equals
        base_stat = Math.abs(base_stat);
        count = permutations;
        for (int j = 0; j < permutations; j++) {
            if (Math.abs(meanDiffs[j]) > base_stat)
                count--;
        }
        tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_EQUALS);
        added[addPos++] = tmp;
        tmp.setP((count)/((double) permutations));

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
