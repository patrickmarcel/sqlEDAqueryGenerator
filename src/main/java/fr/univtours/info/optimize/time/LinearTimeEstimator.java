package fr.univtours.info.optimize.time;


import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.metadata.DatasetStats;
import fr.univtours.info.queries.AssessQuery;

import java.io.IOException;
import java.sql.SQLException;

@Deprecated
public class LinearTimeEstimator implements CostModel {

    static double alpha;
    static double intercept = 0.01;

    static DatasetStats stats = null;

    public static long estimate(AssessQuery q){
        if (stats == null) {

                //FIXME
                //stats = new DatasetStats(DBConfig.newFromFile());

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
