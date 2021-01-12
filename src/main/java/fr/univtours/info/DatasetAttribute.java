package fr.univtours.info;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public abstract class DatasetAttribute {
    Set activeDomain;
    String name;
    Connection conn;
    String table;

    public abstract void setActiveDomain() throws SQLException;



    public DatasetAttribute(String name, Connection conn, String table){
        this.conn=conn;
        this.name=name;
        this.table=table;
    }

}
