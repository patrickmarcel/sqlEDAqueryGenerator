package fr.univtours.info;

import com.alexscode.utilities.collection.Pair;
import com.alexscode.utilities.math.BenjaminiHochbergFDR;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.PartialAggregate;
import fr.univtours.info.dataset.TableFragment;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.optimize.*;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.ConnectionPool;
import fr.univtours.info.tap.Instance;
import org.apache.commons.cli.*;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ListSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.univtours.info.Insight.*;

public class MainTAP {

    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;
    //Default can be overridden by -i
    static String INTERESTINGNESS = "full";
    //Default can be overridden by -c
    public static String CPLEX_BIN = "";
    //Default cab be overridden by -s
    static double SAMPLERATIO = 100.0;
    //Default cab be overridden by -q
    static int QUERIESNB = 25, MAX_DISTANCE = 500;
    static double SIGLEVEL = 0.05;
    static boolean TRANSITIVE_KEEPS = false;
    static boolean DISABLE_AGG_MERGING = false;
    public static boolean USE_UNIFORM_SAMPLING = false;
    static long AGG_RAM = 8589934592L; // 1 GB
    public static boolean CUDA_PRESENT = false;

    public static void main( String[] args ) throws Exception{

        Options options = new Options();
        parseCmdLineArgs(args, options);

        System.out.println("[INFO] CPU Threads/Cores: " + Runtime.getRuntime().availableProcessors() + " | " + "Streams will use : " + ForkJoinPool.commonPool().getParallelism());

        //Attempting to detect cuda
        try {
            CudaRand dummy = new CudaRand();
            dummy.nextInt(10);
            CUDA_PRESENT = true;
        }catch (UnsatisfiedLinkError e){
            System.out.println("[INFO] Couldn't initialize cuda runtime falling back tu CPU");
        }

        //Load config and base dataset
        init();
        conn.setReadOnly(true);

        //generation
        System.out.println("[INFO] Starting hypothesis generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Insight> intuitions = new ArrayList<>(getIntuitions());

        stopwatch.stop();
        System.out.println("[TIME][ms] generation " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("[INFO] " + intuitions.size()*Insight.pprint.length + " hypothesis generated");

        //verification
        System.out.println("[INFO] Starting verification (1) ...");
        stopwatch = Stopwatch.createStarted();

        List<Insight> insights = StatisticalVerifier.check(intuitions, ds, SIGLEVEL, 1000, SAMPLERATIO/100.0, config, TRANSITIVE_KEEPS);

        stopwatch.stop();
        System.out.println("[TIME][s] verification (1) " + stopwatch.elapsed(TimeUnit.SECONDS));
        System.out.println("[INFO] Nb of insights (p<"+SIGLEVEL+") " + insights.size());

        //support
        System.out.println("[INFO] Started looking for supporting queries ...");
        stopwatch = Stopwatch.createStarted();

        ConcurrentMap<Insight, Set<AssessQuery>> isSupportedBy;
        if (DISABLE_AGG_MERGING)
            isSupportedBy = checkSupportNaive(insights);
        else
            isSupportedBy = checkSupportMergeOPT(insights);

        stopwatch.stop();
        System.out.println("[TIME][s] support " + stopwatch.elapsed(TimeUnit.SECONDS));
        System.out.println("[INFO] Supported insights " + isSupportedBy.keySet().size() + "/" + insights.size());
        insights.clear();

        Map<AssessQuery, List<Insight>> supports = new HashMap<>();
        isSupportedBy.forEach(((insight, assessQueries) -> {
            assessQueries.forEach(q -> {
                supports.computeIfAbsent(q, k -> new ArrayList<>());
                supports.get(q).add(insight);
            });
        }));
        supports.forEach(AssessQuery::setInsights);

        List<AssessQuery> tapQueries =  supports.keySet().parallelStream()
                .collect(Collectors.groupingByConcurrent(q -> q.getAssessed().getPrettyName() + "_" + q.getVal1() + "_" + q.getVal2() + "_" + q.getMeasure().getPrettyName()))
                .values().parallelStream().map(l -> l.stream().max(Comparator.comparing(AssessQuery::getInterest)).get()).collect(Collectors.toList());
        System.out.println("[INFO] Total queries (Instance size) " + tapQueries.size());


        System.out.println("[INFO] Computing interestngness ...");
        stopwatch = Stopwatch.createStarted();
        // credibility of insights
       isSupportedBy.entrySet().stream().parallel().forEach(e -> {
            Insight key = e.getKey();
            double trueS = e.getValue().size();
            double possibleS = 0;
            for (DatasetDimension d : ds.getTheDimensions()){
                if (!d.equals(key.getDim()) && !DBUtils.checkAimpliesB(key.getDim(), d, conn, table))
                    possibleS += 1;
            }
           key.setCredibility(trueS/possibleS);
        });

        // significance
        if (INTERESTINGNESS.equals("full") || INTERESTINGNESS.equals("cred") || INTERESTINGNESS.equals("sig") || INTERESTINGNESS.equals("sig_cred")) {
            supports.entrySet().stream().parallel().forEach((e) -> {
                double i;
                final List<Insight> supportedInsights = e.getValue();
                if (INTERESTINGNESS.equals("full") || INTERESTINGNESS.equals("sig_cred"))
                    i = supportedInsights.stream().mapToDouble(insight -> (1 - insight.getP()) * (1 - insight.getCredibility())).sum();
                else if (INTERESTINGNESS.equals("sig"))
                    i = supportedInsights.stream().mapToDouble(insight -> 1 - insight.getP()).sum();
                else
                    i = supportedInsights.stream().mapToDouble(insight -> (1 - insight.getCredibility())).sum();
                e.getKey().setInterest(i);
            });
        }

        // conciseness ponderation
        tapQueries.stream().parallel().forEach(q ->{

            if (INTERESTINGNESS.equals("full"))
                q.setInterest(q.getInterest() * conciseness(q.getReference().getActiveDomain().size(), q.support(stats)));
            else if (INTERESTINGNESS.equals("con"))
                q.setInterest(conciseness(q.getReference().getActiveDomain().size(), q.support(stats)));

        });
        stopwatch.stop();
        System.out.println("[TIME][s] interestingness " + stopwatch.elapsed(TimeUnit.SECONDS));

        // Fetching runtime
        //ConnectionPool cp = new ConnectionPool(config);
        System.out.println("[INFO] Estimating query runtime ...");
        tapQueries.stream().parallel().forEach(q -> {
            //Connection c = cp.getConnection();
            q.setExplainCost(1);
            q.setActualCost(1);
            //cp.returnConnection(c);
        });
        //cp.close();

        // dump data for learning
        try (BufferedWriter out = new BufferedWriter(new FileWriter("tap_dump.csv"))){
            out.write("qid,measure,function,ref,val1,val2,val1_f,val2_f,gb_ad_size,agg_size,interest\n");
            int n = 0;
            for (AssessQuery query : tapQueries){
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
        } catch (IOException e){

        }

        // --- SOLVING TAP ----
        System.out.println("[INFO] Started solving TAP instance ...");
        stopwatch = Stopwatch.createStarted();

        String printSample = SAMPLERATIO == 100.0 ? "" : "_sampled-" + (int) SAMPLERATIO + "_";

        // Naive heuristic
        TAPEngine naive = new KnapsackStyle();
        List<AssessQuery> naiveSolution = naive.solve(tapQueries, QUERIESNB, MAX_DISTANCE);
        naiveSolution.forEach(q -> q.setTestComment(supports.get(q).stream().map(Insight::toString).collect(Collectors.joining(", "))));
        NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
        naiveSolution.forEach(out::addQuery);
        System.out.println("[INFO] KS solution is " + naiveSolution.size() + " queries long");
        Files.writeString(Paths.get("data/KS_" + INTERESTINGNESS + "_" + QUERIESNB + printSample +LocalTime.now().toString().replace(':', '-')+".ipynb"), out.toJson());

        stopwatch.stop();
        System.out.println("[TIME][ms] Heuristic " + stopwatch.elapsed(TimeUnit.MILLISECONDS));


        if (tapQueries.size() < 1000 && ! CPLEX_BIN.equals("")){
            TAPEngine exact = new CPLEXTAP(CPLEX_BIN, "data/tap_instance.dat");
            List<AssessQuery> exactSolution = exact.solve(tapQueries, QUERIESNB, MAX_DISTANCE);
            exactSolution.forEach(q -> q.setTestComment(supports.get(q).stream().map(Insight::toString).collect(Collectors.joining(", "))));
            out = new NotebookJupyter(config.getBaseURL());
            exactSolution.forEach(out::addQuery);
            //Files.write(Paths.get("data/outpout_exact.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));
            System.out.println("[INFO] EXACT solution is " + exactSolution.size() + " queries long");
            Files.writeString(Paths.get("data/EXACT_" + INTERESTINGNESS + "_" + QUERIESNB + printSample +LocalTime.now().toString().replace(':', '-')+".ipynb"), out.toJson());
        } else {
            if (tapQueries.size() > 1000) System.err.println("[WARNING] Couldn't run exact solver : too many queries");
            else System.err.println("[WARNING] No CPLEX binary defined with parameter -c");
        }
        
        conn.close();
    }

    private static ConcurrentMap<Insight, Set<AssessQuery>> checkSupportMergeOPT(List<Insight> insights) {
        ConcurrentMap<Insight, Set<AssessQuery>> isSupportedBy = new ConcurrentHashMap<>();
        /*
                Build the partial aggregates for quick query processing
         */
        System.out.print("[INFO] Building partial aggregate cache...");
        //We need coverage for every pair of dimensions
        //The candidates therefore need to be set of pairs
        //I use a Set<Set> instead of the Set<Pair> as Pair order matters for .equals
        List<Set<Set<DatasetDimension>>> candidates = new ArrayList<>();
        //Generate all or par of the power set of dimensions
        for (int i = 2; i <= 4; i++) {
            if (i == 2)
                candidates.addAll(Sets.combinations(new HashSet<>(ds.getTheDimensions()), i).stream().map(s -> Set.of(s)).collect(Collectors.toSet()));
                //if not dealing with pairs already we need to map them to a set of pairs
            else
                candidates.addAll(Sets.combinations(new HashSet<>(ds.getTheDimensions()), i).stream().map(s -> Sets.combinations(s, 2)).collect(Collectors.toSet()));
        }
        //Get the aggregates estimated sizes of course we need to collapse them to sets of dimensions with .flatMap(Set::stream).collect(Collectors.toSet()))
        List<Double> weights = candidates.stream()
                .map(agg -> (double) stats.estimateAggregateSize(agg.stream().flatMap(Set::stream).collect(Collectors.toSet())))
                .collect(Collectors.toList());
        List<PartialAggregate> aggregates = WeightedSetCover.solve(candidates, weights)
                .stream().map(s -> new PartialAggregate(new ArrayList<>(s.stream().flatMap(Set::stream).collect(Collectors.toSet())), ds.getTheMeasures(), ds))
                .collect(Collectors.toList());
        //This is just an chained Map (from Guava) for simplicity sakes
        Table<DatasetDimension, DatasetDimension, PartialAggregate> cache = HashBasedTable.create();
        //For each pair of dimensions
        Sets.combinations(new HashSet<>(ds.getTheDimensions()), 2).forEach(p -> {
            Iterator<DatasetDimension> it = p.iterator();
            DatasetDimension a = it.next();
            DatasetDimension b = it.next();
            //find an aggregate that contains both
            for (int i = 0, aggregatesSize = aggregates.size(); i < aggregatesSize; i++) {
                PartialAggregate ag = aggregates.get(i);
                if (ag.getGroupBySet().contains(a) && ag.getGroupBySet().contains(b)) {
                    //Cache should work no matter the order
                    cache.put(a, b, ag);
                    cache.put(b, a, ag);
                    break;
                } else {
                    //Sanity check
                    if (i == aggregatesSize - 1) {
                        System.err.println("[ERROR] No partial aggregate was found for " + a + " and " + b);
                    }
                }
            }
        });
        System.out.println("Done");
        /*
                Actually check support for the insights
         */
        insights.parallelStream().forEach(insight -> {
            ds.getTheDimensions().stream().filter(d -> !d.equals(insight.dim)).forEach( otherDim -> {
                if (!DBUtils.checkAimpliesB(insight.getDim(), otherDim, conn, table) && querySupports(insight, cache.get(insight.getDim(), otherDim).assessSum(insight.getMeasure(), otherDim, insight.getDim(), insight.getSelA(), insight.getSelB()))) {
                    isSupportedBy.computeIfAbsent(insight, k -> ConcurrentHashMap.newKeySet());
                    isSupportedBy.get(insight).add(new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), otherDim, insight.getMeasure(), "sum"));
                }
            });
        });
        return isSupportedBy;
    }

    private static ConcurrentMap<Insight, Set<AssessQuery>> checkSupportNaive(List<Insight> insights) {
        AtomicInteger count = new AtomicInteger();
        ConcurrentMap<Insight, Set<AssessQuery>> isSupportedBy = new ConcurrentHashMap<>();
        // grouping insight by selection dimension
        insights.stream().collect(Collectors.groupingBy(Insight::getDim)).forEach((dimB, insightsOfDimB) ->{
            // For every other dimension
            for (DatasetDimension dimA : ds.getTheDimensions().stream().filter(d -> !d.equals(dimB)).collect(Collectors.toList())){
                PartialAggregate pa = new PartialAggregate(List.of(dimA, dimB), ds.getTheMeasures(), ds);
                count.incrementAndGet();
                //Go multithreaded and cache everything
               insightsOfDimB.parallelStream()
                        .filter(insight -> querySupports(insight, pa.assessSum(insight.getMeasure(), dimA, insight.getDim(), insight.getSelA(), insight.getSelB())))
                        //.map(insight -> new Pair<>(insight, new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dimA, insight.getMeasure(), "sum")))
                        .forEach( insight -> {
                            if (! DBUtils.checkAimpliesB(insight.getDim(), dimA, conn, table)) {
                                isSupportedBy.computeIfAbsent(insight, k -> ConcurrentHashMap.newKeySet());
                                isSupportedBy.get(insight).add(new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dimA, insight.getMeasure(), "sum"));
                            }
                        });
            }
        });
        System.out.println("[INFO] ran " + count.get() + " queries");
        return isSupportedBy;
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

    public static double conciseness(int nbGroups, int nbTuples){
        double alpha = 0.05, beta = 0, delta = 1;

        return Math.exp((-1 * (1/Math.pow(nbTuples, delta)) * Math.pow(nbGroups - (alpha*nbTuples) - beta,2)));
    }

    public static Set<Insight> getIntuitions() {
        Set<Insight> intuitions = new HashSet<>();
        for (DatasetDimension dim : ds.getTheDimensions()) {
            ImmutableSet<String> values = ImmutableSet.copyOf(dim.getActiveDomain());
            Set<List<String>> combiVals = Sets.combinations(values, 2).stream().map(ArrayList::new).collect(Collectors.toSet());

            for (List<String> pair : combiVals) {
                //intuitions.add(new Insight(dim, pair.get(0), pair.get(1), null));
                ds.getTheMeasures().stream().map(measure -> new Insight(dim, pair.get(0), pair.get(1), measure)).forEach(intuitions::add);
            }
        }
        return intuitions;
    }

    public static boolean querySupports(Insight insight, double[][] data){
        double[] a = data[0]; double[] b = data[1];
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
            case MEAN_SMALLER -> (mua < mub);
            case Insight.MEAN_GREATER -> (mua > mub);
            case Insight.MEAN_EQUALS -> (Math.abs(mua - mub) < 0.05 * Math.max(mua, mub));
            case VARIANCE_SMALLER -> (vara < varb);
            case VARIANCE_GREATER -> (vara > varb);
            case Insight.VARIANCE_EQUALS -> (Math.abs(vara - varb) < 0.05 * Math.max(vara, varb));
            default -> false;
        };

    }

