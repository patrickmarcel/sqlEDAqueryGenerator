package fr.univtours.info.optimize.time;


import fr.univtours.info.queries.AbstractEDAsqlQuery;

public class CostModelProvider {
    static CostModel defaultModel;

    public static CostModel getModelFor(TimeableOp op){
        if (op instanceof AbstractEDAsqlQuery){
            return new LinearTimeEstimator();
        }
        else {
            if (defaultModel == null){
                throw new IllegalStateException("No default cost model could be found ! Use supported operations or initialize default model.");
            }
            return defaultModel;
        }
    }

    public static CostModel getDefaultModel() {
        return defaultModel;
    }

    public static void setDefaultModel(CostModel defaultModel) {
        CostModelProvider.defaultModel = defaultModel;
    }
}
