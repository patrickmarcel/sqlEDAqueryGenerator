package fr.univtours.info;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import fr.univtours.info.queries.*;
import org.apache.commons.math3.util.Pair;

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
    static String sampleTable;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;
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

        //compute sample
        sampleTable = ds.computeSample(0.1);


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

        computeInterests();

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Interestingness computation time in milliseconds: " + timeElapsed);

        // actual cost computation
        System.out.println("Starting cost computation");
        stopwatch = Stopwatch.createStarted();

        //computeCosts();

        stopwatch.stop();
        timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println("Cost computation time in milliseconds: " + timeElapsed);



    }

    // todo: check if optimization is reasonable in practice...
    static void computeCosts() throws Exception {
        float current=0;
        int nbExplain=0;
        AbstractEDAsqlQuery previous=null;
        int correct = 0, wrong = 0, bad = 0;
        ArrayList<Float> errors = new ArrayList<>(10000);
        ArrayList<Float> times = new ArrayList<>(theQ.getSize());

        for(AbstractEDAsqlQuery q : theQ.theQueries){
            //q.explainAnalyze();

            // THIS MUST BE CHECKED BECAUSE EVEN WHEN DISTANCE IS 0 THE COST MAY DIFFER
            // ESPECIALLY WHEN VALUES IN SELECTION PREDICATE ARE ORDERED
            if(previous!=null && q.getDistance(previous)==0){
                q.setEstimatedCost(current);
                //error check start
                q.explain();
                times.add(q.getEstimatedCost());
                float error = Math.abs(q.getEstimatedCost() - current);

                if (error < 0.1){
                    correct += 1; }
                else {
                    wrong += 1;
                    errors.add(error);
                    if (error > 100)
                        bad += 1;
                }
                //error check end
                q.setEstimatedCost(current);

            }
            else{
                q.explain();
                nbExplain++;

            }
            current=q.getEstimatedCost();
            previous=q;


        }
        float mean = (float) times.stream().mapToDouble(Float::doubleValue).average().orElse(-1);
        double variance = times.stream()
                .map(k -> k - mean)
                .map(k -> k*k)
                .mapToDouble(k -> k).average().getAsDouble();

        System.out.printf("Global time infos, Min %s, Max %s, AVG %s, STD %s%n", times.stream().mapToDouble(Float::doubleValue).min(), times.stream().mapToDouble(Float::doubleValue).max(),mean,Math.sqrt(variance));
        System.out.println("Number of explain executed: "+nbExplain);
        System.out.printf("Checking optimization : Correct %s, Wrong %s, >100ms  %s%n", correct, wrong, bad);
        float mae = (float) errors.stream().mapToDouble(Float::doubleValue).average().orElse(-1);
        double mae_variance = errors.stream()
                .map(k -> k - mae)
                .map(k -> k*k)
                .mapToDouble(k -> k).average().getAsDouble();
        System.out.printf("MAE %s, std %s%n",mae , Math.sqrt(mae_variance));
    }

    static void computeInterests() throws Exception {
        for(AbstractEDAsqlQuery q : theQ.theQueries.subList(10,20)){
            SampleQuery qs = new SampleQuery(q, sampleTable);
            qs.computeInterest();
            qs.print();
            qs.printResult();
            System.out.println("interestingness: " + qs.getInterest());
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
        Map<Pair<DatasetDimension, DatasetDimension>, Boolean> isAllowed = new HashMap<>();

        //For each pair of dimension attributes
        for(Set<DatasetDimension> s : combinations){
            Iterator<DatasetDimension> it = s.iterator();
            Pair<DatasetDimension, DatasetDimension> dims = new Pair<>(it.next(), it.next());
            Pair<DatasetDimension, DatasetDimension> dims_r = new Pair<>(dims.getSecond(), dims.getFirst());

            //TODO compute if absent in hashmap
            List<Pair<DatasetDimension, DatasetDimension>> toGenerate = new ArrayList<>(2);
            if (isAllowed.computeIfAbsent(dims, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table)) &&
                    isAllowed.computeIfAbsent(dims_r, pair -> !DBUtils.checkAimpliesB(pair.getFirst(), pair.getSecond(), conn, table))) {
                toGenerate.add(dims);
                toGenerate.add(dims_r);
            }

            for (Pair<DatasetDimension, DatasetDimension> dim_pair : toGenerate){
                for(DatasetMeasure meas : theMeasures) {
                    for (String agg : tabAgg) {

                        ImmutableSet<String> values = ImmutableSet.copyOf(dim_pair.getFirst().getActiveDomain());
                        Set<Set<String>> combiVals = Sets.combinations(values, 2);

                        for(Set<String> st : combiVals){
                            int i=0;
                            String[] tabstring = new String[2];
                            for(String sc : st){
                                tabstring[i++]=sc;
                            }
                            //System.out.println(tabdim[0] + tabstring[0] +  tabstring[1] +  tabdim[1] );
                            SiblingAssessQuery saq = new SiblingAssessQuery(conn, table, dim_pair.getFirst(), tabstring[0], tabstring[1], dim_pair.getSecond(), meas, agg);
                            //System.out.println(saq);
                            theQ.addQuery(saq);

                        }
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
            theDimensions.get(x).computeActiveDomain();
        }
        String[] meas=measures.split(",");
        for (int x=0; x<meas.length; x++) {
            theMeasures.add(new DatasetMeasure(meas[x], conn, table));
        }
    }

}
