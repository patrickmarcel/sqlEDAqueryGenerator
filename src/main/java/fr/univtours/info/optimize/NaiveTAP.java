package fr.univtours.info.optimize;

import fr.univtours.info.optimize.tsp.TSP;
import fr.univtours.info.queries.AbstractEDAsqlQuery;
import fr.univtours.info.queries.CandidateQuerySet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NaiveTAP implements TAPEngine{
    @Override
    public List<AbstractEDAsqlQuery> solve(List<AbstractEDAsqlQuery> theQ, int timeBudget, int maxDistance) {

        theQ.sort(Comparator.comparingDouble(AbstractEDAsqlQuery::getInterest).reversed());
        theQ = theQ.subList(0, 20);
        theQ = TSP.orderByTSP(theQ);
        return theQ;
    }
}
