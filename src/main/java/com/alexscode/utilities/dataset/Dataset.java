package com.alexscode.utilities.dataset;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interface abstraction the dataset concept
 * A dataset is an ordered collection of things that can be streamed, converted to a list and iterated over.
 *
 * Warning: The minimum implementation is either the stream() method (preferred) or the getAsList() method.
 *          The more performant implementation probably require overriding all the methods
 *
 * @param <E>
 */
public interface Dataset<E> extends Iterable<E> ,AutoCloseable {

    default long size(){
        return getAsList().size();
    }

    /**
     * Return the row at the index specified
     * @param index
     * @return the element (can itself be null) or null if index is out of bounds
     */
    default E getRow(int index){
        return stream().skip(index).findFirst().orElse(null);
    }

    /**
     * Return an optional of E
     * @param index
     * @return Optional.empty() is index is out of bound or an E
     */
    default Optional<E> safeGetRow(int index) {
        return stream().skip(index).findFirst();
    }

    @Override
    default Iterator<E> iterator(){
        return stream().iterator();
    }

    @Override
    default Spliterator<E> spliterator(){
        return stream().spliterator();
    }

    /**
     * Preferred method to be overriden
     * Return the dataset as a stream for efficient linear processing
     * @return A stream of E
     */
    default Stream<E> stream(){
        return getAsList().stream();
    }

    default List<E> getAsList() {
        return stream().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Given a function of E to F, convert a Dataset<E> to a Dataset<F>
     * (Dataset is a functor)
     * @param f conversion function
     * @param <F> output row type of of the output Dataset
     * @return A Dataset with row converted by the function f
     */
    default <F> Dataset<F> map(Function<E, F> f){
        Dataset<E> th = this;
        return new Dataset<F>()  {
            @Override
            public Stream<F> stream() {
                return th.stream().map(f);
            }

            @Override
            public void close() throws Exception {
                th.close();
            }
        };
    }

}
