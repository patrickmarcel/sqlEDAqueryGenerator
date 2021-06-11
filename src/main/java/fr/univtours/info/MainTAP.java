package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.optimize.CPLEXTAP;
import fr.univtours.info.optimize.KnapsackStyle;
import fr.univtours.info.optimize.TAPEngine;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.AssessQuery;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainTAP {

    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
    static Connection conn;
    static DBConfig config;

    public static PrintStream devOut;

    static final String[] aggF = {"avg", "sum", "count"};//"min", "max",

    public static void main( String[] args ) throws Exception{
        //Load config and base dataset
        init();
        conn.setReadOnly(true);

        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Insight> intuitions = new ArrayList<>(getIntuitions());

        stopwatch.stop();
        System.out.println("Generation time in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println(intuitions.size() + " intuitions generated");

        //verification
        System.out.println("Starting verification");
        stopwatch = Stopwatch.createStarted();

        List<Insight> insights = StatisticalVerifier.check(intuitions, ds, 0.05, 1000);

        stopwatch.stop();
        System.out.println("Verification time in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("Nb of insights: " + insights.size());


        //support
        System.out.println("Started looking for supporting queries");
        stopwatch = Stopwatch.createStarted();
        Map<AssessQuery, List<Insight>> supports = new HashMap<>();
        Map<Insight, List<AssessQuery>> isSupportedBy = new HashMap<>();
        try (Statement statement = conn.createStatement()) {
            List<Insight> orphan = new ArrayList<>();
            for (Insight i : insights){
                List<AssessQuery> sqs = getSupportingQueries(i, statement);
                if (sqs.size() == 0)
                    orphan.add(i);
                else{
                    isSupportedBy.computeIfAbsent(i, k-> new ArrayList<>());
                    isSupportedBy.get(i).addAll(sqs);
                    for (AssessQuery q : sqs){
                        supports.computeIfAbsent(q, k -> new ArrayList<>());
                        supports.get(q).add(i);
                    }
                }
            }
            insights.removeAll(orphan);

        }catch (SQLException e){
            System.err.println("Couldn't get supporting queries");
        }

        stopwatch.stop();
        System.out.println("Support time in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("Supported insights " + insights.size());

        List<AssessQuery> tapQueries = new ArrayList<>();
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
        for (Map.Entry<Insight, List<AssessQuery>> entry : isSupportedBy.entrySet()){
            List<AssessQuery> supporting = entry.getValue();
            double p = entry.getKey().getP();
            for (AssessQuery q : supporting){
                q.setInterest(1 - (q.getInterest() + p/supporting.size()));
            }
        }
        // conciseness ponderation
        for (AssessQuery q : tapQueries){
            q.setInterest(q.getInterest() * conciseness(q.getReference().getActiveDomain().size(), q.support()));
        }

        // Naive heuristic
        TAPEngine naive = new KnapsackStyle();
        List<AssessQuery> naiveSolution = naive.solve(tapQueries, 2500, 150);
        NotebookJupyter out = new NotebookJupyter(config.getBaseURL());
        naiveSolution.forEach(out::addQuery);
        Files.write(Paths.get("data/test_new.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));


        if (tapQueries.size() < 1000){
            TAPEngine exact = new CPLEXTAP("C:\\Users\\achan\\source\\repos\\cplex_test\\x64\\Release\\cplex_test.exe", "data/tap_instance.dat");
            List<AssessQuery> exactSolution = exact.solve(tapQueries, 2500, 150);
            out = new NotebookJupyter(config.getBaseURL());
            exactSolution.forEach(out::addQuery);
            Files.write(Paths.get("data/outpout_exact.ipynb"), out.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            System.err.println("[WARNING] Couldn't run exact solver : too many queries");
        }


        conn.close();
    }

    public static void init() throws IOException, SQLException {
        config = DBConfig.readProperties();
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
                for (DatasetMeasure measure : ds.getTheMeasures()){
                    //TODO generate all types of insights
                    intuitions.add(new Insight(dim, pair.get(0), pair.get(1), measure, Insight.MEAN_SMALLER));
                }
            }
        }
        return intuitions;
    }

    public static List<AssessQuery> getSupportingQueries(Insight insight, Statement st) throws SQLException{
        List<AssessQuery> supporting = new ArrayList<>();
        List<DatasetDimension> dims = new ArrayList<>(ds.getTheDimensions());
        dims.remove(insight.dim);
        dims.sort(Comparator.comparing(dim -> dim.getActiveDomain().size()));

        for (DatasetDimension dim : dims){
            // SKip irrelevant combinations
            if (DBUtils.checkAimpliesB(insight.getDim(), dim, conn, table))
                continue;
            AssessQuery q = new AssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dim, insight.getMeasure(), "sum");
            ResultSet rs = q.execute();

            ArrayList<Double> a = new ArrayList<>();
            ArrayList<Double> b = new ArrayList<>();
            rs.beforeFirst();
            while (rs.next()) {
                double m1 = rs.getDouble(2);
                a.add(m1);
                double m2 = rs.getDouble(3);
                b.add(m2);
            }
            double mua = a.stream().mapToDouble(n -> n).sum();
            double mub = b.stream().mapToDouble(n -> n).sum();
            mua = mua / a.size();
            mub = mub / b.size();

            switch (insight.type) {
                case Insight.MEAN_SMALLER:
                    if (mua < mub)
                        supporting.add(q);
                    break;

                case Insight.MEAN_GREATER:
                    if (mua > mub)
                        supporting.add(q);
                    break;

                case Insight.MEAN_EQUALS:
                    if (Math.abs(mua - mub) < 0.05 * Math.max(mua, mub))
                        supporting.add(q);
                    break;

                default:
                    break;
            }

        }
        return supporting;

    }

    public static final double sqrt2pi = Math.sqrt(2*Math.PI);
    public static double conciseness(int nbGroups, int nbTuples){
        double alpha = 0.4, beta = 1;
        double sigma = Math.sqrt(nbTuples/2.);

        return (1/(sigma*sqrt2pi))*Math.exp((-1 * Math.pow(nbGroups - (nbTuples*alpha) - beta ,2))/(sigma*sigma));
    }
}
