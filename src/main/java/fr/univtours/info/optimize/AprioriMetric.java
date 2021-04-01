package fr.univtours.info.optimize;

import fr.univtours.info.queries.AbstractEDAsqlQuery;
@Deprecated
public interface AprioriMetric {

    public double rate(AbstractEDAsqlQuery q);
}
