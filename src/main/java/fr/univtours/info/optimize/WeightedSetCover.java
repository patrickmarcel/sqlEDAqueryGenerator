package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;
import com.alexscode.utilities.collection.PQElement;
import com.alexscode.utilities.collection.UpdateablePriorityQueue;
import fr.univtours.info.dataset.metadata.DatasetDimension;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedSetCover {
    static final double MAXPRIORITY = 10e9;

    public static void main(String[] args) {
        //sets
        List<Set<Integer>> S = new ArrayList<>(List.of(Set.of(1, 2, 3), Set.of(3, 6, 7, 10), Set.of(8), Set.of(9, 5), Set.of(4, 5, 6, 7, 8), Set.of(4, 5, 9, 10)));
        // weights
        List<Double> w = new ArrayList<>(List.of(1., 2., 3., 4., 3., 5.));
        System.out.println(S.size() == w.size());
        solve(S, w);


    }

    public static <T> List<Set<T>> solve(List<Set<T>> S, List<Double> w){
        Map<T, Set<Integer>> udict = new HashMap<>();
        List<Integer> selected = new ArrayList<>();
        List<Set<T>> scopy = new ArrayList<>(); // During the process, S will be modified. Make a copy for S.
        for (int i = 0; i < S.size(); i++) {
            Set<T> item = S.get(i);
            scopy.add(new HashSet<>(item));
            for (T j : item){
                if (!udict.containsKey(j))
                    udict.put(j, new TreeSet<>());
                udict.get(j).add(i);
            }

        }

        var pq = new UpdateablePriorityQueue<PQElement>();
        var coverednum = 0;
        for (int i = 0; i < scopy.size(); i++) {
            var item = scopy.get(i);
            if (item.size() == 0)
                pq.add(new PQElement(i, MAXPRIORITY));
            else
                pq.add(new PQElement(i, w.get(i)/item.size()));//(index, float(w[index]) / len(item))
        }

        while (coverednum < udict.size()){
            var a = pq.poll(); // get the most cost-effective set
            selected.add(a.index); // a: set id
            coverednum += scopy.get(a.index).size();
            // Update the sets that contains the new covered elements
            for (var m : scopy.get(a.index)) {
                for (var n : udict.get(m)) {  // n: set id
                    if (!n.equals(a.index)){
                        scopy.get(n).remove(m);
                        if (scopy.get(n).size() == 0)
                            pq.add(new PQElement(n, MAXPRIORITY));
                        else
                            pq.add(new PQElement(n, w.get(n)/scopy.get(n).size()));
                    }
                }
            }
            scopy.get(a.index).clear();
            pq.add(new PQElement(a.index, MAXPRIORITY));

        }

        return selected.stream().map(S::get).collect(Collectors.toList());
    }

}
