package fr.univtours.info.optimize;

import fr.univtours.info.optimize.tsp.TSP;
import fr.univtours.info.queries.AssessQuery;

import java.util.Comparator;
import java.util.List;

public class NaiveTAP implements TAPEngine{
    @Override
    public List<AssessQuery> solve(List<AssessQuery> theQ, int timeBudget, int maxDistance) {

        theQ.sort(Comparator.comparingDouble(AssessQuery::getInterest).reversed());
        theQ = theQ.subList(0, 20);
        theQ = TSP.orderByTSP(theQ);
        return theQ;
    }
}
