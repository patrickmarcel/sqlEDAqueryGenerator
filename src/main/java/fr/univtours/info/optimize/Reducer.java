package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import lombok.experimental.Accessors;
import java.util.*;

public class Reducer {
    Instance ist;
    List <Integer> sol;
    ArrayList<Table<Double, Double, List<Integer>>> cache;
    @Accessors
    double lower_bound = 0;
    double upper_bound = 0;
    int hit;

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
        Pair<List<Integer>, Double> r = remove(1, deltaT, deltaD);
        return r.left;
    }

    private Pair<List<Integer>, Double> remove(int from, double deltaT, double deltaD){

        if (deltaD <= 0 && deltaT <= 0) {
            return new Pair<>(new ArrayList<>(), 0.);
        }
        if (from + 3 >= sol.size() && (deltaD > 0 || deltaT > 0))
            return new Pair<>(new ArrayList<>(), Double.MAX_VALUE);

        Pair<List<Integer>, Double> next = remove(from + 1, deltaT, deltaD);
        Pair<List<Integer>, Double> nextnext = remove(from + 2,
                deltaT - ist.costs[sol.get(from)],
                deltaD + ist.distances[sol.get(from-1)][sol.get(from+1)] - ist.distances[sol.get(from-1)][sol.get(from)] - ist.distances[sol.get(from)][sol.get(from+1)] );

        if (next.right < nextnext.right){
            return new Pair<>(next.left, next.right);
        } else {
            List<Integer> path = new ArrayList<>(nextnext.left);
            path.add(from);
            return new Pair<>(path, nextnext.right + ist.interest[sol.get(from)]);
        }

    }


}
