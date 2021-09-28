package fr.univtours.info.optimize.tsp;


public interface Measurable<T> {

    public double dist(T other);

}
