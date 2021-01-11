package fr.univtours.info;

import java.util.Set;

public interface EDAsqlQuery {
    void execute() throws Exception;
    void explainAnalyze() throws Exception;
    float getCost();
    float getInterest();
    float getDistance(EDAsqlQuery other);

    Set<DatasetDimension> getGroupby();
    DatasetMeasure getMeasure();
    String getFunction();
}
