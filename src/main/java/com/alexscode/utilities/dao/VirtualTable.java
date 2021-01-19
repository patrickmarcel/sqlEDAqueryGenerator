package com.alexscode.utilities.dao;

import java.util.List;

/**
 * Interface to be implemented by concrete DAO classes
 * @param <T> The type of the object mapped to the sql table
 */
public interface VirtualTable<T> {
    T find(Object key);
    boolean add(T o);
    boolean update(Object key, T o);
    boolean delete(Object key);
    List<T> getAll();
}
