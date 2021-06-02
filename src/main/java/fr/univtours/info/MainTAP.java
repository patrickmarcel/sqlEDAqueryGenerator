package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.queries.AbstractEDAsqlQuery;
import fr.univtours.info.queries.SiblingAssessQuery;
import org.apache.commons.math3.stat.StatUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
        //DEBUG
        devOut = new PrintStream(new FileOutputStream("data/logs/log_100.txt"));
        //Load config and base dataset
        init();
        conn.setReadOnly(true);

        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        Set<Insight> intuitions = getIntuitions();

        stopwatch.stop();
        System.out.println("Generation time in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println(intuitions.size() + " intuitions generated");

        //verification
        System.out.println("Starting verification");
        stopwatch = Stopwatch.createStarted();

        List<Insight> insights = new ArrayList<>();
        for (Insight intuition : intuitions){
            double p = StatisticalVerifier.check(intuition, ds);
            if (p < 0.05){
                insights.add(intuition);
            }
        }

        stopwatch.stop();
        System.out.println("Verification time in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        System.out.println("Nb of insights: " + insights.size());

        System.out.println(insights);

        List<AbstractEDAsqlQuery> support = new ArrayList<>(insights.size());
        try (Statement statement = conn.createStatement()) {
            boolean[] flag = new boolean[insights.size()];
            int l = 0;
            for (Insight i : insights){
                AbstractEDAsqlQuery sq = getSupportingQuery(i, statement);
                support.add(sq);
                flag[l++] = sq == null;
            }
            for (int i = 0; i < flag.length; i++) {

            }

        }catch (SQLException e){
            System.err.println("Couldn't get supporting queries");
        }

        System.out.println(insights.size());

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

    public static AbstractEDAsqlQuery getSupportingQuery(Insight insight, Statement st) throws SQLException{
        List<DatasetDimension> dims = new ArrayList<>(ds.getTheDimensions());
        dims.remove(insight.dim);
        dims.sort(Comparator.comparing(dim -> dim.getActiveDomain().size()));

        for (DatasetDimension dim : dims){
            SiblingAssessQuery q = new SiblingAssessQuery(conn, ds.getTable(), insight.getDim(), insight.getSelA(), insight.getSelB(), dim, insight.getMeasure(), "sum");
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

            if (mua < mub){
                return q;
            }

        }
        System.err.println("Couldn't find supporting query for " + insight);
        return null;

    }
}
