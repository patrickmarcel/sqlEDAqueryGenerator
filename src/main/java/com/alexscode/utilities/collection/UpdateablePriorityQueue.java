package com.alexscode.utilities.collection;

import java.util.PriorityQueue;

public class UpdateablePriorityQueue<T> extends PriorityQueue<T> {
    @Override
    public boolean add(T t) {
        if (this.contains(t))
            this.remove(t);
        return super.add(t);
    }
}
