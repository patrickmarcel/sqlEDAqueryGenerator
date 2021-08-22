package com.alexscode.utilities.collection;

import lombok.Getter;

/**
 * Allows to sort double values in a collection while preserving their original indexes
 */
public class ElementPair implements Comparable<ElementPair> {

    @Getter
    public int i;
    @Getter
    public int j;
    @Getter
    public double value;

    public ElementPair(int i, int j, double value){
        this.i = i;
        this.j = j;
        this.value = value;
    }

    public int compareTo(ElementPair e) {
        return Double.compare(this.value, e.value);
    }


}
