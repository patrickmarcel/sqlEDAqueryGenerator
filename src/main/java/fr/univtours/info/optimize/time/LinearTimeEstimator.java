package fr.univtours.info.optimize.time;


import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.AssessQuery;

import java.io.IOException;
import java.sql.SQLException;

public class LinearTimeEstimator implements CostModel {

    static double alpha;
    static double intercept = 0.01;

    static DatasetStats stats = null;

    public static long estimate(AssessQuery q){
        if (stats == null) {
            try {
                //FIXME
                stats = new DatasetStats(DBConfig.newFromFile());
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
        if (operation instanceof AssessQuery){
            return estimate((AssessQuery) operation);
        } else
            throw new UnsupportedOperationException();
    }
}
