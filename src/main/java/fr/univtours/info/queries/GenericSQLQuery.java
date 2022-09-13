package fr.univtours.info.queries;

import fr.univtours.info.dataset.Dataset;

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

}
