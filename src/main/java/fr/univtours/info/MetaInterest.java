package fr.univtours.info;

import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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
        DBConfig.CONF_FILE_PATH = "/home/alex/IdeaProjects/sqlEDAqueryGenerator/src/main/resources/insects.properties";

        config = DBConfig.newFromFile();
        conn = config.getConnection();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();

        ds = new Dataset(conn, table, theDimensions, theMeasures);
        System.out.println("Connection to database successful");

        System.out.print("Collecting statistics ... ");
        //stats = new DatasetStats(ds);
        System.out.println(" Done");


    }
}
