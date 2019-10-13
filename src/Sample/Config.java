package Sample;

import java.io.File;
import java.io.IOException;
import java.sql.*;

class Config {
    private static Connection connection;
    private static Statement stmt;

    static Connection getConnection() {
        return connection;
    }

    static void connect(){
        try {
            boolean baseExist = checkDataBase();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:config.db");
            stmt = connection.createStatement();

            if (baseExist){
                createConfigDataBase();
            }
        } catch (ClassNotFoundException| SQLException e) {
            e.printStackTrace();
        }
    }

    static String[] getConfigData(){
        String[] configData = new String[2];

        try {
            String sql = "SELECT * FROM config";
            ResultSet rt = stmt.executeQuery(sql);
            rt.next();

            configData[0] = rt.getString("server");
            configData[1] = rt.getString("port");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return configData;
    }

    static void setConfigData(String server, int port){
        try {
            String sql = "DELETE FROM config";
            stmt.execute(sql);
            String sql1 = String.format("INSERT INTO config (server, port) VALUES ('%s', '%s')", server,port);
            stmt.execute(sql1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createConfigDataBase() {
        try {
            String sql = "CREATE TABLE config (server STRING  NOT NULL, port   INTEGER NOT NULL)";
            stmt.execute(sql);
            String sql1 = "INSERT INTO config (server, port) VALUES ('localhost', '8189')";
            stmt.execute(sql1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkDataBase(){
        String currentPath = new File("config.db").getAbsolutePath();
        File dataBase = new File(currentPath);

        boolean isBaseJustCreated = false;
        try {
            isBaseJustCreated = dataBase.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isBaseJustCreated;
    }

    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}