package fr.univtours.info.queries;

import java.util.*;

public class CandidateQuerySet implements Collection<AssessQuery> {

    List<AssessQuery> theQueries;

    public CandidateQuerySet(){
        theQueries=new ArrayList<AssessQuery>();
    }

    public void addQuery(AssessQuery q){
        theQueries.add(q);
    }


    @Override
    public int size() {
        return theQueries.size();
    }

    @Override
    public boolean isEmpty() {
        return theQueries.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return theQueries.contains(0);
    }

    @Override
    public Iterator<AssessQuery> iterator() {
        return theQueries.iterator();
    }

    @Override
    public Object[] toArray() {
        return theQueries.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return theQueries.toArray(a);
    }

    @Override
    public boolean add(AssessQuery assessQuery) {
        return theQueries.add(assessQuery);
    }

    @Override
    public boolean remove(Object o) {
        return theQueries.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return theQueries.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends AssessQuery> c) {
        return theQueries.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return theQueries.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return theQueries.removeAll(c);
    }

    @Override
    public void clear() {

    }

    //debug use only
    @Deprecated
    public void shrink(){
        theQueries = theQueries.subList(0,999);
    }
}
