package fr.univtours.info.optimize;

import fr.univtours.info.queries.AbstractEDAsqlQuery;
import fr.univtours.info.queries.CandidateQuerySet;

import java.util.List;

public interface TAPEngine {
    public List<AbstractEDAsqlQuery> solve(List<AbstractEDAsqlQuery> theQ);
}
