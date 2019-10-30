package Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

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
        String sql = String.format("SELECT nickname FROM main  JOIN " +
                "(SELECT id_black FROM blacklist LEFT JOIN main ON main.id = blacklist.id WHERE main.nickname = '%s') AS blacklst on main.id=blacklst.id_black", nick);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                blacklist.add(rs.getString("nickname"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return blacklist;
    }

    static ArrayList<String> getInverseBlacklist(String nick){
        ArrayList<String> InverseBlacklist = new ArrayList<>();
        String sql = String.format("SELECT nickname FROM main  JOIN " +
                "(SELECT blacklist.id FROM blacklist LEFT JOIN main ON main.id = blacklist.id_black WHERE main.nickname = '%s') AS blacklst on main.id=blacklst.id", nick);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                InverseBlacklist.add(rs.getString("nickname"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return InverseBlacklist;
    }

    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    static void writeHistory(int id_user1, int id_user2, String message) {
        try {
            String sql = String.format("INSERT INTO history (id_user1, id_user2, date, message) VALUES('%s')", id_user1, id_user2, new Date(), message);
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static String getHistory(String nick) {
        String result = "";
        try {
            String sql = String.format("SELECT senders.nickname AS sender, receivers.nickname AS receiver, history.date, history.message FROM history " +
                                        "JOIN main AS senders ON senders.id = history.id_user1 " +
                                        "JOIN main AS receivers ON receivers.id = history.id_user2 WHERE sender = '%s' OR receiver = '%s'",  nick, nick);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                result += rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(4) + "/";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //убираю последний символ
        if (!result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static String getClientList(String nick, ArrayList<String> nicksOnline) {
        String result = "";
        try {
            String sql = String.format("SELECT nickname FROM main WHERE nickname<>'%s'", nick);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                String nickname = rs.getString(1);
                String status;
                if (nicksOnline.contains(nickname)){
                    status = " online";
                }else {
                    status = " offline";
                }

                result += nickname + status + "/";
                //result += nickname + "/";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //убираю последний символ
        if (!result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static void saveHistory(String nickSender, String nickReceiver, String message) {
        try {
            String sql = String.format("SELECT id FROM main WHERE nickname = '%s' OR nickname = '%s'", nickSender, nickReceiver);
            ResultSet rs = stmt.executeQuery(sql);
            String[] idArray = new String[2];
            int i = 0;
            while (rs.next()){
                idArray[i] = rs.getString("id");
                i++;
            }

            String sql1 = String.format("INSERT INTO history (id_user1, id_user2, date, message) VALUES('%s','%s','%s','%s')", idArray[0], idArray[1], new Date(), message);
            stmt.execute(sql1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
