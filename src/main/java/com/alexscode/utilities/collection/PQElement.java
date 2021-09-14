package com.alexscode.utilities.collection;

import lombok.Getter;

public class PQElement implements Comparable<PQElement> {

    @Getter
    public int index;
    @Getter
    public double value;

    public PQElement(int index, double value){
        this.index = index;
        this.value = value;
    }

    public int compareTo(PQElement e) {
        return Double.compare(this.value, e.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PQElement)
            return index == ((PQElement) obj).index;
        else
            return false;
    }

    @Override
    public String toString() {
        return "structs.Element{" +
                "index=" + index +
                ", value=" + value +
                '}';
    }
}
