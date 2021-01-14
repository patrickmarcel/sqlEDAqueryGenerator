package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import fr.univtours.info.queries.*;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.*;
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
        //Cleanup previous execution
        DBservices db= new DBservices();
        conn=db.connectToPostgresql();
        conn.prepareStatement("Drop table if exists sample_1").execute();
        conn.close();

        loadDataset();
        theQ=new QtheSetOfGeneratedQueries();

        // compute sample
        //this.table=ds.computeSample(0.1);
        //

        //generation
        System.out.println("Starting generation");
        Stopwatch stopwatch = Stopwatch.createStarted();

        //generateCounts();
        //generateHistograms();
        //generateAggregates();
        generateSiblingAssesses();

        stopwatch.stop();

        // get elapsed time, expressed in milliseconds
        long timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Generation time in milliseconds: " + timeElapsed);
        System.out.println("Q size: " + theQ.getSize() + " queries generated");


        // interestingness computation
        System.out.println("Starting interestingness computation");
        stopwatch = Stopwatch.createStarted();

        //computeInterests();

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Interestingness computation time in milliseconds: " + timeElapsed);

        // actual cost computation
        System.out.println("Starting cost computation");
        stopwatch = Stopwatch.createStarted();

        computeCosts();

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Cost computation time in milliseconds: " + timeElapsed);



    }

    // todo: check if optimization is reasonable in practice...
    static void computeCosts() throws Exception {
        float avg=0, i=0, min=Float.MAX_VALUE, max=0, current=0;
        int nbExplain=0;
        AbstractEDAsqlQuery previous=null;
        Random r = new Random();
        int correct = 0, wrong = 0;

        for(AbstractEDAsqlQuery q : theQ.theQueries){
            //q.explainAnalyze();

            // THIS MUST BE CHECKED BECAUSE EVEN WHEN DISTANCE IS 0 THE COST MAY DIFFER
            // ESPECIALLY WHEN VALUES IN SELECTION PREDICATE ARE ORDERED
            if(previous!=null && q.getDistance(previous)==0){
                q.setEstimatedCost(current);
                if (r.nextFloat() < 0.1){
                    q.explain();
                    if (Math.abs(q.getEstimatedCost() - current) < 0.1){
                        correct += 1;
                    }
                    else {
                        wrong += 1;
                    }
                }
            }
            else{
                q.explain();
                nbExplain++;

            }
            current=q.getEstimatedCost();
            previous=q;
            i++;
            avg=avg+q.getEstimatedCost();
            if(q.getEstimatedCost()>max) max = q.getEstimatedCost();
            if(q.getEstimatedCost()<min) min = q.getEstimatedCost();

        }
        System.out.println("Number of explain: "+nbExplain);
        System.out.println("Min: "+min);
        System.out.println("Avg: "+avg/i);
        System.out.println("Max: "+max);
        System.out.printf("Checking optimization : Correct %s, Wrong %s%n", correct, wrong);
    }

    static void computeInterests() throws Exception {
        for(AbstractEDAsqlQuery q : theQ.theQueries){
            q.computeInterest();
            q.print();
            //q.printResult();
            System.out.println("interestingness: " + q.getInterest());
        }
    }

    static void loadDataset() throws Exception{
        DBservices db= new DBservices();
        conn=db.connectToPostgresql();
        readProperties();
        ds=new Dataset(conn, table, theDimensions, theMeasures);
    }


    static void generateSiblingAssesses() throws Exception{
        ImmutableSet<DatasetDimension> set = ImmutableSet.copyOf(theDimensions);
        Set<Set<DatasetDimension>> combinations = Sets.combinations(set, 2);

        for(Set<DatasetDimension> s : combinations){
            for(DatasetMeasure meas : theMeasures) {
                for (String agg : tabAgg) {
                    DatasetDimension[] tabdim=new DatasetDimension[2];
                    int i=0;
                    for(DatasetDimension d : s){
                        tabdim[i++]=d;
                    }
                    ImmutableSet<String> values = ImmutableSet.copyOf(tabdim[0].getActiveDomain());
                    Set<Set<String>> combiVals = Sets.combinations(values, 2);

                    for(Set<String> st : combiVals){
                        i=0;
                        String[] tabstring = new String[2];
                        for(String sc : st){
                            tabstring[i++]=sc;
                        }
                        //System.out.println(tabdim[0] + tabstring[0] +  tabstring[1] +  tabdim[1] );
                        SiblingAssessQuery saq = new SiblingAssessQuery(conn, table, tabdim[0], tabstring[0], tabstring[1], tabdim[1], meas, agg);
                        //System.out.println(saq);
                        theQ.addQuery(saq);

                    }
                }
            }
        }
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
            theDimensions.add(new DatasetDimension(dim[x], conn, table));
            theDimensions.get(x).setActiveDomain();
        }
        String[] meas=measures.split(",");
        for (int x=0; x<meas.length; x++) {
            theMeasures.add(new DatasetMeasure(meas[x], conn, table));
        }
    }

}
