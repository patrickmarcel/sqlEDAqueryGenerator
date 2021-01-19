package com.alexscode.utilities.collection;

import java.util.*;
import java.util.function.Function;

/**
 * This classes represent a Multiset or Bag
 * It implement methods of the Collection interface
 * @param <E>
 */
public class MultiSet<E> implements Collection<E> {
    HashMap<E, Integer> hashMap;

    public MultiSet() {
        hashMap = new HashMap<>();
    }

    @Override
    public int size() {
        int n = 0;
        for (Map.Entry<E, Integer> entry : hashMap.entrySet())
            n += entry.getValue();
        return n;
    }

    public int distinctSize(){
        return hashMap.size();
    }

    @Override
    public boolean isEmpty() {
        return hashMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return hashMap.getOrDefault(o, 0) > 0;
    }

    @Override
    public Iterator<E> iterator() {
        ArrayList<E> tmp = new ArrayList<>();
        for(Map.Entry<E, Integer> element : hashMap.entrySet()){
                tmp.add(element.getKey());

        }
        return tmp.iterator();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(E e) {
        int c = hashMap.getOrDefault(e, 0) + 1;
        hashMap.put(e, c);
        return true;
    }

    public boolean add(E e, int count) {
        int c = hashMap.getOrDefault(e, 0) + count;
        hashMap.put(e, c);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int c = hashMap.getOrDefault(o, 0);
        if (c == 0){
            return false;
        }
        else if (c == 1){
            hashMap.remove(o);
        }
        else {
            hashMap.put((E) o, c-1);
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean added = false;
        if (c instanceof MultiSet){
            MultiSet<E> mc = (MultiSet<E>) c;
            for (E e: mc){
                this.add(e, mc.count(e));
                added = true;
            }
        }

        else {
            for (E e: c){
                this.add(e);
                added = true;
            }
        }

        return added;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        c.forEach(x -> {
            this.remove(x);
        });
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    public int count(E element){
        return hashMap.getOrDefault(element, 0);
    }

    public static MultiSet<String> fromString(String phrase, Function<String, String[]> tokenizer){
        MultiSet<String> ret = new MultiSet<>();
        String[] tokens = tokenizer.apply(phrase);
        ret.addAll(Arrays.asList(tokens));
        return ret;
    }

    public MultiSet<E> union(MultiSet<E> other){
        MultiSet<E> ret = new MultiSet<>();
        ret.addAll(this);
        ret.addAll(other);
        return ret;
    }

    public MultiSet<E> intersection(MultiSet<E> other){
        MultiSet<E> ret = new MultiSet<>();
        for (E el : this){
            int c = other.count(el);
            if (c > 0)
                ret.add(el, Math.min(this.count(el), c));
        }
        return ret;
    }

    @Override
    public String toString(){
        String s = "Bag: <";

        for (E e : this){
            int c = this.count(e);
            for (int i = 0; i < c; i++){
                s += e.toString() + " ";
            }
        }
        s = s.substring(0, s.length()-1) + ">";

        return s;
    }
}
