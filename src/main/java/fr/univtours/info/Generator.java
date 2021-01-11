package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 */
public class Generator {
    static Dataset ds;
    static String table;
    static ArrayList<DatasetDimension> theDimensions;
    static ArrayList<DatasetMeasure> theMeasures;
    static Connection conn;
    static QtheSetOfGeneratedQueries theQ;

    static final String[] tabAgg= {"avg", "sum", "min", "max", "stddev"};

    public static void main( String[] args ) throws Exception{
        loadDataset();
        theQ=new QtheSetOfGeneratedQueries();



        //generation
        Stopwatch stopwatch = Stopwatch.createStarted();

        generateCounts();
        generateHistograms();
        generateAggregates();

        stopwatch.stop();

        // get elapsed time, expressed in milliseconds
        long timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Generation time in milliseconds: " + timeElapsed);
        System.out.println("Q size: " + theQ.getSize() + " queries generated");


        // interestingness computation
        stopwatch = Stopwatch.createStarted();
        computeInterests();
        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Interestingness computation time in milliseconds: " + timeElapsed);

        // actual cost computation
        stopwatch = Stopwatch.createStarted();
        //computeCosts();
        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Actual cost computation time in milliseconds: " + timeElapsed);
    }


    static void computeCosts() throws Exception {
        for(EDAsqlQuery q : theQ.theQueries){
            q.explainAnalyze();
        }
    }

    static void computeInterests() throws Exception {
        for(EDAsqlQuery q : theQ.theQueries){
            q.computeInterest();
        }
    }

    static void loadDataset() throws Exception{
        DBservices db= new DBservices();
        conn=db.connectToPostgresql();
        readProperties();
        ds=new Dataset(theDimensions, theMeasures);
    }

    static void generateAggregates() throws Exception {
        ImmutableSet<DatasetDimension> set = ImmutableSet.copyOf(theDimensions);
        Set<Set<DatasetDimension>> powerSet = Sets.powerSet(set);

        for(Set<DatasetDimension> s : powerSet) {
            for(DatasetMeasure meas : theMeasures) {
                for (String agg : tabAgg) {
                    AggregateQuery aq = new AggregateQuery(conn, table, s, meas, agg);
                    theQ.addQuery(aq);

                    //aq.execute();
                    //aq.explainAnalyze();

                    //System.out.println(aq.sql);
                    //System.out.println(aq.cost);
                }
            }
        }
    }

    static void generateHistograms(){
        for(DatasetDimension d : theDimensions){
            HistogramQuery hq = new HistogramQuery(conn, table, d);
            theQ.addQuery(hq);
        }
    }

    static void generateCounts() throws Exception {
        for(DatasetDimension dim : theDimensions){
            CountdistinctQuery cdq= new CountdistinctQuery(conn, table, dim) ;
            theQ.addQuery(cdq);
            //cdq.execute();
            //cdq.explainAnalyze();

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
            theMeasures.add(new DatasetMeasure(meas[x]));
        }
    }

}
