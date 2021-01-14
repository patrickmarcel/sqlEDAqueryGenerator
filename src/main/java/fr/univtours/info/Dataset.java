package fr.univtours.info;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import fr.univtours.info.metadata.DatasetSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Dataset {
    static int sampleNumber=1;
    Connection conn;
    DatasetSchema theSchema;
    String table;

     ArrayList<DatasetDimension> theDimensions;
     ArrayList<DatasetMeasure> theMeasures;

     public Dataset(Connection conn, String table, ArrayList<DatasetDimension> theDimensions, ArrayList<DatasetMeasure> theMeasures ){
         this.conn=conn;
         this.table=table;
         this.theDimensions=theDimensions;
         this.theMeasures=theMeasures;
     }

    DatasetSchema getSchema(){
        return null;
    }

    // todo : sample table should be indexed as well, using indexes over dimensions
    String computeSample(double percentage) throws SQLException {
        String tablename= "sample_" + sampleNumber ;
        String sql="create table " + tablename +"(";

        for(DatasetDimension d : theDimensions){
            sql = sql + d.getName() + " varchar, ";
        }
        for(DatasetMeasure m : theMeasures){
            sql = sql + m.getName() + " float, ";
        }
        sql =sql.substring(0,sql.length()-2);
        sql=sql+");";

        System.out.println(sql);

        Statement pstmt = conn.createStatement();
        pstmt.executeUpdate(sql) ;

        sql="insert into "+ tablename + " (select ";
        for(DatasetDimension d : theDimensions){
            sql = sql + d.getName() + ", ";
        }
        for(DatasetMeasure m : theMeasures){
            sql = sql + m.getName() + ", ";
        }
        sql =sql.substring(0,sql.length()-2);
        sql=sql+ " from "+ table + " where random()< " + percentage + ");";

        System.out.println(sql);


        pstmt = conn.createStatement();
        pstmt.executeUpdate(sql) ;

        return tablename; // return the table name where the sample is stored
    }

}
