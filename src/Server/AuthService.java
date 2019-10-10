package Server;

import java.sql.*;

class AuthService {
    private static Connection connection;
    private static Statement stmt;

    static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
    }

    static String getNickByLoginAndPass(String login, String pass) throws SQLException {
        String sql = String.format("SELECT nickname FROM main WHERE login='%s' AND password='%s'",login,pass);

        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()){
            return rs.getString("nickname");
        }
        return null;
    }

    static boolean checkLoginAndNick(String login, String nick) throws SQLException {
        //логин и ник должны быть уникальны во всех записях таблицы
        String sql = String.format("SELECT * FROM main WHERE login='%s' OR nickname='%s'",login,nick);
        ResultSet rs = stmt.executeQuery(sql);

        //пустой результат запроса - регистрационные данные валидны, можно регестрировать
        return !rs.next();
    }

    static boolean regNewUser(String login, String nick, String password){
        String sql = String.format("INSERT INTO main (login, password, nickname) VALUES ('%s', '%s','%s')", login, password, nick);
        boolean successQuery = false;
        try {
            successQuery = !stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return successQuery;
    }

    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
