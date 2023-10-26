package fr.univtours.info;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.AssessQuery;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.math3.distribution.TDistribution;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.graph.G;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MetaInterest {
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;
    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;

    public static void main(String[] args) throws Exception {
        DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/enedis.properties";

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

        Gson gson = new Gson();
        Type mapType = new TypeToken<HashMap<String, Integer>>() {}.getType();
        JsonObject out = new JsonObject();
        for (DatasetDimension dim : stats.getAdSize().keySet()){
            out.add(dim.getPrettyName(), gson.toJsonTree(stats.getFrequency().get(dim), mapType));
        }
        System.out.println(out.toString());
        System.exit(0);

        System.out.println(ds.getTheDimensions().size() + " | " + ds.measuresNb());
        List<Insight> intuitions = new ArrayList<>();
        for (DatasetDimension dim : ds.getTheDimensions()) {
            ImmutableSet<String> values = ImmutableSet.copyOf(dim.getActiveDomain());
            Set<List<String>> combiVals = Sets.combinations(values, 2).stream().map(ArrayList::new).collect(Collectors.toSet());

            for (List<String> pair : combiVals) {
                //intuitions.add(new Insight(dim, pair.get(0), pair.get(1), null));
                ds.getTheMeasures().stream().map(measure -> new Insight(dim, pair.get(0), pair.get(1), measure)).forEach(intuitions::add);
            }
        }

        System.out.println("Comparison queries # " + intuitions.size());

        Collections.shuffle(intuitions);
        intuitions = intuitions.stream().filter(randomPredicate(intuitions.size() * (theDimensions.size()-1), 10000)).collect(Collectors.toList());

        List<Long> timeVals = new ArrayList<>(intuitions.size());

        try (ProgressBar pb = new ProgressBar("\"Running queries ...\"", intuitions.size() * (theDimensions.size()-1))) {


            for (Insight i : intuitions) {
                for (DatasetDimension d2 : theDimensions) {
                    if (!d2.equals(i.getDim())) {
                        AssessQuery q = new AssessQuery(conn, table, i.getDim(), i.getSelA(), i.selB, d2, i.measure, "sum");
                        q.explainAnalyze();
                        timeVals.add(q.actualTime());
                        pb.step();

                    }

                }

            }

        }
        String fileName = "/home/alex/time.dump";
        PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
        for (Long t : timeVals)
            pw.println(t);
        pw.close();


    }

    static Predicate<Object> randomPredicate(int total, int toChoose) {
        Random random = new Random();
        return obj -> random.nextInt(total) < toChoose;
    }
}
