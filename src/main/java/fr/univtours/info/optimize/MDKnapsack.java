package fr.univtours.info.optimize;

import com.alexscode.utilities.collection.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MDKnapsack {
    public static boolean[] solve2DNaive(double[] values, double[] w1, double c1, double[] w2, double c2){
        int n = values.length;
        assert values.length == w1.length && w1.length == w2.length;
        boolean[] solution = new boolean[n];

        List<Element> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(new Element(i, values[i]/(w1[i]*0.5 + w2[i]*0.5)));
        }
        order.sort(Comparator.comparing(Element::getValue).reversed());

        double totalC1 = 0, totalC2 = 0;
        for (Element item : order){
            if (totalC1 + w1[item.index] > c1 || totalC2 + w2[item.index] > c2)
                continue;
            else {
                solution[item.index] = true;
                totalC1 += w1[item.index];
                totalC2 += w2[item.index];
            }
        }

        return solution;
    }
}
