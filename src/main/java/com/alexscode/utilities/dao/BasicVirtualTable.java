package com.alexscode.utilities.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Alexandre Chanson
 * A simple polymorphic DAO only works with well defined tables (Primary key must exist)
 * @param <T> The type of the objet held in the table
 */
public class BasicVirtualTable<T> implements VirtualTable<T>{
    protected final Class myClass;
    protected String tableName;
    protected Constructor myConstructor;
    private Field key;
    private final String columns;
    private final Connection connection;

    public BasicVirtualTable(Class targeted, Connection con) {
        myClass = targeted;
        this.connection = con;
        try {
            tableName = ((Entity)myClass.getAnnotationsByType(Entity.class)[0]).tableName();
        } catch (Exception e){
            e.printStackTrace();
        }
        try {
            myConstructor = getConstructor(myClass);
        } catch (NoSuchMethodException e){
            System.err.printf("No appropriate constructor found for %s, use @DaoConstructor to reference one.%n", myClass.getSimpleName());
        }
        List<String> colNames = new ArrayList<>();
        for (Field field : targeted.getDeclaredFields()){
            if (field.getAnnotation(Key.class) != null)
                key = field;
            if (!isTransient(field))
                colNames.add(field.getAnnotation(Column.class) != null ? field.getAnnotation(Column.class).name() : field.getName());
            else
                colNames.add(null);
        }
        columns = formatedColNames(colNames);
    }

    @Override
    public boolean delete(Object key) {
        Column c = this.key.getAnnotation(Column.class);
        String query = String.format("DELETE FROM %s WHERE %s = %s ;", tableName, c != null ? c.name() : this.key.getName(), formatObjToString(key));
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
        }catch (SQLException e){
            return false;
        }

