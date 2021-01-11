package fr.univtours.info;

import java.util.ArrayList;

public class QtheSetOfGeneratedQueries {

    ArrayList<EDAsqlQuery> theQueries;

    public QtheSetOfGeneratedQueries(){
        theQueries=new ArrayList<EDAsqlQuery>();
    }

    public void addQuery(EDAsqlQuery q){
        theQueries.add(q);
    }
}
