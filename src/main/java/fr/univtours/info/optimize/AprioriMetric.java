package fr.univtours.info.optimize;

import fr.univtours.info.queries.AssessQuery;
@Deprecated
public interface AprioriMetric {

    public double rate(AssessQuery q);
}
