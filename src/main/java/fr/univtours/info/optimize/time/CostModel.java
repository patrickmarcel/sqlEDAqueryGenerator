package fr.univtours.info.optimize.time;

/**
 * Any time estimator for queries or algorithm must implement this
 */
public interface CostModel {
    /**
     * Provide a cost estimate for a given operation in milliseconds
     * @param operation any TimeableOperation
     * @return it's estimated cost in ms
     */
    public long estimateCost(TimeableOp operation);
}
