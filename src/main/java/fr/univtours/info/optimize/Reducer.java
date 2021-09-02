package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Lists;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Reducer {
    Instance ist;
    List <Integer> sol;
    //Map<Triple<Integer, Double, Double>, Pair<List<Integer>, Double>> cache;

    public Reducer(Instance ist, List<Integer> sol) {
        this.ist = ist;
        this.sol = sol;
        //cache = new HashMap<>();
    }

    public List<Integer> toRemove(double deltaT, double deltaD){
        return remove(1, deltaT, deltaD).left;
    }

    private Pair<List<Integer>, Double> remove(int from, double deltaT, double deltaD){
        //Triple<Integer, Double, Double> key = new Triple<>(from, deltaT, deltaD);
        Pair<List<Integer>, Double> rec = null;//cache.get(key);

        if (rec == null) {
            if ((deltaD <= 0 && deltaT <= 0) || from + 2 >= sol.size() - 2)
                return new Pair<>(Lists.newArrayList(-1), 0.);

            rec = IntStream.rangeClosed(from + 2, sol.size() - 2)
                            .boxed()
                            .map(i -> remove(i, deltaT - ist.costs[sol.get(i)], deltaD + ist.distances[sol.get(i - 1)][sol.get(i + 1)] - ist.distances[sol.get(i - 1)][sol.get(i)] - ist.distances[sol.get(i)][sol.get(i + 1)]))
                            .min(Comparator.comparing(Pair::getRight)).get();
            //cache.put(key, rec);
        }

        List<Integer> path = new ArrayList<>(rec.left);
        path.add(from);
        return new Pair<>(path, ist.interest[sol.get(from)] + rec.right);
    }

    private static <T> Pair<T, Double> argMinPairs(List<Pair<T, Double>> pairs){
        int arg = 0;
        double min = pairs.get(0).right;
        for (int i = 0; i < pairs.size(); i++) {
            if (min > pairs.get(i).right){
                arg = i;
                min = pairs.get(i).right;
            }
        }
        return pairs.get(arg);
    }
}

/*
        if (from == sol.size() - 1){
            return new Pair<>(Lists.newArrayList(from), ist.interest[sol.get(from)]);
        }
        if (from == sol.size() - 2){
            if (ist.interest[sol.get(from)] < ist.interest[sol.get(from+1)])
                return new Pair<>(Lists.newArrayList(from), ist.interest[sol.get(from)]);
            else
                return  new Pair<>(Lists.newArrayList(from+1), ist.interest[sol.get(from+1)]);
        }*/