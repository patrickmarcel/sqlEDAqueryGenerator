package fr.univtours.info.optimize;
import com.alexscode.utilities.collection.Element;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class MonteCarlo {


    public static void main(String[] args) throws Exception {
        final String path="C:\\Users\\chanson\\CLionProjects\\Cplex-TAP\\instances\\tap_22_500.dat";
        double dist = 0.2, temps = 0.15;

        InstanceLegacy ist = InstanceLegacy.readFile(path);
        System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);;
        double eptime = Math.round(temps * ist.size * 27.5f);;
        double alpha = 0.1, r = 4;
        int starts_to_consider = 20;
        RandomGenerator rng = new Well19937c();

        List<Double> values = new ArrayList<>();
        List<List<Integer>> solutions = new ArrayList<>();

        long startTime = System.nanoTime();
        for (int sidx = 0; sidx < starts_to_consider; sidx++) {
            for (int sample = 0; sample < 100; sample++) {

                double[] centralities = computeCentralities(ist);
                boolean[] selected = new boolean[ist.size];
                List<Integer> solution = new ArrayList<>();

                //Select the most central node for now
                //TODO add random stuff in here
                List<Element> order = new ArrayList<>();
                int[] idx = new int[ist.size];//just 0,1,2..n
                for (int i = 0; i < ist.size; i++) {
                    idx[i] = i;
                    order.add(new Element(i, centralities[i]));
                }
                Collections.sort(order);

                int first = order.get(sidx).index;
                solution.add(first);
                selected[first] = true;

                int current = first;
                double total_dist = 0;
                double total_time = ist.costs[first];
                double z = ist.interest[first];
                if (total_time > eptime) {
                    System.err.println("Warning Infeasible");
                }

                while (true) {
                    double[] feasability = new double[ist.size];
                    double sum = 0;
                    for (int i = 0; i < ist.size; i++) {
                        double t = eptime - total_time - ist.costs[i];
                        //small optimization ?
                        if (!selected[i] && t > 0d) {
                            double e = alpha * (epdist - total_dist - ist.distances[current][i]) * centralities[i] * (t);
                            if (e < 0)
                                feasability[i] = 0d;
                            else
                                feasability[i] = Math.pow((ist.interest[i] + e) / (ist.distances[current][i]), r);
                        } else
                            feasability[i] = 0d;
                        sum += feasability[i];
                    }

                    //finished solution
                    if (sum == 0d) {
                        break;
                    }

                    double[] p = new double[ist.size];
                    for (int i = 0; i < ist.size; i++) {
                        p[i] = feasability[i] / sum;
                    }
                    EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(rng, idx, p);
                    int next = distribution.sample(); //check it doesn't draw for p = 0 just to be sure

                    selected[next] = true;
                    solution.add(next);
                    total_dist += ist.distances[current][next];
                    total_time += ist.costs[next];
                    z += ist.interest[next];
                    current = next;
                }

                solutions.add(solution);
                values.add(z);
            }

        }
        long endTime = System.nanoTime();
        System.out.println("Time " + (endTime - startTime) / 1000000 + " ms");
        int best = argMax(values.toArray(new Double[0]));
        System.out.println("IDX = " + best + " Z = " + values.get(best));
        System.out.println(solutions.get(best));
        //System.out.printf("Z = %s | T_dist = %s | T_time = %s%n", z, total_dist, total_time);
        //System.out.println(solution);

        // Write best solution to file for CPLEX
        FileOutputStream fos = new FileOutputStream("C:\\Users\\chanson\\Desktop\\warm_start.dat");
        List<Integer> s = solutions.get(best);
        PrintWriter pw = new PrintWriter(fos);
        pw.println(s.toString().replace("[", "").replace("]", "").replace(", ", " "));
        pw.close();
        fos.close();
    }

    private static double[] computeCentralities(InstanceLegacy ist) {
        double[] centrality = new double[ist.size];
        for (int i = 0; i < ist.size; i++) {
            double c = 0;
            for (int j = 0; j < ist.size; j++) {
                if (i != j){
                    c += (ist.interest[j]/ ist.costs[j])/ ist.distances[i][j];
                }
            }
            centrality[i] = c;
        }
        return centrality;
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


}

