package fr.univtours.info;

import fr.univtours.info.queries.AbstractEDAsqlQuery;
import fr.univtours.info.queries.EDAsqlQuery;

import java.util.ArrayList;

public class QtheSetOfGeneratedQueries {

    ArrayList<AbstractEDAsqlQuery> theQueries;

    public QtheSetOfGeneratedQueries(){
        theQueries=new ArrayList<AbstractEDAsqlQuery>();
    }

    public void addQuery(AbstractEDAsqlQuery q){
        theQueries.add(q);
    }

    public int getSize(){
        return theQueries.size();
    }
}
