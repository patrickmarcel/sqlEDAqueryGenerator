package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.util.*;
import java.util.stream.Collectors;

public class Reducer {
    Instance ist;
    double[][] distances;
    double[] costs;
    double[] interest;
    int[] sol;
    @Setter
    double lb = 0;
    double ub = 0;
    ArrayList<Table<Double, Double, double[]>> cache = new ArrayList<>();

    public Reducer(Instance ist, List<Integer> sol) {
        this.ist = ist;
        distances = ist.distances;
        costs = ist.costs;
        interest = ist.interest;
        this.sol = sol.stream().mapToInt(Integer::intValue).toArray();
        for (int i : sol) {
            ub += interest[i];
            cache.add(TreeBasedTable.create());
        }
    }

    public List<Integer> toRemove(double deltaT, double deltaD){
        double[] r = remove_old(1, deltaT, deltaD, 0);
        return Arrays.stream(r).skip(1).mapToInt(d -> (int) d).boxed().collect(Collectors.toList());
    }

    private double[] remove(int from, double deltaT, double deltaD){

        if (deltaD <= 0 && deltaT <= 0)
            return new double[]{0.};
        if (from + 3 >= sol.length && (deltaD > 0 || deltaT > 0))
            return new double[]{Double.POSITIVE_INFINITY};

        double[] next = remove(from + 1, deltaT, deltaD);
        double[] nextnext = remove(from + 2,
                deltaT - costs[sol[from]],
                deltaD + distances[sol[from-1]][sol[from+1]] - distances[sol[from-1]][sol[from]] - distances[sol[from]][sol[from+1]]);

        if (next[0]< nextnext[0]) {
            if (lb > ub - next[0])
                return new double[]{Double.POSITIVE_INFINITY};
            return next;
        } else {
            double[] updated = new double[nextnext.length + 1];
            updated[0] = nextnext[0] + interest[sol[from]];
            if (lb > ub - updated[0])
                return new double[]{Double.POSITIVE_INFINITY};
            if (updated.length == 2){
                updated[1] = from;
            }else {
                System.arraycopy(nextnext, 1, updated, 1, nextnext.length-1);
                updated[nextnext.length] = from;
            }
            return updated;
        }
    }

    private double[] remove_old(int from, double deltaT, double deltaD, double deltaI){

        if (deltaD <= 0 && deltaT <= 0)
            return new double[]{0.};
        if (from + 3 >= sol.length && (deltaD > 0 || deltaT > 0))
            return new double[]{Double.POSITIVE_INFINITY};
        if (lb > ub - deltaI)
            return new double[]{Double.POSITIVE_INFINITY};

        double[] cached = cache.get(from).get(deltaT, deltaD);
        if (cached != null)
            return cached;

        double[] next = remove_old(from + 1, deltaT, deltaD, deltaI);
        double[] nextnext = remove_old(from + 2,
                deltaT - costs[sol[from]],
                deltaD + distances[sol[from-1]][sol[from+1]] - distances[sol[from-1]][sol[from]] - distances[sol[from]][sol[from+1]], deltaI + interest[sol[from]]);

        if (next[0]< nextnext[0]){
            cache.get(from).put(deltaT, deltaD, next);
            return next;
        } else {
            double[] updated = new double[nextnext.length + 1];
            updated[0] = nextnext[0] + interest[sol[from]];
            if (updated.length == 2){
                updated[1] = from;
            }else {
                System.arraycopy(nextnext, 1, updated, 1, nextnext.length-1);
                updated[nextnext.length] = from;
            }
            cache.get(from).put(deltaT, deltaD, updated);
            return updated;
        }

    }


}
