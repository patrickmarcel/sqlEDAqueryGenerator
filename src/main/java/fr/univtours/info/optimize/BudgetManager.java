package fr.univtours.info.optimize;

import fr.univtours.info.optimize.time.TimeableOp;
import fr.univtours.info.queries.AbstractEDAsqlQuery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the interface for the interestingness vs runtime part of the TAP heuristics
 */
public interface BudgetManager {
    /**
     * Time budget is an integer in milliseconds (simpler to solve)
     * @param candidates candidates queries
     * @param timeBudget the budget in milliseconds
     * @return an execution plan compliant with the budget
     */
    public List<AbstractEDAsqlQuery> findBestPlan(Collection<AbstractEDAsqlQuery> candidates, int timeBudget);

}
