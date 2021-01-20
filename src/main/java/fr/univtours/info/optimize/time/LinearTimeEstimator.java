package fr.univtours.info.optimize.time;


import fr.univtours.info.metadata.DatasetStats;
import fr.univtours.info.queries.AbstractEDAsqlQuery;

import java.io.IOException;
import java.sql.SQLException;

public class LinearTimeEstimator implements CostModel {

    static double alpha;
    static double intercept = 0.01;

    static DatasetStats stats = null;

    public static long estimate(AbstractEDAsqlQuery q){
        if (stats == null) {
            try {
                stats = new DatasetStats();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
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
