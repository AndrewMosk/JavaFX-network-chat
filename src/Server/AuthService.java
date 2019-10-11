package Server;

import org.sqlite.SQLiteException;

import java.sql.*;
import java.util.ArrayList;

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

        //пустой результат запроса - регистрационные данные валидны, можно регистрировать
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

    static boolean  addUserToBlackList(String nick, String nickToBlackList){
        if (isUserAlreadyInList(nick, nickToBlackList)){
            return true; //цель достигнута - юзер в списке и не особо важно, что он и так там уже был
        }else {
            String sql = String.format("INSERT INTO blacklist (id, id_black) VALUES ((SELECT id FROM main WHERE nickname = '%s'),(SELECT id FROM main WHERE nickname = '%s'))", nick,nickToBlackList);
            try {
                return  !stmt.execute(sql);
            } catch (SQLException e) {
                //e.printStackTrace();
                //false только в том случае, когда ника, который нужно внести в черный список нет в базе
                return false;
            }
        }
    }

    private static boolean isUserAlreadyInList(String nick, String nickToBlackList){
        String sql  = String.format("SELECT blacklst.id_black FROM (SELECT id_black FROM blacklist " +
                "LEFT JOIN main ON main.id = blacklist.id WHERE main.nickname = '%s') AS blacklst WHERE blacklst.id_black IN (SELECT id FROM main WHERE nickname = '%s')", nick,nickToBlackList);

        boolean userExist = false;
        try {
            ResultSet rs = stmt.executeQuery(sql);
            userExist = rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userExist;
    }

    static ArrayList<String> getBlacklist(String nick){
        ArrayList<String> blacklist = new ArrayList<>();

//        SELECT nickname FROM main
//        JOIN blacklst on main.id=blacklst.id_black
//
//                (SELECT id_black FROM blacklist
//                        LEFT JOIN main ON main.id = blacklist.id WHERE main.nickname = 'nick1') AS blacklst

        return blacklist;
    }

    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
