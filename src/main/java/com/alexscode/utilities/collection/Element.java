package com.alexscode.utilities.collection;

import lombok.Getter;

/**
 * Allows to sort double values in a collection while preserving their original indexes
 */
public class Element implements Comparable<Element> {

    @Getter
    public int index;
    @Getter
    public double value;

    public Element(int index, double value){
        this.index = index;
        this.value = value;
    }

    public int compareTo(Element e) {
        return Double.compare(this.value, e.value);
    }

    @Override
    public String toString() {
        return "structs.Element{" +
                "index=" + index +
                ", value=" + value +
                '}';
    }
}
