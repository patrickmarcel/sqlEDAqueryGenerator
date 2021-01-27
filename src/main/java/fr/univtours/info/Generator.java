package fr.univtours.info;

import com.alexscode.utilities.collection.Pair;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import fr.univtours.info.optimize.AprioriMetric;
import fr.univtours.info.optimize.BudgetManager;
import fr.univtours.info.optimize.KnapsackManager;
import fr.univtours.info.optimize.tsp.LinKernighan;
import fr.univtours.info.optimize.tsp.TSP;
import fr.univtours.info.queries.*;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 *
 */
public class Generator {
    static Dataset ds;
    static String table;
    static String sampleTable;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static Connection sample_db;
    static Config config;
    public static CandidateQuerySet theQ = new CandidateQuerySet();

    public static PrintStream devOut;

    static final String[] tabAgg= {"avg", "sum", "min", "max", "stddev"};

    public static void main( String[] args ) throws Exception{
        //DEBUG
        devOut = new PrintStream(new FileOutputStream("data/logs/log_100.txt"));
        //Load config and base dataset
        init();

        //compute sample
        //PreparedStatement cleanup = conn.prepareStatement("Drop table if exists sample_covid");
        //cleanup.execute();
        //cleanup.close();
        Class.forName(config.getSampleDriver());
        sample_db = DriverManager.getConnection(config.getSampleURL(), config.getSampleUser(), config.getSamplePassword());
        Dataset sample = ds.computeSample(10, sample_db);


        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        //generateCounts();
        //generateHistograms();
        //generateAggregates();
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
        computeInterests(sample);

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Interestingness computation time in milliseconds: " + timeElapsed);

        // actual cost computation
        System.out.println("Starting cost computation");
        stopwatch = Stopwatch.createStarted();

        //computeCosts();
        theQ.theQueries.forEach(q -> q.setEstimatedCost(5));//TODO useless for now as everything takes about 5ms

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Cost computation time in milliseconds: " + timeElapsed);

        // Do the TAP dance
        List<AbstractEDAsqlQuery> toRun = new ArrayList<>(theQ);
        toRun.sort(Comparator.comparingDouble(AbstractEDAsqlQuery::getInterest).reversed());
        toRun = toRun.subList(0, 20);

        toRun = TSP.orderByTSP(toRun);
        Notebook out = new Notebook();
        toRun.forEach(out::addQuery);
        System.out.println(out.toJson());
        Files.write(Paths.get("data/outpout.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));


        conn.close();
        sample_db.close();
    }

    public static void init() throws IOException, SQLException{
        DBservices db= new DBservices();
        conn=db.connectToPostgresql();
        config = Config.readProperties();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();
        ds=new Dataset(conn, table, theDimensions, theMeasures);
    }

    // todo: check if optimization is reasonable in practice...
    static void computeCosts() throws Exception {
        float current=0;
        int nbExplain=0;
        AbstractEDAsqlQuery previous=null;
        int correct = 0, wrong = 0, bad = 0;
        ArrayList<Float> errors = new ArrayList<>(10000);
        ArrayList<Float> times = new ArrayList<>(theQ.size());

        for(AbstractEDAsqlQuery q : theQ.theQueries){
            //q.explainAnalyze();

            // THIS MUST BE CHECKED BECAUSE EVEN WHEN DISTANCE IS 0 THE COST MAY DIFFER
            // ESPECIALLY WHEN VALUES IN SELECTION PREDICATE ARE ORDERED
            if(previous!=null && q.getDistance(previous)==0){
                q.setEstimatedCost(current);
                //error check start
                q.explain();
                times.add(q.getEstimatedCost());
                float error = Math.abs(q.getEstimatedCost() - current);

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
    }

    public static void computeInterests(Dataset sample) throws Exception {
        int i = 0;
        for(AbstractEDAsqlQuery q : theQ.theQueries){
            //SampleQuery qs = new SampleQuery(q, sample);
            //q.printResult();
            //devOut.print(getId((SiblingAssessQuery) q) + ",");
            q.execute();
            q.computeInterest();
            //System.out.println("interestingness: " + q.getInterest());
            if (i++ % 10000 == 0)
                System.out.println("Q"+i);
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

            //TODO compute if absent in hashmap
            List<Pair<DatasetDimension, DatasetDimension>> toGenerate = new ArrayList<>(2);
            if (isAllowed.computeIfAbsent(dims, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table)) &&
                    isAllowed.computeIfAbsent(dims_r, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table))) {
                toGenerate.add(dims);
                toGenerate.add(dims_r);
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
                            SiblingAssessQuery saq = new SiblingAssessQuery(conn, table, dim_pair.getFirst(), tabstring[0], tabstring[1], dim_pair.getSecond(), meas, agg);
                            //System.out.println(saq);
                            theQ.addQuery(saq);

                        }
                    }
                }
            }
        }
    }

    static void generateAggregates() throws Exception {
        ImmutableSet<DatasetDimension> set = ImmutableSet.copyOf(theDimensions);
        Set<Set<DatasetDimension>> powerSet = Sets.powerSet(set);

        for(Set<DatasetDimension> s : powerSet) {
            for(DatasetMeasure meas : theMeasures) {
                for (String agg : tabAgg) {
                    AggregateQuery aq = new AggregateQuery(conn, table, s, meas, agg);
                    theQ.addQuery(aq);

                    //aq.execute();
                    //aq.explainAnalyze();

                    //System.out.println(aq.sql);
                    //System.out.println(aq.cost);
                }
            }
        }
    }

    static void generateHistograms(){
        for(DatasetDimension d : theDimensions){
            HistogramQuery hq = new HistogramQuery(conn, table, d);
            theQ.addQuery(hq);
        }
    }

    static void generateCounts() throws Exception {
        for(DatasetDimension dim : theDimensions){
            CountdistinctQuery cdq= new CountdistinctQuery(conn, table, dim) ;
            theQ.addQuery(cdq);
            //cdq.execute();
            //cdq.explainAnalyze();

        }

    }

    static String getId(SiblingAssessQuery q){
        String id = "\"" + q.getFunction() + ":" + q.getMeasure().getName() + ":" + q.getReference().getName() + ":" + q.getAssessed().getName() + ":" + ((SiblingAssessQuery) q).getVal1() + ":" + ((SiblingAssessQuery) q).getVal2() + "\"";
        return id;
    }

}
