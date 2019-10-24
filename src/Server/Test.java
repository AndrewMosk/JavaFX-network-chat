package Server;

import javafx.scene.layout.VBox;
import java.util.HashMap;
import java.util.Map;

public class Test {
    private static Map<String, VBox> vBoxCollection = new HashMap<>();
    private static String nickname = "nick1"; //ник клиента

    public static void main(String[] args) {
        AuthService.connect();

        //String sql1 = String.format("INSERT INTO history (id_user1, id_user2, date, message) VALUES('%s','%s','%s','%s')", idArray[0], idArray[1], new Date(), message);
        AuthService.saveHistory("nick1","nick2","test");



        AuthService.disconnect();
    }
}
