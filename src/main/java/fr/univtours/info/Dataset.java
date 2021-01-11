package fr.univtours.info;

import java.util.ArrayList;

public class Dataset {
    DatasetSchema theSchema;

     ArrayList<DatasetDimension> theDimensions;
     ArrayList<DatasetMeasure> theMeasures;

     public Dataset(ArrayList<DatasetDimension> theDimensions, ArrayList<DatasetMeasure> theMeasures ){
         this.theDimensions=theDimensions;
         this.theMeasures=theMeasures;
     }

    DatasetSchema getSchema(){
        return null;
    }

    String computeSample(){
        return null; // return the table name where the sample is stored
    }

}
