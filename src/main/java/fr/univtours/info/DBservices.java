package fr.univtours.info;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public  class DBservices {

    private transient Connection conn;

    public DBservices(){

    }

    /**
     * @return connection to the postgres instance
     */
    public Connection getConnection() {
        return conn;
    }


    public Connection connectToPostgresql() throws SQLException, IOException {
        final FileReader fr = new FileReader(new File(Config.CONF_FILE_PATH));
        final Properties props = new Properties();
        props.load(fr);
        final String passwd = props.getProperty("datasource.password");
        final String user = props.getProperty("datasource.user");
        final String url = props.getProperty("datasource.url") + "?user=" + user + "&password=" + passwd;;
        System.out.println(url);
        conn = DriverManager.getConnection(url);
        return conn;
        /*
        try (final Statement stmt = conn.createStatement();) {
            final String sqlCreate = "CREATE TABLE IF NOT EXISTS Plots (id SERIAL primary key, text TEXT, plot bytea)";
            stmt.execute(sqlCreate);
        } catch (final Exception e) {
            e.printStackTrace();
        }
         */
    }

}

