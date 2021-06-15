package fr.univtours.info.optimize.time;

import com.google.common.base.Stopwatch;
import fr.univtours.info.*;
import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.CandidateQuerySet;


import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeCallibration {


    static final String outCSV = "./data/stats/timed_queries.csv";
    static String table;
    static List<DatasetDimension> theDimensions;
    static List<DatasetMeasure> theMeasures;


    public static void main(String[] args) throws IOException, SQLException {
        DBConfig config = DBConfig.newFromFile();
        table = config.getTable();
        theDimensions = config.getDimensions();
        theMeasures = config.getMeasures();


        // Pre compute stats
        DatasetStats stats = new DatasetStats();
        HashMap<DatasetDimension, Integer> adSize = stats.getAdSize();
        HashMap<DatasetDimension, HashMap<String, Integer>> frequency = stats.getFrequency();
        int rows = stats.getRows();


        //Open csv
        PrintWriter out = new PrintWriter(new FileOutputStream(new File(outCSV)));
        out.println("id,left_sel,right_sel,gb_size,time_ms");

        Generator.init();
        Generator.generateSiblingAssesses();
        CandidateQuerySet theQ = Generator.theQ;

        Connection conn = config.getConnection();
        double sample_rate = 0.005;
        Random rd = new Random();
        int count = 0;
        for (AssessQuery q : theQ){
            if (rd.nextFloat() < sample_rate){
                count +=1;

                double time = timeQuery(q.getSql(), conn);
                String id = "\"" + q.getFunction() + ":" + q.getMeasure().getName() + ":" + q.getReference().getName() + ":" + q.getAssessed().getName() + ":" + q.getVal1() + ":" + q.getVal2() + "\"";
                double left_sel = frequency.get(q.getAssessed()).get(q.getVal1()) / (double) rows;
                double right_sel =  frequency.get(q.getAssessed()).get(q.getVal2()) / (double) rows;
                int gb_size = adSize.get(q.getReference());

                out.printf("%s,%s,%s,%s,%s%n", id, left_sel, right_sel, gb_size, time);
                if (count%100 == 0)
                    out.flush();
            }
        }
        conn.close();

        //Close file
        out.flush();
        out.close();
    }

    public static double timeQuery(String query, java.sql.Connection con){
        try {
            Statement planON = con.createStatement();
            //planON.execute("set statistics time on;");

            Stopwatch sw = Stopwatch.createStarted();
            ResultSet rs = planON.executeQuery(query);

            //Dummy loop don't need the results but go through the ResultSet to be realistic
            int sum = 0;
            while (rs.next()){
                sum = sum + 1; //Should fool any optimizer
            }

            //double t = parseWarning(planON);
            double t = sw.stop().elapsed(TimeUnit.MILLISECONDS);

            planON.close();
            return t;

        } catch (SQLException e){
            System.err.printf("Offending query : [%s]%n", query);
            //e.printStackTrace();
        }
        return -1;
    }

    public static int explainQueryRows(String query, java.sql.Connection con){
        try {
            Statement planON = con.createStatement();
            ResultSet rs = planON.executeQuery("EXPLAIN " + query);

            int sum = 1;
            while (rs.next()){
                sum += rs.getInt("rows");
            }

            planON.close();
            return sum;

        } catch (SQLException e){
            System.err.printf("Offending query : [%s]%n", query);
            //e.printStackTrace();
        }
        return -1;
    }

    static Pattern timePattern = Pattern.compile(".*elapsed time = (\\d*) ms\\.", Pattern.MULTILINE|Pattern.DOTALL);
    public static double parseWarning(Statement st) throws SQLException {
        List<SQLWarning> warnings = new ArrayList<>();
        SQLWarning w = st.getWarnings();
        warnings.add(w);
        while ((w = w.getNextWarning()) != null){
            warnings.add(w);
        }

        for (SQLWarning warning : warnings){
            Matcher m = timePattern.matcher(warning.getMessage());
            boolean status = m.matches();
            if (m.matches()){
                return Double.parseDouble(m.group(1))/1000;
            }
        }
        return -1;
    }

}
