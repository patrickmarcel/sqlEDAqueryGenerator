package fr.univtours.info.metadata;


import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public abstract class DatasetAttribute {

    @Getter
    String name;
    Connection conn;
    String table;

    public abstract void computeActiveDomain() throws SQLException;

    public DatasetAttribute(String name, Connection conn, String table){
        this.conn=conn;
        this.name=name;
        this.table=table;
    }

}
