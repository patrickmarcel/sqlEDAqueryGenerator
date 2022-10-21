package fr.univtours.info.queries;

import fr.univtours.info.dataset.Dataset;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Objects;

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

    @Override
    public String toString() {
        return getSqlInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericSQLQuery that = (GenericSQLQuery) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
