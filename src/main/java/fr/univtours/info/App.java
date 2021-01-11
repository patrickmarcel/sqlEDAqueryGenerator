package fr.univtours.info;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

/**
 *
 *
 */
public class App {
    static Dataset ds;
    static String table;
    static ArrayList<DatasetDimension> theDimensions;
    static ArrayList<DatasetMeasure> theMeasures;
    static Connection conn;

    public static void main( String[] args ) throws Exception{
        loadDataset();
        generateCounts();
    }


    static void loadDataset() throws Exception{
        DBservices db= new DBservices();
        conn=db.connectToPostgresql();
        readProperties();
        ds=new Dataset(theDimensions, theMeasures);
    }

    static void generateAggregate(){
        ImmutableSet<String> set = ImmutableSet.of("APPLE", "ORANGE", "MANGO");
        Set<Set<String>> powerSet = Sets.powerSet(set);
    }

    static void generateCounts() throws Exception {
        for(DatasetDimension dim : theDimensions){
            CountdistinctQuery cdq= new CountdistinctQuery(conn, table, dim.name) ;
            cdq.execute();
            cdq.explainAnalyze();
            System.out.println(cdq.sql);
            System.out.println(cdq.count);
            System.out.println(cdq.cost);
        }

    }

    static void readProperties() throws Exception{
        final FileReader fr = new FileReader(new File("src/main/resources/application.properties"));
        final Properties props = new Properties();
        props.load(fr);
        table = props.getProperty("datasource.table");
        String dimensions=props.getProperty("datasource.dimensions");
        String measures=props.getProperty("datasource.measures");
        theDimensions=new ArrayList<>();
        theMeasures=new ArrayList<>();
        String[] dim=dimensions.split(",");
        for (int x=0; x<dim.length; x++) {
            theDimensions.add(new DatasetDimension(dim[x]));
        }
        String[] meas=measures.split(",");
        for (int x=0; x<meas.length; x++) {
            theMeasures.add(new DatasetMeasure(dim[x]));
        }
    }

}
