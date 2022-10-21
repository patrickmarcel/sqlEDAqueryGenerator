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
        DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/vaccines.properties";

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


        new FtBasic(
                new TkFork(new FkRegex("/time", new TkTime())), 8000
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



}
