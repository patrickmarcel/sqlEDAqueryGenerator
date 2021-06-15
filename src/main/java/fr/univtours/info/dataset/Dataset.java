package fr.univtours.info.dataset;

import fr.univtours.info.dataset.metadata.DatasetAttribute;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import fr.univtours.info.dataset.metadata.DatasetSchema;
import lombok.Getter;


import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class Dataset {
    @Getter
    Connection conn;
    @Getter
    DatasetSchema theSchema;
    @Getter
    String table;
    @Getter
    List<DatasetDimension> theDimensions;
    @Getter
    List<DatasetMeasure> theMeasures;
    @Getter
    int tableSize;

     public Dataset(Connection conn, String table, List<DatasetDimension> theDimensions, List<DatasetMeasure> theMeasures ){
         this.conn=conn;
         this.table=table;
         this.theDimensions=theDimensions;
         this.theMeasures=theMeasures;
         theSchema = new DatasetSchema(this);
         //theSchema.getIndividualHierarchies();
         try (ResultSet rs = conn.createStatement().executeQuery("Select count(*) from " +table)){
             rs.next();
             tableSize = rs.getInt(1);
         }catch (SQLException e){
             System.err.println("Error impossible to fetch table size");
         }
     }

    DatasetSchema getSchema(){
        return null;
    }

    public Dataset computeSample(double percentage, Connection destination) throws SQLException {
        String sampleTableName= "sample_" + table ;
        // Only works with "default" structure
        String sql="create table " + sampleTableName +"(";

        for(DatasetDimension d : theDimensions){
            sql = sql + d.getName() + " varchar, ";
        }
        for(DatasetMeasure m : theMeasures){
            sql = sql + m.getName() + " float, ";
        }
        sql =sql.substring(0,sql.length()-2);
        sql=sql+");";

        // Create table in H2
        Statement createSt = destination.createStatement();
        createSt.executeUpdate(sql) ;
        createSt.close();

        //Compile an insert query
        String insert = "INSERT INTO "+sampleTableName +"("
                + theDimensions.stream().map(DatasetAttribute::getName).collect(Collectors.joining(", ")) + ", "
                + theMeasures.stream().map(DatasetAttribute::getName).collect(Collectors.joining(", ")) + ") VALUES (";
        int vals = theDimensions.size() + theMeasures.size();
        for (int i = 0; i < vals; i++) {
            insert += "?";
            insert += i < vals - 1 ? ", " : "";
        }
        insert += ");";
        PreparedStatement insertSt = destination.prepareStatement(insert);

        // Fetch sample from source database
        Statement originSt = conn.createStatement();

        sql="select " + theDimensions.stream().map(DatasetAttribute::getName).collect(Collectors.joining(", "));
        for(DatasetMeasure m : theMeasures)
            sql += ", " + m.getName();
        sql=sql+ " from "+ table + " where random()< " + percentage + ";";

        ResultSet origin = originSt.executeQuery(sql);

        int rows = 0;
        while (origin.next()){
            rows++;
            int pos = 1;
            for (DatasetAttribute dim : theDimensions){
                insertSt.setString(pos++, origin.getString(dim.getName()));
            }
            for (DatasetMeasure meas : theMeasures){
                insertSt.setFloat(pos++, origin.getFloat(meas.getName()));
            }
            insertSt.addBatch();
            if (rows % 10000 == 0)
                insertSt.executeBatch();
        }
        insertSt.executeBatch();



        insertSt.close();
        originSt.close();
        return new Dataset(destination, sampleTableName, theDimensions, theMeasures);
    }

}
