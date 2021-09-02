package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import lombok.experimental.Accessors;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Reducer {
    Instance ist;
    List <Integer> sol;
    ArrayList<Table<Double, Double, Pair<List<Integer>, Double>>> cache;
    @Accessors
    double lower_bound = 0;
    double upper_bound = 0;

    public Reducer(Instance ist, List<Integer> sol) {
        this.ist = ist;
        this.sol = sol;
        cache = new ArrayList<>(ist.size);
        for (int i = 0; i < ist.size; i++) {
            cache.add(TreeBasedTable.create());
            upper_bound += ist.interest[i];
        }
    }



    public List<Integer> toRemove(double deltaT, double deltaD){
        return remove2(1, deltaT, deltaD);
    }

    private List<Integer> remove2(int from, double deltaT, double deltaD){

        if (deltaD <= 0 && deltaT <= 0)
            return new ArrayList<>();

        double minCut = upper_bound;
        List<Integer> cutList = new ArrayList<>();
        for (int i = from + 2; i < sol.size(); i++) {
            List<Integer> tmp;
            if (i == sol.size()-1)
                tmp = remove2(i, deltaT - ist.costs[sol.get(i)], deltaD - ist.distances[sol.get(i - 1)][sol.get(i)]);
            else
             tmp = remove2(i, deltaT - ist.costs[sol.get(i)], deltaD + ist.distances[sol.get(i - 1)][sol.get(i + 1)] - ist.distances[sol.get(i - 1)][sol.get(i)] - ist.distances[sol.get(i)][sol.get(i + 1)]);
            double cutCost = 0;
            for (int j : tmp){
                cutCost += ist.interest[sol.get(j)];
            }
            if (cutCost <= minCut){
                minCut = cutCost;
                cutList = new ArrayList<>(tmp);
            }
            if (cutCost == 0){
                break;
            }
        }

        cutList.add(from);
        /*double interest_cut = ist.interest[sol.get(from)] + minCut;
        if (upper_bound-interest_cut<lower_bound){
            System.out.println("lb");
        }*/
        return cutList;
    }

    private Pair<List<Integer>, Double> remove(int from, double deltaT, double deltaD){
        Pair<List<Integer>, Double> rec = cache.get(from).get(deltaT, deltaD);

        if (rec == null) {
            if ((deltaD <= 0 && deltaT <= 0) || from + 2 >= sol.size() - 2)
                return new Pair<>(Lists.newArrayList(-1), 0.);

            rec = IntStream.rangeClosed(from + 2, sol.size() - 2)
                            .boxed()
                            .map(i -> remove(i, deltaT - ist.costs[sol.get(i)], deltaD + ist.distances[sol.get(i - 1)][sol.get(i + 1)] - ist.distances[sol.get(i - 1)][sol.get(i)] - ist.distances[sol.get(i)][sol.get(i + 1)]))
                            .min(Comparator.comparing(Pair::getRight)).get();
            cache.get(from).put(deltaT, deltaD, rec);
        }

        List<Integer> path = new ArrayList<>(rec.left);
        path.add(from);
        double interest_cut = ist.interest[sol.get(from)] + rec.right;
        if (upper_bound-interest_cut<lower_bound){
            System.out.println("lb");
        }
        return new Pair<>(path, interest_cut);
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