package fr.univtours.info;

import com.alexscode.utilities.collection.Pair;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.TableFragment;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.optimize.CPLEXTAP;
import fr.univtours.info.optimize.KnapsackStyle;
import fr.univtours.info.optimize.TAPEngine;
import fr.univtours.info.queries.AssessQuery;
import org.apache.commons.cli.*;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainTAP {

    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static DBConfig config;

    //Default can be overridden by -c
    public static String CPLEX_BIN = "C:\\Users\\achan\\source\\repos\\cplex_test\\x64\\Release\\cplex_test.exe";

    public static void main( String[] args ) throws Exception{

        Options options = new Options();

        Option input = new Option("d", "database", true, "database config file path");
        input.setRequired(true);
        options.addOption(input);

        Option cplex = new Option("c", "cplex-binary", true, "path of binary for processing standard TAP instance");
        cplex.setRequired(false);
        options.addOption(cplex);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            DBConfig.CONF_FILE_PATH = cmd.getOptionValue("database");
            System.out.println("Config File :" + DBConfig.CONF_FILE_PATH);
            if (cmd.hasOption('c')){
                CPLEX_BIN = cmd.getOptionValue('c');
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        System.out.println("CPU Threads/Cores: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Streams will use : " + ForkJoinPool.commonPool().getParallelism());

        //Load config and base dataset
        init();
        conn.setReadOnly(true);

        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Insight> intuitions = new ArrayList<>(getIntuitions());

        stopwatch.stop();
        System.out.println("Generation time in seconds: " + stopwatch.elapsed(TimeUnit.SECONDS));
        System.out.println(intuitions.size() + " intuitions generated");

        //verification
        System.out.println("Starting verification");
        stopwatch = Stopwatch.createStarted();

        List<Insight> insights = StatisticalVerifier.check(intuitions, ds, 0.05, 1000);

        stopwatch.stop();
        System.out.println("Verification time in seconds: " + stopwatch.elapsed(TimeUnit.SECONDS));
        System.out.println("Nb of insights: " + insights.size());


        //support
        System.out.println("Started looking for supporting queries");
        stopwatch = Stopwatch.createStarted();

        Map<Insight, Set<AssessQuery>> isSupportedBy = new HashMap<>();

        // grouping insight by selection dimension
        insights.stream().collect(Collectors.groupingBy(Insight::getDim))
                .forEach((dimB, insightsOfDimB) ->{
            // Grouping again by measure
            insightsOfDimB.stream().collect(Collectors.groupingBy(Insight::getMeasure)).forEach( (measure, insightsOfDimBOverM) -> {
                // For every other dimension
                for (DatasetDimension dimA : ds.getTheDimensions().stream().filter(d -> !d.equals(dimB)).collect(Collectors.toList())){
                    try {
                        ResultSet rs = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                                .executeQuery("Select " + dimA.getName() + "," + dimB.getName() + "," + measure.getName() + " from " + ds.getTable() + ";");
                        TableFragment cache = new TableFragment(rs, ds.getTableSize());
                        //Go multithreaded and cache everything
                        List<Pair<Insight, AssessQuery>> validPairs = insightsOfDimBOverM.parallelStream()
                                .filter(insight -> {
                                    Pair<double[], double[]> res = cache.assess(insight.getSelA(), insight.getSelB(), MainTAP::sum);
                                    return querySupports(insight, res.getA(), res.getB());
                                })
                                .map(insight -> new Pair<>(insight, new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dimA, insight.getMeasure(), "sum")))
                                .collect(Collectors.toList());
                        //update the map
                        validPairs.forEach(insightQueryPair -> {
                            isSupportedBy.computeIfAbsent(insightQueryPair.getA(), k -> new HashSet<>());
                            isSupportedBy.get(insightQueryPair.getA()).add(insightQueryPair.getB());
                        });

                    } catch (SQLException e){
                        System.err.println("[ERROR] caching impossible");
                    }
                }
            });
        });
        /*
        for (Insight insight : insights){
            List<AssessQuery> sqs = getSupportingQueries(insight);
            if (sqs.size() == 0)
                orphan.add(insight);
            else{
                isSupportedBy.computeIfAbsent(insight, k-> new ArrayList<>());
                isSupportedBy.get(insight).addAll(sqs);
                for (AssessQuery q : sqs){
                    supports.computeIfAbsent(q, k -> new ArrayList<>());
                    supports.get(q).add(insight);
                }
            }

        }
        insights.removeAll(orphan);*/


        stopwatch.stop();
        System.out.println("Support time in seconds: " + stopwatch.elapsed(TimeUnit.SECONDS));
        System.out.println("Supported insights " + isSupportedBy.keySet().size());

        List<AssessQuery> tapQueries = new ArrayList<>();

        Map<AssessQuery, List<Insight>> supports = new HashMap<>();
        isSupportedBy.forEach(((insight, assessQueries) -> {
            assessQueries.forEach(q -> {
                supports.computeIfAbsent(q, k -> new ArrayList<>());
                supports.get(q).add(insight);
            });
        }));
        System.out.println("Total queries " + supports.keySet());
        for (AssessQuery q : supports.keySet()){
            tapQueries.add(q);
            q.explain();
            StringBuilder sb = new StringBuilder("Insights:");
            for (Insight insight : supports.get(q)) {
                sb.append(insight).append(", ");
            }
            q.setTestComment(sb.toString());
        }

        // get interest from supported insights
        for (Map.Entry<Insight, Set<AssessQuery>> entry : isSupportedBy.entrySet()){
            Set<AssessQuery> supporting = entry.getValue();
            double p = entry.getKey().getP();
            for (AssessQuery q : supporting){
                q.setInterest(1 - (q.getInterest() + p/supporting.size()));
            }
        }
        // conciseness ponderation
        for (AssessQuery q : tapQueries){
            q.setInterest(q.getInterest() * conciseness(q.getReference().getActiveDomain().size(), q.support()));
        }

        // --- SOLVING TAP ----
        System.out.println("Started solving TAP instance");
        stopwatch = Stopwatch.createStarted();

        // Naive heuristic
        TAPEngine naive = new KnapsackStyle();
        List<AssessQuery> naiveSolution = naive.solve(tapQueries, 50000, 100);
        NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
        naiveSolution.forEach(out::addQuery);
        Files.write(Paths.get("data/test_new.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));

        stopwatch.stop();
        System.out.println("Heuristic runtime: " + stopwatch.elapsed(TimeUnit.SECONDS));

        if (tapQueries.size() < 1000){
            TAPEngine exact = new CPLEXTAP(CPLEX_BIN, "data/tap_instance.dat");
            List<AssessQuery> exactSolution = exact.solve(tapQueries, 5000, 100);
            out = new NotebookJupyter(config.getBaseURL());
            exactSolution.forEach(out::addQuery);
            Files.write(Paths.get("data/outpout_exact.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            System.err.println("[WARNING] Couldn't run exact solver : too many queries");
        }


        conn.close();
    }

    public static double sum(Collection<Double> x){
        //if (x == null)
        //    return 0;
        double s = 0;
        for(Double item : x)
            s += item;
        return s;
    }

    public static void init() throws IOException, SQLException {
        config = DBConfig.newFromFile();
        conn = config.getConnection();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();
        ds = new Dataset(conn, table, theDimensions, theMeasures);
    }


    public static Set<Insight> getIntuitions() {
        Set<Insight> intuitions = new HashSet<>();
        for (DatasetDimension dim : ds.getTheDimensions()) {
            ImmutableSet<String> values = ImmutableSet.copyOf(dim.getActiveDomain());
            Set<List<String>> combiVals = Sets.combinations(values, 2).stream().map(ArrayList::new).collect(Collectors.toSet());

            for (List<String> pair : combiVals) {
                ds.getTheMeasures().stream().map(measure -> new Insight(dim, pair.get(0), pair.get(1), measure)).forEach(intuitions::add);
            }
        }
        return intuitions;
    }

    public static boolean querySupports(Insight insight, double[] a, double[] b){

        double mua = 0, mub = 0; // mean
        double muasq = 0, mubsq = 0; // mean of squares
        int count = 0; // count

        for (int i = 0 ; i < a.length; i++){
            double m1 = a[i];
            double m2 = b[i];
            count++;
            mua += m1;
            mub += m2;
            muasq += m1 * m1;
            mubsq += m2 * m2;
        }

        mua = mua / count;
        mub = mub / count;
        mubsq = mubsq/count;
        muasq = muasq/count;
        double vara = muasq - (mua*mua);
        double varb = mubsq - (mub*mub);

        return switch (insight.type) {
            case Insight.MEAN_SMALLER -> (mua < mub);
            case Insight.MEAN_GREATER -> (mua > mub);
            case Insight.MEAN_EQUALS -> (Math.abs(mua - mub) < 0.05 * Math.max(mua, mub));
            case Insight.VARIANCE_SMALLER -> (vara < varb);
            case Insight.VARIANCE_GREATER -> (vara > varb);
            case Insight.VARIANCE_EQUALS -> (Math.abs(vara - varb) < 0.05 * Math.max(vara, varb));
            default -> false;
        };

    }

    public static List<AssessQuery> getSupportingQueries(Insight insight){
        return ds.getTheDimensions().stream()
                .filter(dim -> ! (DBUtils.checkAimpliesB(insight.getDim(), dim, conn, table) || dim.equals(insight.dim)))
                .map(dim ->{
            AssessQuery q = new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dim, insight.getMeasure(), "sum");

            double mua = 0, mub = 0; // mean
            double muasq = 0, mubsq = 0; // mean of squares
            int count = 0; // count
            try (ResultSet rs = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(q.getSql())) {
                while (rs.next()) {
                    double m1 = rs.getDouble(2);
                    double m2 = rs.getDouble(3);
                    count++;
                    mua += m1;
                    mub += m2;
                    muasq += m1 * m1;
                    mubsq += m2 * m2;
                }
            } catch (SQLException e){
                System.err.println("ERROR reading result from " + q);
            }
            mua = mua / count;
            mub = mub / count;
            mubsq = mubsq/count;
            muasq = muasq/count;
            double vara = muasq - (mua*mua);
            double varb = mubsq - (mub*mub);

            switch (insight.type) {
                case Insight.MEAN_SMALLER:
                    if (mua < mub)
                        return q;

                case Insight.MEAN_GREATER:
                    if (mua > mub)
                        return q;

                case Insight.MEAN_EQUALS:
                    if (Math.abs(mua - mub) < 0.05 * Math.max(mua, mub))
                        return q;

                case Insight.VARIANCE_SMALLER:
                    if (vara < varb)
                        return q;

                case Insight.VARIANCE_GREATER:
                    if (vara > varb)
                        return q;

                case Insight.VARIANCE_EQUALS:
                    if (Math.abs(vara - varb) < 0.05 * Math.max(vara, varb))
                        return q;

                default:
                    return null;
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());

    }


    public static double conciseness(int nbGroups, int nbTuples){
        double alpha = 0.02, beta = 5, delta = 0.5;

        return Math.exp((-1 * (1/Math.pow(nbTuples, delta)) * Math.pow(nbGroups - (alpha*nbTuples) - beta,2)));
    }
}
