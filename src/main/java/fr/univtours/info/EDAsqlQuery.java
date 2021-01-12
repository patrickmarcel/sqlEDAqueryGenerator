package fr.univtours.info;

import java.sql.SQLException;
import java.util.Set;

public interface EDAsqlQuery {
    void execute() throws Exception;
    void explainAnalyze() throws Exception;
    void explain() throws Exception;
    float getActualCost();
    double getInterest();
    float getDistance(EDAsqlQuery other);

    void computeInterest() throws Exception;

    Set<DatasetDimension> getDimensions();
    DatasetMeasure getMeasure();
    String getFunction();

    void print();
    void printResult() throws SQLException;
}
