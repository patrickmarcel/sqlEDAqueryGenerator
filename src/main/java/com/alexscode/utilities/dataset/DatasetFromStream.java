package com.alexscode.utilities.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatasetFromStream<E> implements Dataset<E> {
    private List<E> bufferedList;
    private Stream<E> stream;

    public DatasetFromStream(Stream<E> stream) {
        this.stream = stream;
        this.bufferedList = stream.collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<E> getAsList() {
        return bufferedList;
    }

    @Override
    public void close() {
        stream.close();
    }
}
