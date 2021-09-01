package fr.univtours.info.optimize;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Instance {
    int size;
    double[][] distances;
    double[] costs;
    double[] interest;

    public static Instance fromFilter(Instance original, boolean[] keep){
        int size = 0;
        for (boolean b : keep) {
            if (b) size++;
        }
        double[][] distances = new double[size][size];
        double[] costs = new double[size];
        double[] interest = new double[size];
        int ni = 0;
        for (int i = 0; i < original.size; i++) {
            if (keep[i]){
                costs[ni] = original.costs[i];
                interest[ni] = original.interest[i];
                int nj = 0;
                for (int j = 0; j < original.size; j++) {
                    if (keep[j]){
                        distances[ni][nj++] = original.distances[i][j];
                    }
                }
                ni++;
            }
        }

        return new Instance(size, distances, costs, interest);
    }
}
