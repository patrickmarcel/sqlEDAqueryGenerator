package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import com.alexscode.utilities.collection.ElementPair;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

public class ClarkAndWhright {
    public static void main(String[] args) throws Exception{
        final String file = "12_500.dat";
        final String path="data/tap_" + file;
        final String out_path = "data/warm_start_" + file;
        double dist = 0.05, temps = 0.15;

        InstanceFiles.RawInstance ist = InstanceFiles.readFile(path);
        System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);;
        double eptime = Math.round(temps * ist.size * 27.5f);;


        int depot = 0;

        List<Integer> solution = new ArrayList<>();

        List<ElementPair> order = new ArrayList<>();
        for (int i = 0; i < ist.size; i++) {
            for (int j = 0; j < ist.size; j++) {
                if (i != depot && j != depot){
                    order.add(new ElementPair(i, j, ist.distances[i][depot] + ist.distances[j][depot] - ist.distances[i][j]));
                }
            }
        }
        order.sort(Comparator.comparing(ElementPair::getValue).reversed());

        double total_dist = 0;
        double total_time = 0;
        double z = 0;

        Set<ElementPair> added = new HashSet<>();
        for (ElementPair couple : order){
            final int i = couple.i;
            final int j = couple.j;

            //Base case solution empty
            if (solution.isEmpty()){
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
            if (!solution.contains(i) && !solution.contains(i)){
                //Time Constraint check
                if (total_time + ist.costs[i] + ist.costs[j] > eptime)
                    continue;
                //Attempt to add them
                double backup = total_dist;
                total_dist += KnapsackStyle.insert_opt(solution, i, ist.distances, total_dist);
                total_dist += KnapsackStyle.insert_opt(solution, j, ist.distances, total_dist);

                if (total_dist > epdist){
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
            if (solution.contains(i) || solution.contains(j)){
                //Constraint check
                if (total_dist + ist.distances[i][j] > epdist)
                    continue;
                if (solution.get(0) == i){
                    if (total_time + ist.costs[j] > eptime)
                        continue;
                    solution.add(0, j);
                    total_time += ist.costs[j];
                } else if (solution.get(0) == j){
                    if (total_time + ist.costs[i] > eptime)
                        continue;
                    solution.add(0, i);
                    total_time += ist.costs[i];
                } else if (solution.get(solution.size()-1) == i){
                    if (total_time + ist.costs[j] > eptime)
                        continue;
                    solution.add(j);
                    total_time += ist.costs[j];
                } else if (solution.get(solution.size()-1) == j){
                    if (total_time + ist.costs[i] > eptime)
                        continue;
                    solution.add(i);
                    total_time += ist.costs[i];
                }

                //insert and update solution
                added.add(couple);
                z += ist.interest[i] + ist.interest[j];
                total_dist += ist.distances[i][j];
                continue;
            }
        }

        System.out.println("Distance: " + total_dist + "/" + epdist);
        System.out.println("Time: " + total_time + "/" + eptime);
        // Write best solution to file for CPLEX
        System.out.println("Z=" + z + " | Sol=" + solution);
        FileOutputStream fos = new FileOutputStream(out_path);
        PrintWriter pw = new PrintWriter(fos);
        pw.println(solution.toString().replace("[", "").replace("]", "").replace(", ", " "));
        pw.close();
        fos.close();
    }
}
