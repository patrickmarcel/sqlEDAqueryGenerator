package fr.univtours.info.optimize;

import fr.univtours.info.queries.AssessQuery;

import java.util.*;

/**
 * This is the interface for the interestingness vs runtime part of the TAP heuristics
 */
@Deprecated
public interface BudgetManager {
    /**
     * Time budget is an integer in milliseconds (simpler to solve)
     * @param candidates candidates queries
     * @param timeBudget the budget in milliseconds
     * @return an execution plan compliant with the budget
     */
    public List<AssessQuery> findBestPlan(Collection<AssessQuery> candidates, int timeBudget);

}
