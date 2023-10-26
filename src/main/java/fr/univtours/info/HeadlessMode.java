package fr.univtours.info;

import com.alexscode.utilities.collection.Pair;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.GenericSQLQuery;
import fr.univtours.info.queries.Query;
import org.apache.commons.cli.*;
import org.takes.Request;
import org.takes.Response;
import org.takes.Take;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.Exit;
import org.takes.http.FtBasic;
import org.takes.rq.RqPrint;
import org.takes.rs.RsHtml;
import org.takes.rs.RsJson;
import org.takes.rs.RsText;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class HeadlessMode {
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;
    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;

    public static void main(String[] args) throws SQLException, IOException {
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
                DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/enedis.properties";
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

        Gson gson = new Gson();
        HashMap<GenericSQLQuery, Long> timeCache = new HashMap<>();

        final class TkTime implements Take {
            @Override
            public Response act(final Request req) throws IOException {
                final String rawBody = new RqPrint(req).printBody();
                //System.out.println(rawBody);
                ArrayList<Long> times = new ArrayList<>();

                JsonArray body = gson.fromJson(rawBody, JsonArray.class);
                for (JsonElement jquery : body){
                    GenericSQLQuery sqlQuery = queryFromJson(jquery.getAsJsonObject());
                    timeCache.computeIfAbsent(sqlQuery, Query::estimatedTime);
                    times.add(timeCache.get(sqlQuery));
                    System.out.println(sqlQuery);
                }
                //System.out.println(gson.toJson(times));
                return new RsText(gson.toJson(times));
            }
        }

        final class TkInterest implements Take {
            @Override
            public Response act(final Request req) throws IOException {
                final String rawBody = new RqPrint(req).printBody();
                JsonObject body = gson.fromJson(rawBody, JsonObject.class);
                System.out.println("[DEBUG] raw query body: " + rawBody);

                Insight rawInsight = insightFromJSON(body);
                List<Insight> valid = StatisticalVerifier.check(List.of(rawInsight), ds, 0.05, 10000, 1, config, true);

                boolean checks = !valid.isEmpty();

                return new RsText("{\"response\":" + checks + "}");
            }
        }

        final class TkInterestArr implements Take {
            @Override
            public Response act(final Request req) throws IOException {
                final String rawBody = new RqPrint(req).printBody();
                JsonArray body = gson.fromJson(rawBody, JsonArray.class);

                List<Boolean> r = new ArrayList<>();
                for (JsonElement q : body) {
                    Insight rawInsight = insightFromJSON(q.getAsJsonObject());
                    List<Insight> valid = StatisticalVerifier.check(List.of(rawInsight), ds, 0.05, 10000, 1, config, true);

                    r.add(!valid.isEmpty());
                }

                return new RsText(gson.toJson(r));
            }
        }


        new FtBasic(
                new TkFork(new FkRegex("/insight", new TkInterest())
                           , new FkRegex("/insights", new TkInterestArr())), 4242
        ).start(Exit.NEVER);

    }


    static private GenericSQLQuery queryFromJson(JsonObject json){
        String gbAtt = json.get("gb").getAsString();
        String fun = json.get("agg").getAsString();
        String lMeasure = json.get("leftMeasure").getAsString();
        String rMeasure = json.get("rightMeasure").getAsString();
        List<Pair<String, String>> lPred =
                json.get("leftPredicate").getAsJsonObject().entrySet().stream()
                        .map(e -> new Pair<>(e.getKey(), e.getValue().getAsString()))
                        .collect(Collectors.toList());
        List<Pair<String, String>> rPred =
                json.get("rightPredicate").getAsJsonObject().entrySet().stream()
                        .map(e -> new Pair<>(e.getKey(), e.getValue().getAsString()))
                        .collect(Collectors.toList());


        StringBuilder q = new StringBuilder("SELECT t1.\"");
        q.append(gbAtt).append("\"");
        q.append(", measure1, measure2 FROM ");

        q.append("( SELECT ").append(gbAtt).append(", ").append(fun).append("(\"").append(lMeasure).append("\") measure1");
        q.append(" FROM ").append(ds.getTable()).append(" WHERE ");
        q.append(lPred.stream().map(p -> "\"" + p.left + "\" = '" + p.right + "'").collect(Collectors.joining(" AND ")));
        q.append(" GROUP BY ").append(gbAtt).append(", ").append(lPred.stream().map(Pair::getA).collect(Collectors.joining(", ")));
        q.append(" ) t1,");

        q.append("( SELECT ").append(gbAtt).append(", ").append(fun).append("(\"").append(rMeasure).append("\") measure2");
        q.append(" FROM ").append(ds.getTable()).append(" WHERE ");
        q.append(rPred.stream().map(p -> "\"" + p.left + "\" = '" + p.right + "'").collect(Collectors.joining(" AND ")));
        q.append(" GROUP BY ").append(gbAtt).append(", ").append(rPred.stream().map(Pair::getA).collect(Collectors.joining(", ")));
        q.append(" ) t2");

        q.append(" WHERE t1.\"").append(gbAtt).append("\" = t2.\"").append(gbAtt).append("\" order by ").append(gbAtt);
        q.append(";");

        return new GenericSQLQuery(ds, q.toString());
    }

    static private Insight insightFromJSON(JsonObject json){

        DatasetMeasure lMeasure = null;
        for (var dim : theMeasures){
            if (json.get("leftMeasure").getAsString().equals(dim.getPrettyName()))
                lMeasure = dim;
        }

        List<Pair<String, String>> lPred =
                json.get("leftPredicate").getAsJsonObject().entrySet().stream()
                        .map(e -> new Pair<>(e.getKey(), e.getValue().getAsString()))
                        .collect(Collectors.toList());
        List<Pair<String, String>> rPred =
                json.get("rightPredicate").getAsJsonObject().entrySet().stream()
                        .map(e -> new Pair<>(e.getKey(), e.getValue().getAsString()))
                        .collect(Collectors.toList());


        DatasetDimension selAtt = null;
        for (DatasetDimension dim : theDimensions){
            if (lPred.get(0).left.equals(dim.getPrettyName()))
                selAtt = dim;
        }


        return new Insight(selAtt, lPred.get(0).right, rPred.get(0).right, lMeasure, Insight.RAW);
    }



}
