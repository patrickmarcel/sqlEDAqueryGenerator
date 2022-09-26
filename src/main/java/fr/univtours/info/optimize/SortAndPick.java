package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortAndPick implements TAPEngine{
    public static void main(String[] args) throws Exception {
        double temps = 0.25, dist = 0.35;

        var folder = "/home/alex/instances/";
        for (int i = 0; i < 30; i++) {
            for (int size : new int[]{20, 40, 60, 80, 100, 150, 200, 250, 300, 350, 400, 450, 500}) {
                var in = folder + "tap_" + i + "_" + size + ".dat";
                var out = folder + "tap_" + i + "_" + size + ".warm";
                compute(in, out, temps, dist);
            }
        }

    }

    private static void compute(String path, String out_path, double temps, double dist) throws IOException {
        InstanceLegacy ist = InstanceLegacy.readFile(path);
        //System.out.println("Loaded " + path + " | " + ist.size + " queries");
        double epdist = Math.round( dist * ist.size * 4.5);
        double eptime = Math.round(temps * ist.size * 27.5f);


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

            if (eptime - (total_time + ist.costs[current]) >= 0){
                if (solution.size() > 0 && epdist - (total_dist + ist.distances[solution.get(solution.size() - 1)][current]) < 0)
                    continue;
                if (solution.size() > 0)
                    total_dist += ist.distances[solution.get(solution.size() - 1)][current];
                total_time += ist.costs[current];
                solution.add(current);
                z += ist.interest[current];
            }
        }
        System.out.printf("%s;%s;%s;%s%n",path.substring(21).replace("_"+ist.size+".dat", ""),ist.size,z,solution.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    @Override
    public List<Query> solve(List<Query> theQ, int timeBudget, int maxDistance) {
        int size = theQ.size();

        List<Integer> solution = new ArrayList<>();
        Element[] order = new Element[size];
        for (int i = 0; i < size; i++) {
            order[i] = new Element(i, theQ.get(i).getInterest());
        }

        // Merge sort see javadoc
        Arrays.sort(order, Comparator.comparing(Element::getValue).reversed());


        double total_dist = 0;
        double total_time = 0;
        double z = 0;


        for (int i = 0; i < size; i++)
        {
            int current = order[i].index;

            if (timeBudget - (total_time + theQ.get(current).estimatedTime()) >= 0){
                if (solution.size() > 0 && maxDistance - (total_dist + theQ.get(solution.get(solution.size() - 1)).dist(theQ.get(current))) < 0)
                    continue;
                if (solution.size() > 0)
                    total_dist += theQ.get(solution.get(solution.size() - 1)).dist(theQ.get(current));
                total_time += theQ.get(current).estimatedTime();
                solution.add(current);
                z += theQ.get(current).getInterest();
            }
        }
        return solution.stream().map(theQ::get).collect(Collectors.toList());
    }
}
