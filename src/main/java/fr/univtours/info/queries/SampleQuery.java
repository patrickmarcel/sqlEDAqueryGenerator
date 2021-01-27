package fr.univtours.info.queries;

import fr.univtours.info.Dataset;
import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Query object dedicated to be run on a sample table
 */
public class SampleQuery extends AbstractEDAsqlQuery{
    private final AbstractEDAsqlQuery original;
    private final Dataset sampleTable;


    public SampleQuery(AbstractEDAsqlQuery original, Dataset sampleTable){
        this.original = original;
        this.sampleTable = sampleTable;
        this.conn = sampleTable.getConn();
    }

    @Override
    protected String getSqlInt() {
        String originalTable = original.table;
        original.table = sampleTable.getTable();
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
    public void interestFromResult() throws Exception {
        long startTime = System.nanoTime();
        execute();
        ResultSet orRs = original.resultset;
        original.resultset = this.resultset;
        original.interestFromResult();
        this.interest = original.interest;
        original.resultset = orRs;
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        System.out.println(duration/1000000);
    }


}
