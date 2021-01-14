package fr.univtours.info.queries;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;

import java.sql.SQLException;
import java.util.Set;

/**
 * Query object dedicated to be run on a sample table
 */
public class SampleQuery extends AbstractEDAsqlQuery{
    private final AbstractEDAsqlQuery original;
    private final String sampleTable;


    public SampleQuery(AbstractEDAsqlQuery original, String sampleTable){
        this.original = original;
        this.sampleTable = sampleTable;
        this.conn = original.conn;
    }

    @Override
    protected String getSqlInt() {
        String originalTable = original.table;
        original.table = sampleTable;
        String sql = original.getSqlInt();
        original.table = originalTable;
        return sql;
    }

    @Override
    public DatasetMeasure getMeasure() {
        return original.getMeasure();
    }

    @Override
    public String getFunction() {
        return original.getFunction();
    }

    @Override
    public Set<DatasetDimension> getDimensions() {
        return original.getDimensions();
    }


    @Override
    public DatasetDimension getAssessed() {
        return original.getAssessed();
    }

    @Override
    public DatasetDimension getReference() {
        return original.getReference();
    }

    @Override
    public float getDistance(AbstractEDAsqlQuery other) {
        return original.getDistance(other);
    }

    @Override
    public void interestWithZscore() throws Exception {
        execute();

    }


}
