package fr.univtours.info;

public interface EDAsqlQuery {
    void execute() throws Exception;
    void explainAnalyze() throws Exception;
    float getCost();
    float getInterest();
    float getDistance(EDAsqlQuery other);
}
