package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import fr.univtours.info.queries.AbstractEDAsqlQuery;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class KnapsackStyle implements TAPEngine{


    public static void main(String[] args) throws Exception {
        final String path="C:\\Users\\chanson\\CLionProjects\\Cplex-TAP\\instances\\tap_12_500.dat";
        double dist = 0.2, temps = 0.15;

        InstanceFiles.RawInstance ist = InstanceFiles.readFile(path);
        System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);;
        double eptime = Math.round(temps * ist.size * 27.5f);;


        List<Integer> solution = new ArrayList<>();
        List<Element> order = new ArrayList<>();
        for (int i = 0; i < ist.size; i++) {
            order.add(new Element(i, ist.interest[i]));
        }
        order.sort(Comparator.comparing(Element::getValue).reversed());

        double total_dist = 0;
        double total_time = 0;
        double z = 0;


        for (int i = 0; i < ist.size; i++)
        {
            int current = order.get(i).index;

            if (eptime - (total_time + ist.costs[current]) > 0){
                if (solution.size() > 0 && epdist - (total_dist + ist.distances[solution.get(solution.size() - 1)][current]) < 0)
                    continue;
                if (solution.size() >0)
                    total_dist += ist.distances[solution.get(solution.size() - 1)][current];
                total_time += ist.costs[current];
                solution.add(current);
                z += ist.interest[current];
            }
        }



        // Write best solution to file for CPLEX
        System.out.println("Z=" + z + " | Sol=" + solution);
        FileOutputStream fos = new FileOutputStream("C:\\Users\\chanson\\Desktop\\warm_start.dat");
        PrintWriter pw = new PrintWriter(fos);
        pw.println(solution.toString().replace("[", "").replace("]", "").replace(", ", " "));
        pw.close();
        fos.close();
    }


    public static int argMax(Double... a) {
        double v = Integer.MIN_VALUE;
        int ind = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > v) {
                v = a[i];
                ind = i;
            }
        }
        return ind;
    }


    @Override
    public List<AbstractEDAsqlQuery> solve(List<AbstractEDAsqlQuery> theQ, int timeBudget, int maxDistance) {
        int size = theQ.size();

        List<Integer> solution = new ArrayList<>();
        List<Element> order = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            order.add(new Element(i, theQ.get(i).getInterest()));
        }
        order.sort(Comparator.comparing(Element::getValue).reversed());

        double total_dist = 0;
        double total_time = 0;
        double z = 0;


        for (int i = 0; i < size; i++)
        {
            int current = order.get(i).index;

            if (timeBudget - (total_time + theQ.get(current).getEstimatedCost()) > 0){
                if (solution.size() > 0 && maxDistance - (total_dist + theQ.get(solution.get(solution.size() - 1)).dist(theQ.get(current))) < 0)
                    continue;
                if (solution.size() >0)
                    //total_dist += ist.distances[solution.get(solution.size() - 1)][current];
                    total_dist += theQ.get(solution.get(solution.size() - 1)).dist(theQ.get(current));
                total_time += theQ.get(current).getEstimatedCost();
                solution.add(current);
                z += theQ.get(current).getInterest();
            }
        }
        return solution.stream().map(theQ::get).collect(Collectors.toList());
    }
}

