package com.alexscode.utilities.collection;

/**
 * Allows to sort double values in a collection while preserving their original indexes
 */
public class Element implements Comparable<Element> {

    public int index;
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
