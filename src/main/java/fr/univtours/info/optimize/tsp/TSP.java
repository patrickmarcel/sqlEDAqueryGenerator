package fr.univtours.info.optimize.tsp;

import fr.univtours.info.queries.AssessQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TSP {
    public static List<AssessQuery> orderByTSP(Collection<AssessQuery> input){
        //Ordering phase
        List<AssessQuery> toOrder = new ArrayList<>(input);
        List<Integer> ids = IntStream.range(0, toOrder.size()).boxed().collect(Collectors.toList());

        LinKernighan tsp = new LinKernighan(toOrder.stream().map(q -> (Measurable) q).collect(Collectors.toList()), ids);
        tsp.runAlgorithm();

        return Arrays.stream(tsp.tour).mapToObj(toOrder::get).collect(Collectors.toList());
    }
}
