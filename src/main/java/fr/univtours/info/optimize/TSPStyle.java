package fr.univtours.info.optimize;

import edu.princeton.cs.algs4.AssignmentProblem;
import lombok.Getter;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TSPStyle {
    public static void main(String[] args) {
        //final String file = "22_500.dat";
        //final String path="C:\\Users\\chanson\\Desktop\\instances\\tap_" + file;
        final String file = "22_500.dat";
        final String path ="/users/21500078t/instances/tap_" + file;

        double temps = 0.8, dist = 0.5;
        boolean modeFull = true;

        InstanceLegacy ist = InstanceLegacy.readFile(path);
        System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);
        double eptime = Math.round(temps * ist.size * 27.5f);

        long startTime = System.nanoTime();
        // 1 solve affectation
        List<List<Integer>> subtours = solveAffectation(ist.distances);
        System.out.println(subtours.size());
        System.out.println(subtours.stream().map(List::size).collect(Collectors.toList()));

        List<List<Integer>> selected = new ArrayList<>();
        if (modeFull) {
            selected.addAll(subtours);
        }
        else {
            // 2.1.1 solve Md-KS to find a collection of subtours
            boolean[] KSSolution = MDKnapsack.solve2DNaive(subtours.stream().mapToDouble(st -> subtourValue(st, ist)).toArray(),
                    subtours.stream().mapToDouble(st -> subtourTime(st, ist)).toArray(), eptime,
                    subtours.stream().mapToDouble(st -> subtourDistance(st, ist)).toArray(), epdist);
            for (int i = 0; i < KSSolution.length; i++) {
                if (KSSolution[i])
                    selected.add(subtours.get(i));
            }
        }

        // 2.1.2 stitch subtours
        List<Integer> full = stitch(selected, ist);
        System.out.println("Checking subtour stiching ... " + full.size() + "/" + selected.stream().mapToInt(List::size).sum());

        // 2.1.3 check constraint (distance)
        System.out.println("Objective: "+ subtourValue(full, ist));
        System.out.println("Time constraint: "+ subtourTime(full, ist)  + "/" + eptime);
        System.out.println("Distance constraint: "+ (subtourDistance(full, ist) - maxEdgeValue(full, ist)) + "/" + epdist);
        boolean dis_check = subtourDistance(full, ist) > epdist + maxEdgeValue(full, ist) || subtourTime(full, ist) > eptime;

        // 2.1.4
        if (dis_check){
            System.out.println("  --> constraint violated running elimination");
            /*
            while (dis_check){
                List<Element> gains = new ArrayList<>();
                for (int i = 0; i < full.size(); i++) {
                    gains.add(new Element(i, ist.interest[full.get(i)]));
                }
                gains.sort(Comparator.comparing(Element::getValue));//.reversed());
                full.remove(gains.get(0).index);
                dis_check = subtourDistance(full, ist) > epdist + maxEdge(full, ist) || subtourTime(full, ist) > eptime;
            }*/

            //Switch to sequence for this
            int posme = argMaxEdge(full, ist);
            if (posme != 0)
                full = getAligned(full, posme);

            Reducer rd = new Reducer(ist, full);
            rd.setLb(235);
            List<Integer> toRemove = rd.toRemove(subtourTime(full, ist) - eptime, sequenceDistance(full, ist) - epdist);
            System.out.println(toRemove);
            full.removeAll(toRemove.stream().filter(i -> i >= 0).map(full::get).collect(Collectors.toList()));

            System.out.println("  Objective: "+ subtourValue(full, ist));
            System.out.println("  Time constraint: "+ subtourTime(full, ist)  + "/" + eptime);
            System.out.println("  Distance constraint: "+ (subtourDistance(full, ist) - maxEdgeValue(full, ist)) + "/" + epdist);


        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;
        System.out.println("RUNTIME = " + duration / 1000.0 + " s");

    }



    public static List<Integer> stitch(List<List<Integer>> tours, InstanceLegacy ist){

        Graph<Integer, StitchOP> g = new SimpleGraph<>(StitchOP.class);
        IntStream.rangeClosed(0, tours.size()).forEach(g::addVertex);
        for (int i = 0; i < tours.size(); i++) {
            for (int j = i+1; j < tours.size(); j++) {
                g.addEdge(i, j, getApproximateStitch(tours.get(i), tours.get(j), ist));
            }
        }

        KruskalMinimumSpanningTree<Integer, StitchOP> mst = new KruskalMinimumSpanningTree<>(g);
        SpanningTreeAlgorithm.SpanningTree<StitchOP> tree = mst.getSpanningTree();

        List<Integer> full = new ArrayList<>();
        HashSet<List<Integer>> done = new HashSet<>();
        for (StitchOP op : tree){
            if (full.size() == 0){
                full = execute(op);
                done.add(op.aptr);
                done.add(op.bptr);
                continue;
            }

            if (done.contains(op.aptr) && ! done.contains(op.bptr)){
                full = execute(getApproximateStitch(full, op.bptr, ist));
                done.add(op.bptr);
                continue;
            }

            if (! done.contains(op.aptr) && done.contains(op.bptr)){
                full = execute(getApproximateStitch(full, op.aptr, ist));
                done.add(op.aptr);
                continue;
            }

        }

        return full;
    }

    public static List<Integer> execute(StitchOP op){
        List<Integer> alignedA = getAligned(op.aptr, op.aptr.indexOf(op.vertex1a));
        List<Integer> alignedB = getAligned(op.bptr, op.bptr.indexOf(op.vertex1b));
        alignedA.addAll(alignedB);
        return alignedA;
    }

    private static List<Integer> getAligned(List<Integer> original, int start){
        List<Integer> alignedA = new ArrayList<>();
        int cnt = original.size();
        int ptr = start;
        while (cnt != 0){
            alignedA.add(original.get(ptr));
            cnt--;
            if (ptr == original.size()-1)
                ptr = 0;
            else
                ptr++;
        }
        return alignedA;
    }

    // alternating algorithm from https://vlsicad.ucsd.edu/Publications/Journals/j67.pdf#page=11&zoom=100,0,422
    public static StitchOP getApproximateStitch(List<Integer> a, List<Integer> b, InstanceLegacy ist){
        HashSet<StitchOP> history = new HashSet<>();
        StitchOP current = new StitchOP();
        current.vertex1a = a.get(0);
        current.vertex2a = a.get(1);
        current.vertex1b = b.get(0);
        current.vertex2b = b.get(1);
        current.aptr = a;
        current.bptr = b;

        for (int iter = 0; !(history.contains(current)); iter++) {
            ArrayList<StitchOP> candidates;
            if (iter % 2 == 0){
                candidates = new ArrayList<>(b.size() * 2);
                for (int i = 0; i < b.size()-1; i++) {
                    StitchOP candidate = new StitchOP(current);
                    candidate.gains = current.cost - (- ist.distances[b.get(i)][b.get(i+1)] - ist.distances[current.vertex1a][current.vertex2a]
                            + ist.distances[b.get(i+1)][current.vertex2a] + ist.distances[b.get(i)][current.vertex1a]);
                    candidate.vertex1b = b.get(i);
                    candidate.vertex2b = b.get(i+1);
                    candidates.add(candidate);
                    candidate = new StitchOP(current);
                    candidate.gains = current.cost - (- ist.distances[b.get(i)][b.get(i+1)] - ist.distances[current.vertex1a][current.vertex2a]
                            + ist.distances[b.get(i)][current.vertex2a] + ist.distances[b.get(i+1)][current.vertex1a]);
                    candidate.vertex2b = b.get(i);
                    candidate.vertex1b = b.get(i+1);
                    candidates.add(candidate);
                }
            } else {
                candidates = new ArrayList<>(a.size() * 2);
                for (int i = 0; i < a.size()-1; i++) {
                    StitchOP candidate = new StitchOP(current);
                    candidate.gains = current.cost - (- ist.distances[current.vertex1b][current.vertex2b] - ist.distances[a.get(i)][a.get(i+1)]
                            + ist.distances[current.vertex2b][a.get(i+1)] + ist.distances[current.vertex1b][a.get(i)]);
                    candidate.vertex1a = a.get(i);
                    candidate.vertex2a = a.get(i+1);
                    candidates.add(candidate);
                    candidate = new StitchOP(current);
                    candidate.gains = current.cost - (- ist.distances[current.vertex1b][current.vertex2b] - ist.distances[a.get(i)][a.get(i+1)]
                            + ist.distances[current.vertex2b][a.get(i)] + ist.distances[current.vertex1b][a.get(i+1)]);
                    candidate.vertex2a = a.get(i);
                    candidate.vertex1a = a.get(i+1);
                    candidates.add(candidate);

                }
            }
            candidates.removeIf(stitchOP -> stitchOP.gains < 0);

            if (candidates.size() == 0) {
                current.cost = - ist.distances[current.vertex1b][current.vertex2b] - ist.distances[current.vertex1a][current.vertex2a]
                        + ist.distances[current.vertex2b][current.vertex2a] + ist.distances[current.vertex1b][current.vertex1a];
                return current;
            }

            candidates.sort(Comparator.comparing(StitchOP::getGains).reversed());
            current = candidates.get(0);
            current.cost = - ist.distances[current.vertex1b][current.vertex2b] - ist.distances[current.vertex1a][current.vertex2a]
                    + ist.distances[current.vertex2b][current.vertex2a] + ist.distances[current.vertex1b][current.vertex1a];


            if (iter > 5)
                history.add(current);
        }

        return current;
    }


    public static double subtourValue(List<Integer> tour, InstanceLegacy ist){
        return tour.stream().mapToDouble(i -> ist.interest[i]).sum();
    }

    public static double subtourTime(List<Integer> tour, InstanceLegacy ist){
        return tour.stream().mapToDouble(i -> ist.costs[i]).sum();
    }

    public static double subtourDistance(List<Integer> tour, InstanceLegacy ist){
        return ist.distances[tour.get(tour.size() - 1)][tour.get(0)] + sequenceDistance(tour, ist);
    }

    public static double sequenceDistance(List<Integer> tour, InstanceLegacy ist){
        double d = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            d += ist.distances[tour.get(i)][tour.get(i+1)];
        }
        return d;
    }

    public static double maxEdgeValue(List<Integer> tour, InstanceLegacy ist){
        double max = ist.distances[tour.get(tour.size() - 1)][tour.get(0)];
        for (int i = 0; i < tour.size() - 1; i++) {
            double d = ist.distances[tour.get(i)][tour.get(i+1)];
            if (d > max)
                max = d;
        }
        return max;
    }

    public static int argMaxEdge(List<Integer> tour, InstanceLegacy ist){
        double max = ist.distances[tour.get(tour.size() - 1)][tour.get(0)];
        int right = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            double d = ist.distances[tour.get(i)][tour.get(i+1)];
            if (d > max) {
                max = d;
                right = i+1;
            }
        }
        return right;
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

        //System.out.println(subtours);
        return subtours;
    }

    static class StitchOP  extends DefaultEdge implements Comparable<StitchOP>{
        @Getter
        double cost, gains;
        int vertex1a, vertex1b;
        int vertex2a, vertex2b;
        List<Integer> aptr, bptr;

        public StitchOP() {
            cost = 0;
        }

        public StitchOP(StitchOP old) {
            this.cost = old.cost;
            this.vertex1a = old.vertex1a;
            this.vertex2a = old.vertex2a;
            this.vertex1b = old.vertex1b;
            this.vertex2b = old.vertex2b;
            this.aptr = old.aptr;
            this.bptr = old.bptr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StitchOP stitchOP = (StitchOP) o;
            return vertex1a == stitchOP.vertex1a && vertex1b == stitchOP.vertex1b && vertex2a == stitchOP.vertex2a && vertex2b == stitchOP.vertex2b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex1a, vertex1b, vertex2a, vertex2b);
        }

        @Override
        public int compareTo(StitchOP o) {
            return Double.compare(this.cost, o.cost);
        }

        @Override
        public String toString() {
            return "StitchOP{" +"cost=" + cost + ", (" + vertex1a + "," + vertex2a + "), (" + vertex1b +"," + vertex2b  + ")}";
        }
    }

}
