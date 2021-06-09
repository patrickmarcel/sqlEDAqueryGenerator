package fr.univtours.info.optimize;

import fr.univtours.info.queries.AssessQuery;

import java.util.List;

public interface TAPEngine {
    public List<AssessQuery> solve(List<AssessQuery> theQ, int timeBudget, int maxDistance);
}
