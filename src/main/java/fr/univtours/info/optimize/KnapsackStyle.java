package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import fr.univtours.info.queries.Query;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class KnapsackStyle implements TAPEngine{
    private static int current_ID = -1;
    private static int current_SIZE = -1;

    static class DPQ {
        int gbId, LselId, RselId, lmId, rmId;
        String selDim, agg;

        public DPQ(String in){
            String tmp[] = in.split(";");
            agg = tmp[0];
            lmId = Integer.parseInt(tmp[1]);
            rmId = Integer.parseInt(tmp[2]);
            gbId = Integer.parseInt(tmp[3]);
            selDim = tmp[4].split("=")[0];
            LselId = Integer.parseInt(tmp[4].split("=")[1].replace("&", ""));
            RselId = Integer.parseInt(tmp[5].split("=")[1].replace("&", ""));
        }

        public double dist(DPQ other){
            int diffs = 0;
            // Agg function changed ?
            if(agg != other.agg)  diffs += 1;
            // Measure changed ?
            if(lmId != other.lmId) diffs += 1;
            if(rmId != other.rmId) diffs += 1;

            // predicates
            if(!selDim.equals(other.selDim) || LselId != other.LselId) {
                diffs += 2;
            }
            if(!selDim.equals(other.selDim) || RselId != other.RselId) {
                diffs += 2;
            }
            // Group by dimension
            if (gbId != other.gbId){
                diffs += 5;
            }

            return diffs;
        }

    }

    public static void main(String[] args) throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get("/home/alex/IdeaProjects/sqlEDAqueryGenerator/data/to_run"))) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file);
                    var tmp = file.getFileName().toString().split("_");
                    int epdist = Integer.parseInt(tmp[4]);
                    int eptime = Integer.parseInt(tmp[3]);
                    int nbQ = Integer.parseInt(lines.get(0));
                    double[] interest = new double[nbQ];
                    double[] temps = new double[nbQ];
                    int idx = 0;
                    for (String el : lines.get(1).split(" "))
                        interest[idx++] = Double.parseDouble(el);
                    idx = 0;
                    for (String el : lines.get(2).split(" "))
                        temps[idx++] = Double.parseDouble(el);


                    List<DPQ> qs = new ArrayList<>(nbQ);
                    for (String line : lines.subList(3, lines.size())){
                        qs.add(new DPQ(line));
                    }

                    System.out.println(tmp[4]+"_"+tmp[2]+"_"+tmp[3]);
                    System.out.println(solve(qs, eptime, epdist, interest, temps));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }



    }

    private static void compute(String path, String out_path, double temps, double dist) throws IOException {
        InstanceLegacy ist = InstanceLegacy.readFile(path);
        //System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round(dist * ist.size * 4.5);
        //double epdist = Math.round(dist * ist.size * 7); //f2
        //double epdist = Math.round(dist * ist.size * 5.5); //f1
        double eptime = Math.round(temps * ist.size * 27.5f);
        //double eptime = Math.round(temps * ist.size * 6); //f2
        //double eptime = Math.round(temps * ist.size * 27.5); //f1


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
        //System.out.println("Distance: " + total_dist + "/" + epdist + " |Time: " + total_time + "/" + eptime);
        //System.out.println("Z=" + z + " | Sol=" + solution);

        // Post processing
        while (true) {
            List<Integer> prevSolution = new ArrayList<>(solution);
            int victim = argMinInt(solution, ist);
            solution.remove(Integer.valueOf(victim));
            total_dist = sequenceDistance(solution, ist);
            total_time -= ist.costs[victim];
            z -= ist.interest[victim];

            List<Element> potential = new ArrayList<>();
            for (int i = 0; i < ist.size; i++) {
                if (i != victim && !solution.contains(Integer.valueOf(i)) && ist.interest[victim] < ist.interest[i])
                    potential.add(new Element(i, ist.interest[i]));
            }
            potential.sort(Comparator.comparing(Element::getValue).reversed());

            boolean success = false;
            for (Element e : potential) {
                if (eptime - (total_time + ist.costs[e.index]) > 0) {
                    double backup = total_dist;
                    total_dist += insert_opt(solution, e.index, ist.distances);
                    if (total_dist > epdist) {
                        //rollback and check next querry
                        solution.remove(Integer.valueOf(e.index));
                        total_dist = backup;
                        continue;
                    }
                    total_time += ist.costs[e.index];
                    z += ist.interest[e.index];
                    success = true;
                    break;
                }
            }
            if (!success){
                solution = prevSolution;
                z = solution.stream().mapToDouble(idx -> ist.interest[idx]).sum();
                break;
            }
        }

        assert sequenceDistance(solution, ist) <= epdist && solution.stream().mapToDouble(idx -> ist.costs[idx]).sum() <= eptime;

        // Write best solution to file for CPLEX
        FileOutputStream fos = new FileOutputStream(out_path);
        PrintWriter pw = new PrintWriter(fos);
        pw.println(solution.toString().replace("[", "").replace("]", "").replace(", ", " "));
        pw.close();
        fos.close();
        System.out.printf("%s;%s;%s;%s;%s%n",current_ID,current_SIZE,"0.01",z,solution.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    static double insert_opt(List<Integer> solution, int candidate, double[][] distances) {
        if (solution.size() == 0){
            solution.add(candidate);
            return 0;
        }
        double best_insert_cost = 10e50;// large enough
        int best_insert_pos = -1;
        for (int i = 0; i <= solution.size(); i++) {
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

    static double insert_opt(List<Integer> solution, int candidate, List<Query> queries) {
        if (solution.size() == 0){
            solution.add(candidate);
            return 0;
        }
        double best_insert_cost = 10e50;// large enough
        int best_insert_pos = -1;
        Query candidateQuery = queries.get(candidate);
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
    public List<Query> solve(List<Query> theQ, int timeBudget, int maxDistance) {
        System.out.println("[INFO] KS Heuristic : Init");
        int size = theQ.size();

        List<Integer> solution = new ArrayList<>();
        Element[] order = new Element[size];
        for (int i = 0; i < size; i++) {
            order[i] = new Element(i, theQ.get(i).getInterest()/theQ.get(i).estimatedTime());
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
                double backup = total_dist;
                total_dist += insert_opt(solution, current, theQ);
                if (total_dist > maxDistance){
                    //rollback and check next querry
                    solution.remove(Integer.valueOf(current));
                    total_dist = backup;
                } else {
                    total_time += theQ.get(current).estimatedTime();
                    z += theQ.get(current).getInterest();
                }
            }
        }
        System.out.println("[INFO] KS Heuristic : solution is done z=" + z);
        return solution.stream().map(theQ::get).collect(Collectors.toList());
    }

    public static List<Integer> solve(List<DPQ> theQ, int timeBudget, int maxDistance, double[] interest , double[] times ) {
        System.out.println("[INFO] KS Heuristic : Init, |Q|=" + theQ.size());
        int size = theQ.size();

        List<Integer> solution = new ArrayList<>();
        Element[] order = new Element[size];
        for (int i = 0; i < size; i++) {
            order[i] = new Element(i, interest[i]/times[i]);
        }

        // Merge sort see javadoc
        System.out.println("[INFO] KS Heuristic : Starting sort");
        Arrays.sort(order, Comparator.comparing(Element::getValue).reversed());
        System.out.println("[INFO] KS Heuristic : Sort Complete");

        for (int i = 0; i < 100; i++) {
            System.out.print(order[i].index + "|" + order[i].value + " ");
        }
        System.out.println();

        double total_dist = 0;
        double total_time = 0;
        double z = 0;


        System.out.println("[INFO] KS Heuristic : Construction Solution");
        for (int i = 0; i < size; i++)
        {
            int current = order[i].index;

            if (timeBudget - (total_time + times[current]) >= 0){
                double backup = total_dist;
                total_dist += insert_opt_dpq(solution, current, theQ);
                if (total_dist > maxDistance){
                    //rollback and check next querry
                    solution.remove(Integer.valueOf(current));
                    total_dist = backup;
                } else {
                    total_time += times[current];
                    z += interest[current];
                }
            }
        }
        System.out.println("[INFO] KS Heuristic : eptime=" + total_time + "/" + timeBudget);
        System.out.println("[INFO] KS Heuristic : epdist=" + total_dist + "/" + maxDistance);
        System.out.println("[INFO] KS Heuristic : solution is done z=" + z);

        return solution;
    }

    static double insert_opt_dpq(List<Integer> solution, int candidate, List<DPQ> queries) {
        if (solution.size() == 0){
            solution.add(candidate);
            return 0;
        }
        double best_insert_cost = 10e50;// large enough
        int best_insert_pos = -1;
        DPQ candidateQuery = queries.get(candidate);
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

    private static int argMinInt(List<Integer> sol, InstanceLegacy ist){
        List<Element> order = new ArrayList<>();
        for (int i : sol) {
            order.add(new Element(i, ist.interest[i]));
        }
        order.sort(Comparator.comparing(Element::getValue));
        return order.get(0).getIndex();
    }

    public static double sequenceDistance(List<Integer> tour, InstanceLegacy ist){
        double d = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            d += ist.distances[tour.get(i)][tour.get(i+1)];
        }
        return d;
    }
}

