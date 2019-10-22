package Server;

import Sample.TextMessage;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class Test {
    private static Map<String, VBox> vBoxCollection = new HashMap<>();
    private static String nickname = "nick1"; //ник клиента

    public static void main(String[] args) {
        AuthService.connect();

        String result = AuthService.getHistory(nickname);
        String[] strings = result.split("/");
        for (String str:strings) {
            String nickSender;
            //String nickReceiver;
            String collectionOwner;
            String[] tokens = str.split(" ",3);
            nickSender = tokens[0];

            if (nickname.equals(tokens[0])) {
                collectionOwner = tokens[1];
            }else {
                collectionOwner = tokens[0];
            }
            insertIntoVBox(nickSender, collectionOwner, tokens[2]);
        }





        AuthService.disconnect();
    }

                                                //nick3           //nick1
    private static void insertIntoVBox(String nickSender, String collectionOwner, String msg) {
        VBox vBoxChat = new VBox();

        if (vBoxCollection.containsKey(collectionOwner)) {
            vBoxChat = vBoxCollection.get(collectionOwner);
        }else {
            vBoxCollection.put(collectionOwner,vBoxChat);
        }

        VBox vb = new VBox();
        HBox hb = new HBox();
        VBox vb1 = new VBox();
        VBox vb2 = new VBox();

        if (nickSender.equals(nickname)) {
            vb1.getChildren().add(new TextMessage(msg));
            vb.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }else {
            vb2.getChildren().add(new TextMessage(msg));
            vb.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        hb.getChildren().addAll(vb1, vb2);
        vb.getChildren().add(hb);

        vBoxChat.getChildren().addAll(vb);
        //listView.scrollTo(listView.getItems().size());
    }
}
