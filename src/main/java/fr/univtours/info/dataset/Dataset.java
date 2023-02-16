package fr.univtours.info.dataset;

import fr.univtours.info.dataset.metadata.*;
import lombok.Getter;


import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
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

    private boolean inMemory = false;

     public Dataset(Connection conn, String table, List<DatasetDimension> theDimensions, List<DatasetMeasure> theMeasures ){
         this.conn=conn;
         this.table=table;
         this.theDimensions=theDimensions;
         this.theMeasures=theMeasures;
         theSchema = new DatasetSchema(this);
         try (ResultSet rs = conn.createStatement().executeQuery("Select count(*) from \"" + table + "\";")){
             rs.next();
             tableSize = rs.getInt(1);
         }catch (SQLException e){
             System.err.println("[ERROR] impossible to fetch table size");
         }
     }

    public Dataset(Connection conn, String table, List<DatasetDimension> theDimensions, List<DatasetMeasure> theMeasures, boolean inMemory){
        this(conn, table, theDimensions, theMeasures);
        this.inMemory = inMemory;
    }

    DatasetSchema getSchema(){
        return null;
    }

    public Dataset computeUniformSample(double percentage, Connection destination) throws SQLException {
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
                insertSt.setString(pos++, origin.getString(dim.getPrettyName()));
            }
            for (DatasetMeasure meas : theMeasures){
                insertSt.setFloat(pos++, origin.getFloat(meas.getPrettyName()));
            }
            insertSt.addBatch();
            if (rows % 10000 == 0)
                insertSt.executeBatch();
        }
        insertSt.executeBatch();



        insertSt.close();
        originSt.close();
        return new Dataset(destination, sampleTableName, theDimensions, theMeasures, true);
    }

    public Dataset computeStatisticalSample(DatasetDimension dim, int size, Connection destination) throws SQLException {
        String sampleTableName= "sample_" + table + "_on_" + dim.getName().replaceAll("\"", "").replace(' ', '_') ;

        // Only works with "default" structure
        String sql="create table " + sampleTableName +"(";
            sql = sql + dim.getName() + " varchar, ";
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
        final String measuresString = theMeasures.stream().map(DatasetAttribute::getName).collect(Collectors.joining(", "));
        String insert = "INSERT INTO "+sampleTableName +"("
                + dim.getName() + ", "
                + measuresString + ") VALUES (";
        int vals = 1 + theMeasures.size();
        for (int i = 0; i < vals; i++) {
            insert += "?";
            insert += i < vals - 1 ? ", " : "";
        }
        insert += ");";
        PreparedStatement insertSt = destination.prepareStatement(insert);

        // Fetch sample from source database

        sql = "select X."+dim.getName()+", "+ measuresString +" from " +
                "              (select "+dim.getName()+", "+ measuresString +", random() r from "+ table +") X" +
                "                  join (select "+dim.getName()+", count(*)::double precision c from "+ table +" group by  "+dim.getName()+") Y on X."+dim.getName()+" = Y."+dim.getName()+" where r < ("+size+".0/(select count(distinct "+dim.getName()+") from "+ table +")::double precision)/c;";

        RowSetFactory factory = RowSetProvider.newFactory();
        CachedRowSet origin = factory.createCachedRowSet();
        origin.setCommand(sql);
        origin.execute(conn);

        //HashSet<String> ad = new HashSet<String>();
        int rows = 0;
        while (origin.next()){
            rows++;
            int pos = 1;
            insertSt.setString(pos++, origin.getString(dim.getPrettyName()));
            //ad.add(origin.getString(dim.getPrettyName()));
            for (DatasetMeasure meas : theMeasures){
                insertSt.setFloat(pos++, origin.getFloat(meas.getPrettyName()));
            }
            insertSt.addBatch();
            if (rows % 10000 == 0)
                insertSt.executeBatch();
        }
        insertSt.executeBatch();
        //System.out.println(dim + " | " + ad.size() + " | " + ad);


        insertSt.close();
        origin.close();
        return new Dataset(destination, sampleTableName, List.of(dim), theMeasures, true);
    }

    public int measuresNb(){
         return this.theMeasures.size();
    }

    public void drop(){
         if (inMemory){
             String sql= "drop table "+table+";";
             // drop table in H2
             Statement createSt = null;
             try {
                 createSt = conn.createStatement();
                 createSt.executeUpdate(sql) ;
                 createSt.close();
             } catch (SQLException throwables) {
                 throwables.printStackTrace();
             }

         }else {
             System.err.println("[WARNING] couldn't drop table only supported for in memory datasets !");
         }
    }

}
