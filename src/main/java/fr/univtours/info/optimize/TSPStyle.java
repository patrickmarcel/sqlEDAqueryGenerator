package fr.univtours.info.optimize;

//Hungarian algorithm https://github.com/aalmi/HungarianAlgorithm/blob/master/HungarianAlgorithm.java
// https://mvnrepository.com/artifact/munkres/munkres/0.3.0

import edu.princeton.cs.algs4.AssignmentProblem;

import java.util.*;

public class TSPStyle {
    public static void main(String[] args) {
        final String file = "22_500.dat";
        final String path="C:\\Users\\chanson\\CLionProjects\\Cplex-TAP\\instances\\tap_" + file;
        final String out_path = "C:\\Users\\chanson\\Desktop\\warm_start_" + file;
        double temps = 0.25, dist = 0.35;

        InstanceFiles.RawInstance ist = InstanceFiles.readFile(path);
        System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);
        double eptime = Math.round(temps * ist.size * 27.5f);

        // 1 solve affectation
        List<List<Integer>> subtours = solveAffectation(ist.distances);

        // 2.1.1 solve Md-KS to find a collection of subtours
        boolean[] KSSolution = MDKnapsack.solve2DNaive(subtours.stream().mapToDouble(st -> subtourValue(st, ist)).toArray(),
                subtours.stream().mapToDouble(st -> subtourTime(st, ist)).toArray(), eptime,
                subtours.stream().mapToDouble(st -> subtourDistance(st, ist)).toArray(), epdist);
        List<List<Integer>> selected = new ArrayList<>();
        for (int i = 0; i < KSSolution.length; i++) {
            if (KSSolution[i])
                selected.add(subtours.get(i));
        }
        System.out.println(selected);

        // 2.1.2 stitch subtours
        List<Integer> full = stitch(selected);

        // 2.1.3 check constraint (distance)
        boolean cs_check = true;

        // 2.1.4


    }

    public static List<Integer> stitch(List<List<Integer>> tours){
        List<Integer> full = tours.get(0);
        tours.remove(full);

        //TODO

        return full;
    }

    public static double subtourValue(List<Integer> tour, InstanceFiles.RawInstance ist){
        return tour.stream().mapToDouble(i -> ist.interest[i]).sum();
    }

    public static double subtourTime(List<Integer> tour, InstanceFiles.RawInstance ist){
        return tour.stream().mapToDouble(i -> ist.costs[i]).sum();
    }

    public static double subtourDistance(List<Integer> tour, InstanceFiles.RawInstance ist){
        double d = ist.distances[tour.get(tour.size() - 1)][tour.get(0)];
        for (int i = 0; i < tour.size() - 1; i++) {
            d += ist.distances[tour.get(i)][tour.get(i+1)];
        }
        return d;
    }

    public static List<List<Integer>> solveAffectation(double[][] distances){
        List<List<Integer>> subtours = new ArrayList<>();

        distances = Arrays.stream(distances).map(double[]::clone).toArray(double[][]::new);
        // Make sure distance to self is very high ~= infinite
        for (int i = 0; i < distances.length; i++) {
            distances[i][i] = 10e9;
        }

        //Call hungarian method lib
        AssignmentProblem assignmentProblem = new AssignmentProblem(distances);
        //Remember points already assigned to subtours
        TreeSet<Integer> done = new TreeSet<>();
        //We can stop early if all points are put in their subtours
        for (int i = 0; i < distances.length && done.size() != distances.length; i++) {
            if (!done.contains(i)){
                ArrayList<Integer> subtour = new ArrayList<>();
                int start = i;
                int current = -1;
                subtour.add(i);
                done.add(i);
                // follow along until back at the 'first' point
                current = assignmentProblem.sol(start);
                while (current != start){
                    done.add(current);
                    subtour.add(current);
                    current = assignmentProblem.sol(current);
                }
                subtours.add(subtour);
            }
        }

        System.out.println(subtours);
        return subtours;
    }

}
