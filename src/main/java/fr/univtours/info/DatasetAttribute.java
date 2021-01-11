package fr.univtours.info;


import java.util.Set;

public abstract class DatasetAttribute {
    Set activeDomain;
    String name;

    public abstract void setActiveDomain();

    public DatasetAttribute(String name){
        this.name=name;
    }

}
