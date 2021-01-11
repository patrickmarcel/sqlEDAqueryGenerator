package fr.univtours.info;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

/**
 *
 *
 */
public class App {
    Dataset ds;
    String table;
    ArrayList<DatasetDimension> theDimensions;
    ArrayList<DatasetMeasure> theMeasures;

    public static void main( String[] args ) {
    }


    void loadDataset() throws Exception{
        DBservices db= new DBservices();
        db.connectToPostgresql();
        readProperties();
        ds=new Dataset();
    }

    void generate(){
        ImmutableSet<String> set = ImmutableSet.of("APPLE", "ORANGE", "MANGO");
        Set<Set<String>> powerSet = Sets.powerSet(set);
    }

    void readProperties() throws Exception{
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
