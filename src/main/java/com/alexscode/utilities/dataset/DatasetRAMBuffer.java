package com.alexscode.utilities.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatasetRAMBuffer<E> implements Dataset<E> {

    List<E> bufferedList;
    Dataset<E> internal_dataset;

    public DatasetRAMBuffer(Dataset<E> dataset) {
        internal_dataset = dataset;
        this.bufferedList = null;
    }

    private List<E> tryGetList(){
        if (bufferedList == null){
            bufferedList = internal_dataset.stream().collect(Collectors.toCollection(ArrayList::new));
        }
        return bufferedList;
    }

    @Override
    public E getRow(int index){
        return tryGetList().get(index);
    }

    @Override
    public List<E> getAsList() {
        return tryGetList();
    }

    @Override
    public Stream<E> stream(){
        if (bufferedList == null){
            bufferedList = internal_dataset.getAsList();
        }
        return bufferedList.stream();
    }

    public void invalidateBuffer(){
        bufferedList = null;
    }

    @Override
    public void close() throws Exception {
        internal_dataset.close();
    }
}
