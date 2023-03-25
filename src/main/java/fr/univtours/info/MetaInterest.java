package fr.univtours.info;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.graph.G;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class MetaInterest {
    static Connection conn;
    static DBConfig config;
    static DatasetStats stats;
    static Dataset ds;
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;

    public static void main(String[] args) throws SQLException, IOException {
        DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/insurance.properties";

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


    }
}
