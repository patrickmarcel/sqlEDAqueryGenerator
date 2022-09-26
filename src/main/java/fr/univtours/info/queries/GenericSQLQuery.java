package fr.univtours.info.queries;

import fr.univtours.info.dataset.Dataset;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class GenericSQLQuery extends Query{
    final private String text;

    public GenericSQLQuery(Dataset source, String sql) {
        this.source = source;
        text = sql;
    }

    @Override
    protected String getSqlInt() {
        return text;
    }

    @Override
    public double dist(Query other) {
        LevenshteinDistance ld = new LevenshteinDistance(10);
        return ld.apply(other.getSqlInt(), this.getSqlInt());

    }
}
