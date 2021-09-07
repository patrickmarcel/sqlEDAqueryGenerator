package fr.univtours.info.dataset;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Sets;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class TableFragment {
    final Object[] dimA, dimB;
    final double[] measure;
    final int len;

    public TableFragment(ResultSet resultSet, int tableLen) {
        dimA = new Object[tableLen];
        dimB = new Object[tableLen];
        measure = new double[tableLen];
        len = tableLen;

        try {
            int ptr = 0;
            while (resultSet.next()){
                dimA[ptr] = resultSet.getObject(1);
                dimB[ptr] = resultSet.getObject(2);
                measure[ptr++] = resultSet.getDouble(3);
            }
        } catch (SQLException | ArrayIndexOutOfBoundsException e){
            System.err.println("[Error] building table fragment failed");
        }
    }

    public Pair<double[], double[]> assess(Object val1, Object val2, Function<Collection<Double>, Double> agg){
        Map<Object, List<Double>> left = new HashMap<>();
        Map<Object, List<Double>> right = new HashMap<>();

        for (int i = 0; i < len; i++) {
            if (val1.equals(dimB[i])){
                left.computeIfAbsent(dimA[i], k -> new ArrayList<>());
                left.get(dimA[i]).add(measure[i]);
                continue; // mutually exclusive selection
            }
            if (val2.equals(dimB[i])){
                right.computeIfAbsent(dimA[i], k -> new ArrayList<>());
                right.get(dimA[i]).add(measure[i]);
            }
        }
        Set<Object> groups = Sets.intersection(left.keySet(), right.keySet());
        double l[] = new double[groups.size()];
        double r[] = new double[groups.size()];
        int ptr = 0;
        for (Object group : groups){
            l[ptr] = agg.apply(left.get(group));
            r[ptr] = agg.apply(right.get(group));
            ptr++;
        }

        return new Pair<>(l, r);
    }

    public static double sum(Collection<Double> x){
        //if (x == null)
        //    return 0;
        double s = 0;
        for(Double item : x)
            s += item;
        return s;
    }
}
