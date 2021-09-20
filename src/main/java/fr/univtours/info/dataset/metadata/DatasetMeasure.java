package fr.univtours.info.dataset.metadata;


import java.sql.Connection;

public class DatasetMeasure extends DatasetAttribute {

    public DatasetMeasure(String name, Connection conn, String table){
        super(name,conn, table);
        prettyName = name;
    }

    public void computeActiveDomain(){}


    @Override
    public String toString() {
        return "M{" + prettyName +'}';
    }
}
