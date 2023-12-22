package fr.univtours.info;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.AssessQuery;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ComparisonCostEstimation {
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;
    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;

    static Random dice = new Random(System.nanoTime());

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option input = new Option("d", "database", true, "database config file path");
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args, false);
            if (cmd.hasOption('d')){
                DBConfig.CONF_FILE_PATH = cmd.getOptionValue('d');
            } else {
                DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/flights.properties";
            }


        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }


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

        DatasetMeasure M = theMeasures.get(0);

        List<Double> baselineCosts = new ArrayList<>();
        List<Double> idxACosts = new ArrayList<>();
        List<Double> idxBCosts = new ArrayList<>();
        List<Double> idxABCosts = new ArrayList<>();
        List<Double> materializedCosts = new ArrayList<>();

        for (DatasetDimension A : ds.getTheDimensions()) {
            for (DatasetDimension B : ds.getTheDimensions()) {
                if (A.equals(B)) continue;

                List<AssessQuery> sampleQueries = new ArrayList<>(10);
                for (int i = 0; i < 10; i++) {
                    Pair<String, String> vals = getRandomValPair(B);
                    sampleQueries.add(new AssessQuery(conn, table, B, vals.left, vals.right, A, M, "sum"));
                }

                // Compute baseline
                double[] base = new double[10];
                for (int i = 0; i < sampleQueries.size(); i++) {
                    AssessQuery q = sampleQueries.get(i);
                    q.explainAnalyze();
                    baselineCosts.add(q.actualTime() / 1000.0);
                    base[i] = (q.actualTime() / 1000.0);
                }

                // Index on A
                ds.setIndex(A);
                for (int i = 0; i < sampleQueries.size(); i++) {
                    AssessQuery q = sampleQueries.get(i);
                    q.explainAnalyze();
                    idxACosts.add((q.actualTime() / 1000.0)/base[i]);
                }
                ds.removeIndex(A);

                // Index on B
                ds.setIndex(B);
                for (int i = 0; i < sampleQueries.size(); i++) {
                    AssessQuery q = sampleQueries.get(i);
                    q.explainAnalyze();
                    idxBCosts.add((q.actualTime() / 1000.0)/base[i]);
                }
                ds.removeIndex(B);

                // Index on A and B
                ds.setIndex(A, B);
                for (int i = 0; i < sampleQueries.size(); i++) {
                    AssessQuery q = sampleQueries.get(i);
                    q.explainAnalyze();
                    idxABCosts.add((q.actualTime() / 1000.0)/base[i]);
                }
                ds.removeIndex(A, B);

            }
        }
        System.out.println(baselineCosts);
        System.out.println(idxACosts);
        System.out.println(idxBCosts);
        System.out.println(idxABCosts);

    }




    private static Pair<String, String> getRandomValPair(DatasetDimension b) {
        String left = "", right;
        var ad = b.getActiveDomain();
        int valOneID = 1 + dice.nextInt(ad.size()-1),
                valTwoID  = 1 + dice.nextInt(ad.size()-1);
        while (valTwoID == valOneID){
            valTwoID  = 1 + dice.nextInt(ad.size()-1);
        }

        //val 1 is always smaller
        if (valTwoID < valOneID){
            int tmp = valTwoID;
            valTwoID = valOneID;
            valOneID = tmp;
        }

        var adIt = ad.iterator();
        for (int i = 0; i < valTwoID; i++) {
            if (i == valOneID)
                left = adIt.next();
            else
                adIt.next();
        }
        right = adIt.next();

        return new Pair<>(left, right);
    }
}
