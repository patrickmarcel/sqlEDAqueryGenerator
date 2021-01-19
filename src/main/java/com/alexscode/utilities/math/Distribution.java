package com.alexscode.utilities.math;



import com.alexscode.utilities.collection.MultiSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Discrete probability distribution over a Set X
 * @param <T> the type of x € X
 */
public class Distribution<T> {
    public static final double log2 = Math.log(2.0);
    public static final double oneOverRootTwo = (1.0/Math.sqrt(2.0));

    private HashMap<T, Double> map;

    public Distribution() {
        map = new HashMap<>();
    }

    public Distribution(MultiSet<T> from){
        map = new HashMap<>();
        for(T el: from){
            map.put(el, from.count(el)/(double)from.size());
        }
    }

    public boolean isProba(Double epsilon){
        return Math.abs(1 - sumOver()) <= epsilon;
    }

    public boolean isProba(){
        return isProba(10e-20);
    }

    private double sumOver(){
        return map.entrySet().stream().mapToDouble(Map.Entry::getValue).sum();
    }

    public void setProba(T item, double probability){
        map.put(item, probability);
    }

    public double getProba(T item){
        return map.getOrDefault(item, 0.0);
    }

    public Set<T> getSet(){
        return map.keySet();
    }

    public static <E> double hellinger(Distribution<E> p, Distribution <E> q){
        Set<E> universe = new HashSet<>(p.map.keySet());
        universe.addAll(q.map.keySet());
        double sum = 0;
        for (E e: universe){
            sum += Math.pow(Math.sqrt(p.getProba(e)) - Math.sqrt(q.getProba(e)), 2);
        }
        return oneOverRootTwo*Math.sqrt(sum);
    }

    public static <E> double jensenShannon(Distribution<E> p, Distribution<E> q){
        Distribution<E> m = average(p, q);
        return 0.5*kullbackLeibler(p, m) + 0.5*kullbackLeibler(q, m);
    }

    public static <E> double kullbackLeibler(Distribution<E> p, Distribution<E> q){
        Set<E> universe = new HashSet<>(p.map.keySet());
        universe.addAll(q.map.keySet());
        double sum = 0;
        for (E e : universe){
            if (q.getProba(e) == 0 && p.getProba(e) != 0) {
                //System.out.println(e);
                continue;
                //throw new IllegalArgumentException("Absolute continuity is required ! If q(i) = 0 then p(i) must be 0.");
            }
            sum += p.getProba(e)*log2(p.getProba(e)/q.getProba(e));
        }
        return sum;
    }

    public static <E> Distribution<E> average(Distribution<E> a, Distribution<E> b){
        Set<E> universe = new HashSet<>(a.map.keySet());
        universe.addAll(b.map.keySet());

        Distribution<E> avg = new Distribution<>();
        for (E e : universe){
            avg.setProba(e, 0.5*(a.getProba(e)+b.getProba(e)));
        }

        return avg;
    }

    public static double log2(double a) {
        return a == 0.0D ? 0.0D : Math.log(a) / log2;
    }

}
