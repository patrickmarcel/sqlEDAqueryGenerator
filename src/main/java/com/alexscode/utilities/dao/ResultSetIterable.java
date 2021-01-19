package com.alexscode.utilities.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResultSetIterable<T> implements Iterable<T>,AutoCloseable {

    private final ResultSet rs;
    private final Function<ResultSet, T> onNext;
    private final Statement statement;

    public ResultSetIterable(ResultSet rs, Function<ResultSet, T> onNext, Statement statement){
        this.rs = rs;
        this.onNext = onNext;
        this.statement = statement;
    }


    @Override
    public Iterator<T> iterator() {

        try {
            return new Iterator<T>() {

                boolean hasNext = rs.next();

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public T next() {

                    T result = onNext.apply(rs);
                    try {
                        hasNext = rs.next();
                        if (! hasNext){
                            statement.close();
                        }
                    } catch (SQLException e) {
                        System.err.println("Failed to produce next object for Stream (SQL error)!");
                        e.printStackTrace();
                    }
                    return result;
                }
            };
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    @Override
    public void close() throws Exception {
        this.statement.close();
    }
}
