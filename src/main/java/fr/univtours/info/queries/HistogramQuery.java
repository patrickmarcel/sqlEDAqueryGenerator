package fr.univtours.info.queries;

import fr.univtours.info.dataset.metadata.DatasetDimension;

import java.sql.Connection;
import java.util.HashSet;

public class HistogramQuery extends AbstractEDAsqlQuery {

    private final DatasetDimension attrib;

    public HistogramQuery(Connection conn, String table, DatasetDimension attribute){
        this.conn=conn;
        this.table=table;

        this.dimensions =new HashSet<>();
        this.attrib = attribute;
        this.dimensions.add(attribute);
        this.measure=null;
        this.function="count";


    }


    @Override
    protected String getSqlInt() {
        return "select " + attrib.getName() + ",count(*) from " + table +" group by " + attrib.getName()+ ";";
    }

    @Override
    public float getDistance(AbstractEDAsqlQuery other) {
        return 0;
    }

    @Override
    public void interestFromResult() throws Exception {

    }
}
