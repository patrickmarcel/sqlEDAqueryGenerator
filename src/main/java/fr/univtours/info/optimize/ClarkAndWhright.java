package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.ElementPair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ClarkAndWhright {
    public static void main(String[] args) throws Exception{
        final String file = "22_500.dat";
        final String path="C:\\Users\\chanson\\CLionProjects\\Cplex-TAP\\instances\\tap_" + file;
        final String out_path = "C:\\Users\\chanson\\Desktop\\warm_start_" + file;
        double temps = 0.25, dist = 0.35;




        var folder = "C:\\Users\\chanson\\Desktop\\instances\\";
        for (int i = 0; i < 30; i++) {
            for (int size : new int[]{20, 40, 60, 80, 100, 150, 200, 250, 300, 350, 400, 450, 500}) {
                var in = folder + "tap_" + i + "_" + size + ".dat";
                var out = folder + "tap_" + i + "_" + size + ".warm";

                Instance ist = InstanceFiles.readFile(in);
                System.out.println("Loaded " + in + " | " + ist.size + " queries");
                double epdist = Math.round( dist * ist.size * 4.5) - 1;
                double eptime = Math.round(temps * ist.size * 27.5f) - 1;

                compute(out, ist, epdist, eptime);
            }
        }


    }

    private static void compute(String out_path, Instance ist, double epdist, double eptime) throws IOException {
        List<Double> zs = new ArrayList<>();
        List<List<Integer>> solutions = new ArrayList<>();
        for (int dpt = 0; dpt < ist.size; dpt++) {

            int depot = dpt;

            List<Integer> solution = new ArrayList<>();

            List<ElementPair> order = new ArrayList<>();
            for (int i = 0; i < ist.size; i++) {
                for (int j = 0; j < ist.size; j++) {
                    if (i != depot && j != depot) {
                        order.add(new ElementPair(i, j, ist.distances[i][depot] + ist.distances[j][depot] - ist.distances[i][j]));
                    }
                }
            }
            order.sort(Comparator.comparing(ElementPair::getValue).reversed());

            double total_dist = 0;
            double total_time = 0;
            double z = 0;

            Set<ElementPair> added = new HashSet<>();
            for (ElementPair couple : order) {
                final int i = couple.i;
                final int j = couple.j;

                //Base case solution empty
                if (solution.isEmpty()) {
                    //Constraint check
                    if (total_dist + ist.distances[i][j] > epdist || total_time + ist.costs[i] + ist.costs[j] > eptime)
                        continue;
                    //insert and update solution
                    solution.add(i);
                    solution.add(j);
                    added.add(couple);
                    z += ist.interest[i] + ist.interest[j];
                    total_dist += ist.distances[i][j];
                    total_time += ist.costs[i] + ist.costs[j];
                    continue;
                }

                //Points already in solution
                if (solution.contains(i) && solution.contains(j))
                    continue;

                // Points outside of solution add them at best possible place ?
                if (!solution.contains(i) && !solution.contains(i)) {
                    //Time Constraint check
                    if (total_time + ist.costs[i] + ist.costs[j] > eptime)
                        continue;
                    //Attempt to add them
                    double backup = total_dist;
                    total_dist += KnapsackStyle.insert_opt(solution, i, ist.distances, total_dist);
                    total_dist += KnapsackStyle.insert_opt(solution, j, ist.distances, total_dist);

                    if (total_dist > epdist) {
                        //rollback and check next couple
                        solution.remove(Integer.valueOf(i));
                        solution.remove(Integer.valueOf(j));
                        total_dist = backup;
                        continue;
                    }
                    total_time += ist.costs[i] + ist.costs[j];
                    z += ist.interest[i] + ist.interest[j];
                    added.add(couple);
                    continue;
                }

                // One point in solution start or end
                if (solution.contains(i) || solution.contains(j)) {
                    //Constraint check
                    if (total_dist + ist.distances[i][j] > epdist)
                        continue;

                    if (solution.get(0) == i) {
                        if (total_time + ist.costs[j] > eptime)
                            continue;
                        solution.add(0, j);
                        total_time += ist.costs[j];
                    } else if (solution.get(0) == j) {
                        if (total_time + ist.costs[i] > eptime)
                            continue;
                        solution.add(0, i);
                        total_time += ist.costs[i];
                    } else if (solution.get(solution.size() - 1) == i) {
                        if (total_time + ist.costs[j] > eptime)
                            continue;
                        solution.add(j);
                        total_time += ist.costs[j];
                    } else if (solution.get(solution.size() - 1) == j) {
                        if (total_time + ist.costs[i] > eptime)
                            continue;
                        solution.add(i);
                        total_time += ist.costs[i];
                    } else
                        continue;
                    //insert and update solution
                    added.add(couple);
                    z += ist.interest[i] + ist.interest[j];
                    total_dist += ist.distances[i][j];
                    continue;
                }
            }
            zs.add(z);
            solutions.add(solution);
        }

        double z = zs.stream().max(Double::compareTo).get();
        List<Integer> solution = solutions.get(zs.indexOf(z));


        // Write best solution to file for CPLEX
        System.out.println("Z=" + z + " | Sol=" + solution);
        FileOutputStream fos = new FileOutputStream(out_path);
        PrintWriter pw = new PrintWriter(fos);
        pw.println(solution.toString().replace("[", "").replace("]", "").replace(", ", " "));
        pw.close();
        fos.close();


        zs.sort(Double::compareTo);
        System.out.println(zs);
    }
}
