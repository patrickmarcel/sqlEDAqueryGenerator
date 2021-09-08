package fr.univtours.info.queries;

import fr.univtours.info.dataset.DBConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPool {

        String connString;
        String user;
        String pwd;

        static final int INITIAL_CAPACITY = 16;
        BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(INITIAL_CAPACITY+1);


        public ConnectionPool(DBConfig dbc) throws SQLException {
            this.connString = dbc.getBaseURL();
            this.user = dbc.getBaseUser();
            this.pwd = dbc.getBasePassword();

            for (int i = 0; i < INITIAL_CAPACITY; i++) {
                pool.add(DriverManager.getConnection(connString, user, pwd));
            }

        }

        public Connection getConnection() {
            try {
                return pool.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void returnConnection(Connection connection) {
            try {
                pool.put(connection);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void close(){
            for (Connection c :
                    pool) {
                try {
                    c.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }


}
