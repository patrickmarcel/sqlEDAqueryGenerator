package fr.univtours.info.queries;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;

import java.sql.SQLException;
import java.util.Set;

@Deprecated
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