        return true;
    }

    @Override
    public T find(Object key) {
        if (this.key == null){
            System.err.printf("No @Key declared for class %s !%n", myClass.getSimpleName());
            return null;
        }
        String colName = this.key.getAnnotation(Column.class) != null ? this.key.getAnnotation(Column.class).name() : this.key.getName();
        return find(key, colName);
    }

    public T find(Object key, String colName){
        if (key == null)
            return null;
        String formatedKey = formatObjToString(key);
        Object[] params = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            statement.execute("SELECT * from " + tableName + " WHERE "+colName+" = "+formatedKey+";");
            resultSet = statement.getResultSet();
            int paramsNb = myConstructor.getParameterCount();
            params = new Object[paramsNb];
            if (!resultSet.next())
                return null;
            for (int i = 1; i <= paramsNb; ++i)
                params[i - 1] = resultSet.getObject(i);
            resultSet.close();
            statement.close();
            return (T) myConstructor.newInstance(params);

        }catch (SQLException e){
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.err.printf("Error on importing %s from %s with parameters (%s).%n", myClass.getSimpleName(), tableName, Arrays.toString(params));
            return null;
        }
        finally {
            try {
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public T find(Object key1, String colName1, Object key2, String colName2){
        if (key1 == null || key2 == null)
            return null;
        String query = String.format("SELECT * from %s WHERE %s = %s AND %s = %s ;", tableName, colName1, formatObjToString(key1), colName2, formatObjToString(key2));
        Object[] params = null;
        try {
            Statement statement = connection.createStatement();
            statement.execute(query);
            ResultSet resultSet = statement.getResultSet();
            int paramsNb = myConstructor.getParameterCount();
            params = new Object[paramsNb];
            if (!resultSet.next())
                return null;
            for (int i = 1; i <= paramsNb; ++i)
                params[i - 1] = resultSet.getObject(i);
            resultSet.close();
            statement.close();
            return (T) myConstructor.newInstance(params);

        }catch (SQLException e){
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.err.printf("Error on importing %s from %s with parameters (%s).%n", myClass.getSimpleName(), tableName, Arrays.toString(params));
            return null;
        }

    }

    public List<T> findAll(Object key1, String colName1, Object key2, String colName2){
        if (key1 == null || key2 == null)
            return null;
        String query = String.format("SELECT * from %s WHERE %s = %s AND %s = %s ;", tableName, colName1, formatObjToString(key1), colName2, formatObjToString(key2));
        Object[] params = null;
        List<T> ret = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            statement.execute(query);
            ResultSet resultSet = statement.getResultSet();
            int paramsNb = myConstructor.getParameterCount();

            params = new Object[paramsNb];
            while (resultSet.next()) {
                for (int i = 1; i <= paramsNb; ++i)
                    params[i - 1] = resultSet.getObject(i);

                ret.add( (T) myConstructor.newInstance(params));
            }
            resultSet.close();
            statement.close();
        }catch (SQLException e){
            e.printStackTrace();

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.err.printf("Error on importing %s from %s with parameters (%s).%n", myClass.getSimpleName(), tableName, Arrays.toString(params));

        }
        return ret;

    }

    /**
     * Adds the object to the table
     * @param o the object to insert
     */
    @Override
    public boolean add(T o) {
        Class c = o.getClass();
        StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " " + columns + " VALUES (");
        for (Field field : c.getDeclaredFields()){
            if (! Modifier.isTransient(field.getModifiers())){
                field.setAccessible(true);
                try {
                    query.append(field.get(o).getClass().equals(String.class) ? "\'" + field.get(o) + "\'" : field.get(o));
                    query.append(",");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        query.setCharAt(query.lastIndexOf(","), ')');
        query.append(";");

        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query.toString());
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * More efficient insertion method requires implementation of @DBBean
     * @param bean an object of type T implementing the interface
     * @return success status of the insertion
     */
    public boolean addBean(DBBean bean){
        String query = String.format("INSERT INTO %s %s VALUES (%s);", tableName, columns, bean.insertToString());
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean update(Object key, T o) {
        //TODO update method
        return false;
    }

    /**
     * Recover the whole table
     * @return The entire table contents as a List
     */
    public List<T> getAll() {
        try {
            //Reference to your connexion singleton goes here
            Statement statement = connection.createStatement();
            statement.execute("SELECT * from " + tableName + ";");
            ResultSet resultSet = statement.getResultSet();
            List<T> result = new ArrayList<>();
            int paramsNb = myConstructor.getParameterCount();
            Object[] params = new Object[paramsNb];

            while (resultSet.next()) {

                for (int i = 1; i <= paramsNb; ++i) {
                    params[i - 1] = resultSet.getObject(i);
                }
                try {
                    result.add((T) myConstructor.newInstance(params));

                } catch (Exception e1) {
                    System.err.printf("Error on importing %s from %s with parameters (%s).%n", myClass.getSimpleName(), tableName, Arrays.toString(params));
                }
            }
            resultSet.close();
            return result;
        } catch (SQLException e) {
            System.err.printf("Error on importing table : %s, error : %s.%n", tableName, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches all the table elements in a more civilized manner
     * @return A stream tied to the cursor
     */
    public Stream<T> getStream(){
        Function<ResultSet, T> onNext = resultSet -> {
            try {
                int paramsNb = myConstructor.getParameterCount();
                Object[] params = new Object[paramsNb];
                for (int i = 1; i <= paramsNb; ++i) {
                    params[i - 1] = resultSet.getObject(i);
                }
                return (T) myConstructor.newInstance(params);
            } catch (SQLException | IllegalAccessException | InstantiationException | InvocationTargetException e){
                e.printStackTrace();
                return null;
            }


        };

        try {
            Statement statement = connection.createStatement();
            statement.setFetchSize(50);
            statement.execute("SELECT * from " + tableName + ";");
            ResultSet resultSet = statement.getResultSet();
            return new ResultSetIterable<T>(resultSet, onNext, statement).stream();
        } catch (SQLException e){
            e.printStackTrace();
            return new ArrayList<T>().stream();
        }
    }

    /**
     * Internal method used to fetched the annotated constructor of a class
     * @param target the Class we need a constructor for
     * @return The constructor declared using @DaoConstructor annotation
     * @throws NoSuchMethodException if there is no constructor annotated
     */
    private static Constructor getConstructor(Class target) throws NoSuchMethodException {
        for (Constructor c :
                target.getConstructors()) {
            if (c.isAnnotationPresent(DaoConstructor.class))
                return c;
        }
        throw new NoSuchMethodException();
    }

    private static boolean isTransient(Field f){
        return f.toGenericString().contains("transient");
    }

    private static String formatedColNames(List<String> list){
        StringBuilder builder = new StringBuilder("(");
        list.forEach(cname -> {
            if (cname != null)
                builder.append(cname).append(",");
        });
        builder.setCharAt(builder.lastIndexOf(","), ')');
        return builder.toString();
    }

    protected static String formatObjToString(Object o){
        return o instanceof String ? "\'" + o + "\'" : o.toString();
    }


}
