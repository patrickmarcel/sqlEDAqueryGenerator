package fr.univtours.info.dataset.metadata;

import java.sql.Connection;

public class DatasetMeasure extends DatasetAttribute {
    public DatasetMeasure(String name, Connection conn, String table){

        super(name,conn, table);
    }

    public void computeActiveDomain(){
    }

    public float getMin(){
        return 0;
    }
    public float getMax(){
        return 0;
    }

    public float getAvg(){
        return 0;
    }

    public float getSttdev(){
        return 0;
    }


    @Override
    public String toString() {
        return "M{" +
                "name='" + name + '\'' +
                '}';
    }
}
