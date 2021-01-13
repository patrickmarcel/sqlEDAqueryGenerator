package fr.univtours.info;

import java.sql.SQLException;
import java.util.Set;

public interface EDAsqlQuery {
    void execute() throws Exception;
    void explainAnalyze() throws Exception;
    void explain() throws Exception;
    float getActualCost();
    float getEstimatedCost();
    double getInterest();
    float getDistance(EDAsqlQuery other);




    void computeInterest() throws Exception;
    void setEstimatedCost(float estimation);

    String getSql();
    DatasetDimension getAssessed();
    DatasetDimension getReference();
    Set<DatasetDimension> getDimensions();
    DatasetMeasure getMeasure();
    String getFunction();

    void print();
    void printResult() throws SQLException;
}
