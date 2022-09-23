package fr.univtours.info.optimize;

import fr.univtours.info.optimize.tsp.TSP;
import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.Query;

import java.util.Comparator;
import java.util.List;

public class NaiveTAP implements TAPEngine{
    @Override
    public List<Query> solve(List<Query> theQ, int timeBudget, int maxDistance) {

        theQ.sort(Comparator.comparingDouble(Query::getInterest).reversed());
        theQ = theQ.subList(0, 20);
        theQ = TSP.orderByTSP(theQ);
        return theQ;
    }
}
