package fr.univtours.info;

import java.util.Set;

public interface EDAsqlQuery {
    void execute() throws Exception;
    void explainAnalyze() throws Exception;
    void explain() throws Exception;
    float getActualCost();
    float getInterest();
    float getDistance(EDAsqlQuery other);

    void computeInterest() throws Exception;

    Set<DatasetDimension> getGroupby();
    DatasetMeasure getMeasure();
    String getFunction();
}
