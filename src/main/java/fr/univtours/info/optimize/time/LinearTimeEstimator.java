package fr.univtours.info.optimize.time;


import fr.univtours.info.queries.AbstractEDAsqlQuery;

public class LinearTimeEstimator implements CostModel {

    static double alpha;
    static double intercept = 0.01;

    public static long estimate(AbstractEDAsqlQuery q){
        return 0; //TODO
    }

    @Override
    public long estimateCost(TimeableOp operation) {
        if (operation instanceof AbstractEDAsqlQuery){
            return estimate((AbstractEDAsqlQuery) operation);
        } else
            throw new UnsupportedOperationException();
    }
}