    private static void parseCmdLineArgs(String[] args, Options options) {
        Option input = new Option("d", "database", true, "database config file path");
        input.setRequired(true);
        options.addOption(input);

        Option cplex = new Option("c", "cplex-binary", true, "path of binary for processing standard TAP instance");
        cplex.setRequired(false);
        options.addOption(cplex);

        Option interest = new Option("i", "interestingness", true, "Interestingness measure tu use : full/con/sig/cred/sig_cred");
        interest.setRequired(false);
        options.addOption(interest);

        Option samp = new Option("s", "sample", true, "Sample ratio between 0 and 100% (default 100% no sampling)");
        samp.setRequired(false);
        options.addOption(samp);

        Option qSize = new Option("q", "notebook-size", true, "Number of queries to put in the notebook (defaults to 25)");
        qSize.setRequired(false);
        options.addOption(qSize);

        Option ram = new Option("r", "agg-ram", true, "RAM Limit (in MB) for aggregates");
        ram.setRequired(false);
        options.addOption(ram);

        Option md = new Option("m", "epsilon-distance", true, "Epsilon constraint for sequence distance defaults to 500");
        md.setRequired(false);
        options.addOption(md);

        Option trans = new Option("t", "keep-transitive", false, "if enabled does not delete transitive insights such as A > C when A > B and B > C");
        trans.setRequired(false);
        options.addOption(trans);

        Option agg = new Option("a", "disable-agg", false, "if present disables aggregate merging optimization");
        agg.setRequired(false);
        options.addOption(agg);

        Option uni = new Option("u", "sample-uniform", false, "if present along with a sampling rate (-s xx) uses an uniform sampling algorithm instead of the rebalanced one.");
        uni.setRequired(false);
        options.addOption(uni);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args, false);
            DBConfig.CONF_FILE_PATH = cmd.getOptionValue("database");
            System.out.println("Config File :" + DBConfig.CONF_FILE_PATH);
            if (cmd.hasOption('c')){
                CPLEX_BIN = cmd.getOptionValue('c');
            }
            TRANSITIVE_KEEPS = cmd.hasOption("t");
            DISABLE_AGG_MERGING = cmd.hasOption("a");
            USE_UNIFORM_SAMPLING = cmd.hasOption("u");
            if (cmd.hasOption('i')) {
                INTERESTINGNESS = cmd.getOptionValue('i');
                if (!INTERESTINGNESS.equals("sig_cred") && !INTERESTINGNESS.equals("full") && !INTERESTINGNESS.equals("con")  && !INTERESTINGNESS.equals("sig") && !INTERESTINGNESS.equals("cred")){
                    System.err.println("[ERROR] Unknown interestingness measure: '" + INTERESTINGNESS + "'");
                    System.exit(1);
                }
            }
            if (cmd.hasOption("s")){
                try {
                    double s = Double.parseDouble(cmd.getOptionValue("s"));
                    if (s > 0 && s <= 100)
                        SAMPLERATIO = s;
                    else {
                        System.err.println("[ERROR] Sample ratio between 0 and 100% (default 100% no sampling) !");
                    }
                } catch (NumberFormatException e){
                    System.err.println("[ERROR] couldn't parse argument sample ratio '" + cmd.getOptionValue("s") + "' as a double");
                }
            }
            if (cmd.hasOption("m")){
                try {
                    int q = Integer.parseInt(cmd.getOptionValue("m"));
                    if (q > 0)
                        MAX_DISTANCE = q;
                    else {
                        System.err.println("[ERROR] epsilon distance must be positive");
                    }
                } catch (NumberFormatException e){
                    System.err.println("[ERROR] couldn't parse argument epsilon distance '" + cmd.getOptionValue("m") + "' as an integer");
                }
            }
            if (cmd.hasOption("q")){
                try {
                    int q = Integer.parseInt(cmd.getOptionValue("q"));
                    if (q > 0)
                        QUERIESNB = q;
                    else {
                        System.err.println("[ERROR] Query number must be positive");
                    }
                } catch (NumberFormatException e){
                    System.err.println("[ERROR] couldn't parse argument notebook size '" + cmd.getOptionValue("q") + "' as an integer");
                }
            }
            if (cmd.hasOption("r")){
                try {
                    int q = Integer.parseInt(cmd.getOptionValue("r"));
                    if (q > 0)
                        AGG_RAM = q * 1024L * 1024L * 8L; // convert to bits
                    else {
                        System.err.println("[ERROR] RAM space must be positive");
                    }
                } catch (NumberFormatException e){
                    System.err.println("[ERROR]  parsing RAM size");
                }
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
    }

    @Deprecated
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
                case MEAN_SMALLER:
                    if (mua < mub)
                        return q;

                case Insight.MEAN_GREATER:
                    if (mua > mub)
                        return q;

                case Insight.MEAN_EQUALS:
                    if (Math.abs(mua - mub) < 0.05 * Math.max(mua, mub))
                        return q;

                case VARIANCE_SMALLER:
                    if (vara < varb)
                        return q;

                case VARIANCE_GREATER:
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
}
