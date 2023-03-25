package fr.univtours.info.dataset.metadata;


import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public abstract class DatasetAttribute implements JsonSerializer<DatasetAttribute> {

    @Getter
    String name;
    @Getter @Setter
    String prettyName;
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

    @Override
    public JsonElement serialize(DatasetAttribute datasetAttribute, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(name);
    }
}
