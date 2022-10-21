package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.optimize.CPLEXTAP;
import fr.univtours.info.optimize.KnapsackStyle;
import fr.univtours.info.optimize.TAPEngine;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.Query;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExpeColGen {

    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;

    public static String CPLEX_BIN = "/home/alex/tap_vanilla";
    static int EPS_TIME = 10, MAX_DISTANCE = 250;
    public static boolean CUDA_PRESENT = false;

    public static Map<String, Double> dimWeights = new HashMap<>();
    public static Map<String, Double> dimTimes = new HashMap<>();
    static {
        int SELECTED = 3;

        switch (SELECTED) {
            //Vaccines
            case 1:
            DBConfig.CONF_FILE_PATH = "src/main/resources/vaccines.properties";
            dimTimes.put("year", 1.6);
            dimTimes.put("month", 1.2);
            dimTimes.put("country", 2d);
            dimTimes.put("continent", 1.);
            dimTimes.put("vaccine", 1.1d);
            dimWeights.put("year", 1d);
            dimWeights.put("month", 1.2);
            dimWeights.put("country", 0.8);
            dimWeights.put("continent", 1d);
            dimWeights.put("vaccine", 1d); break;

            //Diabetes
            case 2:
            DBConfig.CONF_FILE_PATH = "src/main/resources/diabetes.properties";
            dimTimes.put("Outcome", 1.7);
            dimTimes.put("Age", 1.1d);
            dimTimes.put("Pregnancies", 1.8);
            dimWeights.put("Outcome", 1.2);
            dimWeights.put("Age", 1d);
            dimWeights.put("Pregnancies", 0.8); break;

            //Violence against women
            case 3:
            DBConfig.CONF_FILE_PATH = "src/main/resources/violence.properties";
            dimTimes.put("Country", 1.7d);
            dimTimes.put("Gender", 1.1);
            dimTimes.put("Question", 1.8);
            dimTimes.put("Demographics Response", 1.1);
            dimWeights.put("Country", 1.2d);
            dimWeights.put("Gender", 1.);
            dimWeights.put("Question", 0.8);
            dimWeights.put("Demographics Response", 0.9); break;

            //Students
            case 4:
            DBConfig.CONF_FILE_PATH = "src/main/resources/students_data.properties";
            dimWeights.put("sex", 0.8);
            dimWeights.put("famsize", 1.2);
            dimWeights.put("guardian", 1d);
            dimWeights.put("Pstatus", 1.1d);
            dimWeights.put("internet", 1d);
            dimTimes.put("sex", 0.9);
            dimTimes.put("famsize", 1.2);
            dimTimes.put("guardian", 1.d);
            dimTimes.put("Pstatus", 1.d);
            dimTimes.put("internet", 1.4d);break;

            //Ramen
            case 5:
            DBConfig.CONF_FILE_PATH = "src/main/resources/ramen.properties";
            dimTimes.put("Brand", 1.7);
            dimTimes.put("Style", 1.1d);
            dimTimes.put("Country", 1.4d);
            dimWeights.put("Brand", 0.8);
            dimWeights.put("Style", 1.1d);
            dimWeights.put("Country", 1d);break;
        }
    }

    public static void init() throws IOException, SQLException {
        config = DBConfig.newFromFile();
        conn = config.getConnection();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();
        ds = new Dataset(conn, table, theDimensions, theMeasures);
        System.out.println("Connection to database successful");
        System.out.print("Collecting statistics ... ");
        stats = new DatasetStats(ds);
        System.out.println(" Done");
    }

    public static void main( String[] args ) throws Exception{

        System.out.println("[INFO] CPU Threads/Cores: " + Runtime.getRuntime().availableProcessors() + " | " + "Streams will use : " + ForkJoinPool.commonPool().getParallelism());

        //Attempting to detect cuda
        try {
            CudaRand dummy = new CudaRand();
            dummy.nextInt(10);
            CUDA_PRESENT = true;
        }catch (UnsatisfiedLinkError e){
            CUDA_PRESENT = false;
            System.out.println("[INFO] Couldn't initialize cuda runtime falling back tu CPU");
        }

        //Load config and base dataset
        init();
        conn.setReadOnly(true);

        //generation
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Query> tapQueries = new ArrayList<>();
        for (DatasetDimension dim : ds.getTheDimensions()) {
            ImmutableSet<String> values = ImmutableSet.copyOf(dim.getActiveDomain());
            Set<List<String>> combiVals = Sets.combinations(values, 2).stream().map(ArrayList::new).collect(Collectors.toSet());
            //TODO remove me
            //combiVals = StreamSupport.stream(Iterables.limit(combiVals, 20).spliterator(), false).collect(Collectors.toSet());
            for (List<String> pair : combiVals) {
                for (DatasetMeasure m : ds.getTheMeasures()){
                    for (DatasetDimension otherDim : ds.getTheDimensions()){
                        if (!otherDim.equals(dim)){
                            tapQueries.add(new AssessQuery(conn, ds.getTable(), dim, pair.get(0), pair.get(1), otherDim, m, "sum"));
                        }
                    }
                }
            }
        }

        // Fetching runtime
        //ConnectionPool cp = new ConnectionPool(config);
        System.out.println("[INFO] Estimating queries ("+tapQueries.size()+") runtime ... ");
        tapQueries.stream().parallel().forEach(q -> {
            //Connection c = cp.getConnection();
            q.explain();
            //((AssessQuery)q).setActualCost(q.estimatedTime());
            double tmp = dimTimes.get(((AssessQuery)q).getReference().getName().replace("\"",""));
            ((AssessQuery)q).setActualCost(Math.round(tmp));
            ((AssessQuery)q).setExplainCost(Math.round(tmp));
            //cp.returnConnection(c);
        });
        //cp.close();

        // Interest function
        tapQueries.stream().parallel().forEach(q -> {
            q.setInterest(dimWeights.get(((AssessQuery)q).getAssessed().getName().replace("\"","")) + dimWeights.get(((AssessQuery)q).getReference().getName().replace("\"","")));
        });

        stopwatch.stop();
        System.out.println("[TIME][ms] Generation " + stopwatch.elapsed(TimeUnit.MILLISECONDS));

        // dump data for learning
        System.out.println("[INFO] Attempting to dump query info ...");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/tap_queries_dump.csv"))){
            out.write("qid,measure,function,ref,val1,val2,val1_f,val2_f,gb_ad_size,agg_size,interest\n");
            int n = 0;
            for (Query q : tapQueries){
                AssessQuery query = (AssessQuery) q;
                out.write(n + ",");
                out.write(query.getMeasure() + ",");
                out.write(query.getFunction() + ",");
                out.write(query.getReference() + ",");
                out.write(query.getVal1() + ",");
                out.write(query.getVal2() + ",");
                out.write( stats.getFrequency().get(query.getAssessed()).get(query.getVal1()) + ",");
                out.write( stats.getFrequency().get(query.getAssessed()).get(query.getVal2()) + ",");
                out.write( stats.getAdSize().get((query.getAssessed())) + ",");
                out.write( stats.estimateAggregateSize(List.of(query.getReference())) + ",");
                out.write(String.valueOf(query.getInterest()));
                out.write("\n");
                n++;
            }
            System.out.println("[INFO] Success");
        } catch (IOException e){
            e.printStackTrace();
        }

        // --- SOLVING TAP ----
        System.out.println("[INFO] Started solving TAP instance ...");
        stopwatch = Stopwatch.createStarted();


        // Naive heuristic
        TAPEngine naive = new KnapsackStyle();
        List<AssessQuery> naiveSolution = naive.solve(tapQueries, EPS_TIME, MAX_DISTANCE).stream()
                .map(AssessQuery.class::cast).collect(Collectors.toList());
        //naiveSolution.forEach(q -> q.setTestComment(supports.get(q).stream().map(Insight::toString).collect(Collectors.joining(", "))));
        NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
        naiveSolution.forEach(out::addQuery);
        System.out.println("[INFO] KS solution is " + naiveSolution.size() + " queries long");
        //Files.writeString(Paths.get("data/KS_" + INTERESTINGNESS + "_" + QUERIESNB + printSample + LocalTime.now().toString().replace(':', '-')+".ipynb"), out.toJson());

        stopwatch.stop();
        System.out.println("[TIME][ms] Heuristic " + stopwatch.elapsed(TimeUnit.MILLISECONDS));


        if (tapQueries.size() < 50000 && ! CPLEX_BIN.equals("")){
            TAPEngine exact = new CPLEXTAP(CPLEX_BIN, "data/tap_instance.dat");
            List<AssessQuery> exactSolution = exact.solve(tapQueries, EPS_TIME, MAX_DISTANCE).stream()
                    .map(AssessQuery.class::cast).collect(Collectors.toList());
            //exactSolution.forEach(q -> q.setTestComment(supports.get(q).stream().map(Insight::toString).collect(Collectors.joining(", "))));
            out = new NotebookJupyter(config.getBaseURL());
            exactSolution.forEach(out::addQuery);
            //Files.write(Paths.get("data/outpout_exact.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));
            System.out.println("[INFO] EXACT solution is " + exactSolution.size() + " queries long");
            //Files.writeString(Paths.get("data/EXACT_" + INTERESTINGNESS + "_" + QUERIESNB + printSample +LocalTime.now().toString().replace(':', '-')+".ipynb"), out.toJson());
        } else {
            if (tapQueries.size() > 1000) System.err.println("[WARNING] Couldn't run exact solver : too many queries");
            else System.err.println("[WARNING] No CPLEX binary defined with parameter -c");
        }

        conn.close();
    }
}
