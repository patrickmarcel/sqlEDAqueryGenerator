package fr.univtours.info;

import com.alexscode.utilities.Stuff;
import com.alexscode.utilities.collection.Pair;
import com.alexscode.utilities.math.BenjaminiHochbergFDR;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.optimize.CPLEXTAP;
import fr.univtours.info.optimize.NaiveTAP;
import fr.univtours.info.optimize.TAPEngine;
import fr.univtours.info.queries.*;
import me.tongfei.progressbar.ProgressBar;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Deprecated
public class Generator {
    static Dataset ds;
    static String table;
    static String sampleTable;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static Connection sample_db;
    static DBConfig config;
    public static CandidateQuerySet theQ = new CandidateQuerySet();

    public static PrintStream devOut;

    static final String[] tabAgg = {"avg", "sum", "count"};//"min", "max",

    public static void main( String[] args ) throws Exception{
        //DEBUG
        devOut = new PrintStream(new FileOutputStream("data/logs/log_100.txt"));
        //Load config and base dataset
        init();

        //compute sample
        Class.forName(config.getSampleDriver());
        sample_db = DriverManager.getConnection(config.getSampleURL(), config.getSampleUser(), config.getSamplePassword());
        Dataset sample = ds.computeUniformSample(10, sample_db);

        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        generateSiblingAssesses();

        stopwatch.stop();

        // get elapsed time, expressed in milliseconds
        long timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Generation time in milliseconds: " + timeElapsed);
        System.out.println("Q size: " + theQ.size() + " queries generated");


        // interestingness computation
        System.out.println("Starting interestingness computation");
        stopwatch = Stopwatch.createStarted();
        //theQ.shrink();
        computeInterests(ds);

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Interestingness computation time in milliseconds: " + timeElapsed);

        // actual cost computation
        System.out.println("Starting cost computation");
        stopwatch = Stopwatch.createStarted();

        //computeCosts();
        //TODO useless for now as everything takes about 5ms
        for (AssessQuery assessQuery : theQ) {
            assessQuery.explainAnalyze();
        }

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Cost computation time in milliseconds: " + timeElapsed);

        //TAP
        List<AssessQuery> queries = new ArrayList<>(theQ);

        if (theQ.size() < 10000){
            TAPEngine exact = new CPLEXTAP("C:\\Users\\achan\\source\\repos\\cplex_test\\x64\\Release\\cplex_test.exe", "data/tap_instance.dat");
            List<AssessQuery> exactSolution = exact.solve(queries, 25, 150);
            NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
            exactSolution.forEach(out::addQuery);
            Files.write(Paths.get("data/outpout_exact.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            System.err.println("[WARNING] Couldn't run exact solver : too many queries");
        }

        //Naive heuristic from 2020 paper
        TAPEngine naive = new NaiveTAP();
        List<AssessQuery> naiveSolution = naive.solve(queries, 25, 150);
        NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
        naiveSolution.forEach(out::addQuery);
        Files.write(Paths.get("data/outpout.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));


        conn.close();
        sample_db.close();
    }

    public static void init() throws IOException, SQLException{
        config = DBConfig.newFromFile();
        conn = config.getConnection();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();
        ds=new Dataset(conn, table, theDimensions, theMeasures);
    }
/*
    // todo: check if optimization is reasonable in practice...
    static void computeCosts() throws Exception {
        long current=0;
        int nbExplain=0;
        AssessQuery previous=null;
        int correct = 0, wrong = 0, bad = 0;
        ArrayList<Float> errors = new ArrayList<>(10000);
        ArrayList<Float> times = new ArrayList<>(theQ.size());

        for(AssessQuery q : theQ){
            //q.explainAnalyze();

            // THIS MUST BE CHECKED BECAUSE EVEN WHEN DISTANCE IS 0 THE COST MAY DIFFER
            // ESPECIALLY WHEN VALUES IN SELECTION PREDICATE ARE ORDERED
            if(previous!=null && q.getDistance(previous)==0){
                q.setEstimatedCost(current);
                //error check start
                q.explain();
                times.add(q.estimatedTime());
                long error = Math.abs(q.estimatedTime() - current);

                if (error < 0.1){
                    correct += 1; }
                else {
                    wrong += 1;
                    errors.add(error);
                    if (error > 100)
                        bad += 1;
                }
                //error check end
                q.setEstimatedCost(current);

            }
            else{
                q.explain();
                nbExplain++;

            }
            current=q.getEstimatedCost();
            previous=q;


        }
        float mean = (float) times.stream().mapToDouble(Float::doubleValue).average().orElse(-1);
        double variance = times.stream()
                .map(k -> k - mean)
                .map(k -> k*k)
                .mapToDouble(k -> k).average().getAsDouble();

        System.out.printf("Global time infos, Min %s, Max %s, AVG %s, STD %s%n", times.stream().mapToDouble(Float::doubleValue).min(), times.stream().mapToDouble(Float::doubleValue).max(),mean,Math.sqrt(variance));
        System.out.println("Number of explain executed: "+nbExplain);
        System.out.printf("Checking optimization : Correct %s, Wrong %s, >100ms  %s%n", correct, wrong, bad);
        float mae = (float) errors.stream().mapToDouble(Float::doubleValue).average().orElse(-1);
        double mae_variance = errors.stream()
                .map(k -> k - mae)
                .map(k -> k*k)
                .mapToDouble(k -> k).average().getAsDouble();
        System.out.printf("MAE %s, std %s%n",mae , Math.sqrt(mae_variance));
    }*/

    public static void computeInterests(Dataset sample) throws Exception {
        List<AssessQuery> toEvaluate = new ArrayList<>(theQ);
        double[] pPearson = new double[toEvaluate.size()];
        double[] pT = new double[toEvaluate.size()];
        double[] pF = new double[toEvaluate.size()];
        try (ProgressBar progress = new ProgressBar("Performing statistical tests", toEvaluate.size())) {
            for (int i = 0; i < toEvaluate.size(); i++){
                //Run the query
                AssessQuery assess = (AssessQuery) toEvaluate.get(i);
                assess.execute();
                //Perform statistical tests on output
                Pair<Double, Double> res = assess.pearsonTest(false);
                pPearson[i] = res.getB();
                pT[i] = assess.TTest(false);
                pF[i] = assess.FTest(true);
                //Progress
                progress.step();
            }
        }
        // Account for MCP
        BenjaminiHochbergFDR corrector = new BenjaminiHochbergFDR(pPearson);
        pPearson = corrector.getAdjustedPvalues();

        corrector = new BenjaminiHochbergFDR(pT);
        pT = corrector.getAdjustedPvalues();

        corrector = new BenjaminiHochbergFDR(pF);
        pF = corrector.getAdjustedPvalues();

        String[] testNames = new String[]{"Correlation", "Different Means", "Different Variances"};
        for (int j = 0; j < toEvaluate.size(); j++) {
            double mostSignificant = Stuff.arrayMin(pPearson[j], pT[j], pF[j]);
            String tests = "Statistically significant relation(s) (p<0.05) between columns: ";
            if (pPearson[j] < 0.05)
                tests += testNames[0];
            if (pT[j] < 0.05)
                tests += ", " + testNames[1];
            if (pF[j] < 0.05)
                tests += ", " + testNames[2];

            ((AssessQuery) toEvaluate.get(j)).setTestComment(tests);
            toEvaluate.get(j).setInterest(1d - mostSignificant);
        }
    }



    public static void generateSiblingAssesses() {
        ImmutableSet<DatasetDimension> set = ImmutableSet.copyOf(theDimensions);
        Set<Set<DatasetDimension>> combinations = Sets.combinations(set, 2);
        Map<Pair<DatasetDimension, DatasetDimension>, Boolean> isAllowed = new HashMap<>();

        //For each pair of dimension attributes
        for(Set<DatasetDimension> s : combinations){
            Iterator<DatasetDimension> it = s.iterator();
            Pair<DatasetDimension, DatasetDimension> dims = new Pair<>(it.next(), it.next());
            Pair<DatasetDimension, DatasetDimension> dims_r = new Pair<>(dims.getSecond(), dims.getFirst());

            List<Pair<DatasetDimension, DatasetDimension>> toGenerate = new ArrayList<>(2);
            if (isAllowed.computeIfAbsent(dims, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table)) &&
                    isAllowed.computeIfAbsent(dims_r, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table))) {
                // generate comparisons 'both ways'
                toGenerate.add(dims);
                //toGenerate.add(dims_r);
            }

            for (Pair<DatasetDimension, DatasetDimension> dim_pair : toGenerate){
                for(DatasetMeasure meas : theMeasures) {
                    for (String agg : tabAgg) {

                        ImmutableSet<String> values = ImmutableSet.copyOf(dim_pair.getFirst().getActiveDomain());
                        Set<Set<String>> combiVals = Sets.combinations(values, 2);

                        for(Set<String> st : combiVals){
                            int i=0;
                            String[] tabstring = new String[2];
                            for(String sc : st){
                                tabstring[i++]=sc;
                            }
                            //System.out.println(tabdim[0] + tabstring[0] +  tabstring[1] +  tabdim[1] );
                            AssessQuery saq = new AssessQuery(conn, table, dim_pair.getFirst(), tabstring[0], tabstring[1], dim_pair.getSecond(), meas, agg);
                            //System.out.println(saq);
                            theQ.addQuery(saq);

                        }
                    }
                }
            }
        }
    }

    static String getId(AssessQuery q){
        String id = "\"" + q.getFunction() + ":" + q.getMeasure().getName() + ":" + q.getReference().getName() + ":" + q.getAssessed().getName() + ":" + ((AssessQuery) q).getVal1() + ":" + ((AssessQuery) q).getVal2() + "\"";
        return id;
    }

}
