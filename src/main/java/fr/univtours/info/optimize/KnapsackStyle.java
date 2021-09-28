package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import fr.univtours.info.queries.AssessQuery;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class KnapsackStyle implements TAPEngine{


    public static void main(String[] args) throws Exception {
        final String file = "22_500.dat";
        final String path="C:\\Users\\chanson\\Desktop\\instances\\tap_" + file;
        final String out_path = "C:\\Users\\chanson\\Desktop\\warm_start_" + file;/*
        final String file = "22_100.dat";
        final String path="data/tap_" + file;
        final String out_path = "data/warm_start_" + file;*/
        double temps = 0.15, dist = 0.1;

        //compute(path, out_path, temps, dist);
        //System.exit(0);

        var folder = "C:\\Users\\chanson\\Desktop\\instances\\";
        for (int i = 0; i < 30; i++) {
            for (int size : new int[]{100, 200, 300, 400, 500, 600, 700}) {
                var in = folder + "tap_" + i + "_" + size + ".dat";
                var out = folder + "tap_" + i + "_" + size + ".warm";
                compute(in, out, temps, dist);
            }
        }

        //compute(path, out_path, temps, dist);
    }

    private static void compute(String path, String out_path, double temps, double dist) throws IOException {
        InstanceLegacy ist = InstanceLegacy.readFile(path);
        //System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);
        double eptime = Math.round(temps * ist.size * 27.5f);


        List<Integer> solution = new ArrayList<>();
        List<Element> order = new ArrayList<>();
        for (int i = 0; i < ist.size; i++) {
            order.add(new Element(i, ist.interest[i]/ist.costs[i]));
        }
        order.sort(Comparator.comparing(Element::getValue).reversed());

        double total_dist = 0;
        double total_time = 0;
        double z = 0;

        for (int i = 0; i < ist.size; i++)
        {
            int current = order.get(i).index;

            if (eptime - (total_time + ist.costs[current]) > 0){
                double backup = total_dist;
                total_dist += insert_opt(solution, current, ist.distances);
                if (total_dist > epdist){
                    //rollback and check next querry
                    solution.remove(Integer.valueOf(current));
                    total_dist = backup;
                    continue;
                }
                total_time += ist.costs[current];


                z += ist.interest[current];
            }
        }
        //System.out.println("Distance: " + total_dist + "/" + epdist);
        //System.out.println("Time: " + total_time + "/" + eptime);
        // Write best solution to file for CPLEX
        //System.out.println("Z=" + z + " | Sol=" + solution);
        //FileOutputStream fos = new FileOutputStream(out_path);
        //PrintWriter pw = new PrintWriter(fos);
        //pw.println(solution.toString().replace("[", "").replace("]", "").replace(", ", " "));
        //pw.close();
        //fos.close();
        System.out.printf("%s;%s;%s;%s%n",path.substring(39).replace("_"+ist.size+".dat", ""),ist.size,z,solution.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    static double insert_opt(List<Integer> solution, int candidate, double[][] distances) {
        if (solution.size() == 0){
            solution.add(candidate);
            return 0;
        }
        double best_insert_cost = 10e50;// large enough
        int best_insert_pos = -1;
        for (int i = 0; i < solution.size() + 1; i++) {
            double new_cost = 0;
            // insert at first position
            if (i == 0){
                new_cost += distances[candidate][solution.get(0)];
            } else if (i < solution.size()){
                int current_querry = solution.get(i);
                new_cost += distances[candidate][solution.get(i-1)];
                new_cost += distances[candidate][current_querry];
                new_cost -= distances[solution.get(i-1)][current_querry];
            } else {
                new_cost += distances[solution.get(solution.size()-1)][candidate];
            }
            if (new_cost < best_insert_cost){
                best_insert_cost = new_cost;
                best_insert_pos = i;
            }
        }
        //System.out.println(best_insert_pos);
        solution.add(best_insert_pos, candidate);
        return best_insert_cost;
    }

    static double insert_opt(List<Integer> solution, int candidate, List<AssessQuery> queries) {
        if (solution.size() == 0){
            solution.add(candidate);
            return 0;
        }
        double best_insert_cost = 10e50;// large enough
        int best_insert_pos = -1;
        AssessQuery candidateQuery = queries.get(candidate);
        for (int i = 0; i < solution.size() + 1; i++) {
            double new_cost = 0;
            // insert at first position
            if (i == 0){
                new_cost += candidateQuery.dist(queries.get(solution.get(0)));
            } else if (i < solution.size()){
                int current_querry = solution.get(i);
                new_cost += candidateQuery.dist(queries.get(solution.get(i-1)));
                new_cost += candidateQuery.dist(queries.get(current_querry));
                new_cost -= queries.get(solution.get(i-1)).dist(queries.get(current_querry));
            } else {
                new_cost += queries.get(solution.get(solution.size()-1)).dist(candidateQuery);
            }
            if (new_cost < best_insert_cost){
                best_insert_cost = new_cost;
                best_insert_pos = i;
            }
        }
        solution.add(best_insert_pos, candidate);
        return best_insert_cost;
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
    public List<AssessQuery> solve(List<AssessQuery> theQ, int timeBudget, int maxDistance) {
        System.out.println("[INFO] KS Heuristic : Init");
        int size = theQ.size();

        List<Integer> solution = new ArrayList<>();
        Element[] order = new Element[size];
        for (int i = 0; i < size; i++) {
            order[i] = new Element(i, theQ.get(i).getInterest());
        }

        // Merge sort see javadoc
        System.out.println("[INFO] KS Heuristic : Starting sort");
        Arrays.sort(order, Comparator.comparing(Element::getValue).reversed());
        System.out.println("[INFO] KS Heuristic : Sort Complete");

        double total_dist = 0;
        double total_time = 0;
        double z = 0;


        System.out.println("[INFO] KS Heuristic : Construction Solution");
        for (int i = 0; i < size; i++)
        {
            int current = order[i].index;

            if (timeBudget - (total_time + theQ.get(current).estimatedTime()) >= 0){
                if (solution.size() > 0 && maxDistance - (total_dist + theQ.get(solution.get(solution.size() - 1)).dist(theQ.get(current))) < 0)
                    continue;
                if (solution.size() > 0) {
                    double backup = total_dist;
                    total_dist += insert_opt(solution, current, theQ);
                    if (total_dist > maxDistance){
                        //rollback and check next querry
                        solution.remove(Integer.valueOf(current));
                        total_dist = backup;
                        continue;
                    }
                }

                total_time += theQ.get(current).estimatedTime();
                solution.add(current);
                z += theQ.get(current).getInterest();
            }
        }
        System.out.println("[INFO] KS Heuristic : solution is done z=" + z);
        return solution.stream().map(theQ::get).collect(Collectors.toList());
    }
}

