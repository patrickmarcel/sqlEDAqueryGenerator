package fr.univtours.info.optimize;

import fr.univtours.info.queries.Query;

import java.util.List;

public interface TAPEngine {
    public List<Query> solve(List<Query> theQ, int timeBudget, int maxDistance);
}
