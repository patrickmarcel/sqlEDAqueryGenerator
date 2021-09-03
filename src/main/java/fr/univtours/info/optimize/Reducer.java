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


    public Reducer(Instance ist, List<Integer> sol) {
        this.ist = ist;
        distances = ist.distances;
        costs = ist.costs;
        interest = ist.interest;
        this.sol = sol.stream().mapToInt(Integer::intValue).toArray();
        for (int i : sol) {
            ub += interest[i];
        }
    }

    public List<Integer> toRemove(double deltaT, double deltaD){
        double[] r = remove(1, deltaT, deltaD, 0);
        return Arrays.stream(r).skip(1).mapToInt(d -> (int) d).boxed().collect(Collectors.toList());
    }

    private double[] remove(int from, double deltaT, double deltaD, double deltaI){

        if (deltaD <= 0 && deltaT <= 0)
            return new double[]{0.};
        if (from + 3 >= sol.length && (deltaD > 0 || deltaT > 0))
            return new double[]{Double.POSITIVE_INFINITY};
        if (lb > ub - deltaI)
            return new double[]{Double.POSITIVE_INFINITY};

        double[] next = remove(from + 1, deltaT, deltaD, deltaI);
        double[] nextnext = remove(from + 2,
                deltaT - costs[sol[from]],
                deltaD + distances[sol[from-1]][sol[from+1]] - distances[sol[from-1]][sol[from]] - distances[sol[from]][sol[from+1]], deltaI + interest[sol[from]]);

        if (next[0]< nextnext[0]){
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
            return updated;
        }

    }


}
