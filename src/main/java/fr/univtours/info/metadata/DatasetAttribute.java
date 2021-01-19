package fr.univtours.info.metadata;


import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetAttribute)) return false;
        DatasetAttribute that = (DatasetAttribute) o;
        return getName().equals(that.getName()) && table.equals(that.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), table);
    }
}
