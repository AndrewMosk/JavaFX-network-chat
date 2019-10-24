package Sample;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Controller {
    @FXML
    TextField textField;
    @FXML
    MenuItem close;
    @FXML
    MenuItem clear;
    @FXML
    MenuItem about;
    //@FXML
    private VBox vBoxChat = new VBox();
    private Map<String, VBox> vBoxCollection = new HashMap<>();
    @FXML
    SplitPane splitPane;

    @FXML
    ListView listView;
    @FXML
    ListView clientsList;
    private Map<String, Integer> messageCounterCollection = new HashMap<>();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isAuthorized;

    @FXML
    HBox bottomPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    TextField nickField;
    @FXML
    VBox VBoxUpperPanel;
    @FXML
    Button registrationButton;

    private Stage regStage = new Stage();
    private Stage infoStage = new Stage();
    private Stage configStage = new Stage();

    private String nickname = "";

    private void setAuthorized(boolean isAuthorized){
        this.isAuthorized = isAuthorized;
        if (!isAuthorized){
            VBoxUpperPanel.setVisible(true);
            VBoxUpperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
            //а здесь нужна очистка? пожалуй - да
            splitPane.getItems().clear();
        }else {
            VBoxUpperPanel.setVisible(false);
            VBoxUpperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        }
    }

    private void setNewTitle(String nick){
        Stage stage = Main.getPrimaryStage();

        if(nick.isEmpty()){
            stage.setTitle("Chat");
            nickname = "";
        }else {
            String newTitle = stage.getTitle() + " " + nick;
            nickname = nick;
            stage.setTitle(newTitle);
        }
    }

    private void connect() {
        try {
            String[] configData = getConfigData();
            socket = new Socket(configData[0], Integer.parseInt(configData[1]));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Task<Void> task = new Task<Void>() {
                @Override protected Void call() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/authOk")) {
                                String[] tokens = str.split(" ");
                                //setAuthorized(true);
                                Platform.runLater(() -> {
                                    setNewTitle(tokens[1]);
                                    //fillClientsList();
                                    try {
                                        out.writeUTF("/get_clientList " + nickname);
                                        out.writeUTF("/get_history " + nickname); //нужно отправлять историю только тому, кто только что авторизовался!
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                    setAuthorized(true);
                                });
                                break;
                            } else if (str.equals("Ошибка аутентификаци")) {
                                Platform.runLater(() -> showAlertWithHeaderText(str, "Неверно введена пара логин/пароль"));
                            } else if (str.equals("Попытка повторного входа")) {
                                Platform.runLater(() -> showAlertWithHeaderText(str, "Клиент с такими учетными данными уже воплнил вход"));
                            } else if (str.equals("Регистрация прошла успешно")) {
                                Platform.runLater(() -> {
                                    showAlertWithHeaderText(str, "Вы можете осуществить вход по только что введенным учетным данным");
                                    //закрываю окно регистрации
                                    Stage stage = (Stage) registrationButton.getScene().getWindow();
                                    stage.close();
                                });
                            } else if (str.equals("Регистрация закончилась неудачей")) {
                                Platform.runLater(() -> showAlertWithHeaderText(str, "Возможно возникла техническая проблема, попробуйте пройти регистрацию еще раз"));
                            } else if (str.equals("Регистрация отклонена")) {
                                Platform.runLater(() -> showAlertWithHeaderText(str, "Такой логин или ник уже зарегестрированы"));
                            }
                        }
                        while (true) {
                            String msg = in.readUTF();
                            if (msg.equalsIgnoreCase("/clientClose")) {
                                setAuthorized(false);
                                Platform.runLater(() -> {
                                    setNewTitle("");
                                    vBoxChat.getChildren().clear();//ПЕРЕДЕЛАТЬ ---> ОТПРАВЛЯТЬ В ПРОЦЕДУРУ ОЧИСТКИ ВБОКСА ВМЕСТЕ С АПДЕЙТОМ КОЛЛЕКЦИИ!
                                });
                            }else if (msg.startsWith("/clientList")){
                                //создание и заполнение списка clientsList
                                String[] tokens = msg.split(" ",2);
                                Platform.runLater(() -> {
                                    fillClientsList(tokens[1]);
                                });


                                //ТУТ ТРЕБУЕТСЯ ИЗМЕНИТЬ! ТЕПЕРЬ ПРИ ПРИХОДЕ ДАННОГО КЛЮЧЕВОГО СЛОВА БУДЕТ СОДЕРЖАТЬСЯ ИНФА ОТ 1 КЛИЕНТА - ОНЛАЙН ОН ИЛИ ОФФЛАЙН!
                                //КЛИЕНТ ТОТ БУДЕТ ДВАЖДЫ ОТПРАВЛЯТЬ ВСЕМ СВОЙ СТАТУС - ПРИ УСПЕШНОЙ АВТОРИЗАЦИИ - ОНЛАЙН, ПРИ ОТКЛЮЧЕНИИ - ОФФЛАЙН
                                //тут будет /clientstatus!!!!


//                                String[] tokens = msg.split(" ");
//                                Platform.runLater(() -> {
//                                markClient(true);
////                                    clientsList.getItems().clear();
////                                    for (int i = 1; i < tokens.length; i++) {
////                                        if (!tokens[i].equals(nickname)) {
////                                            clientsList.getItems().add(tokens[i]);
////                                        }
////                                    }
//                                });
                            } else if (msg.equals("Ошибка добавления в черный список")) {
                                Platform.runLater(() -> showAlertWithHeaderText(msg, "Нельзя добавить в черный список самого себя"));
                            } else if (msg.startsWith("/blacklist")) {
                                String[] tokens = msg.split(" ", 3);
                                Platform.runLater(() -> showAlertWithHeaderText(tokens[2], "Пользователь " + tokens[1]));
                            }else if (msg.startsWith("/history")) {
                                String[] tokens = msg.split(" ", 2);
                                fillHistory(tokens[1]);
                            }else if (msg.startsWith("/clientStatus")) {
                                String[] tokens = msg.split(" ", 3);
                                markClient(tokens[1],tokens[2]);

                            } else {
                                System.out.println(msg);
                                Platform.runLater(() -> addText(msg));
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }finally {
                        try {
                            in.close();
                            out.close();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            };
            Thread thread =new Thread(task);
            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void markClient(String nick, String status) {
        ObservableList list = clientsList.getItems();
        for (Object obj : list) {
            HBox box = (HBox) obj;
            String labelString = box.getChildren().get(0).toString();
            String[] parseLabelString = labelString.split("'", 3);
            if (parseLabelString[1].equals(nick)) {
                Button button = (Button) box.getChildren().get(1);
                if (status.equals("true")){
                    button.setStyle("-fx-background-radius: 5em; -fx-min-width: 5px; -fx-min-height: 5px; -fx-max-width: 5px; -fx-max-height: 5px; -fx-background-color: green;");
                }else {
                    button.setStyle("-fx-background-radius: 5em; -fx-min-width: 5px; -fx-min-height: 5px; -fx-max-width: 5px; -fx-max-height: 5px; -fx-background-color: red;");
                }
            }
        }
    }

    private void fillClientsList(String message) {
        String[] tokens = message.split("/");
        for (String nick_status: tokens) {
            String[] parse_nick_status = nick_status.split(" ");
            HBox hbox = new HBox();
            Label label = new Label(parse_nick_status[0]);
            Button button = new Button("");
            button.setDisable(true);
            hbox.getChildren().addAll(label,button);

            String color;
            if (parse_nick_status[1].equals("online")) color = "green;";
            else color = "red;";
            button.setStyle("-fx-background-radius: 5em; -fx-min-width: 5px; -fx-min-height: 5px; -fx-max-width: 5px; -fx-max-height: 5px; -fx-background-color: " + color);
            clientsList.getItems().add(hbox);
        }
    }

    private void fillHistory(String history) {
        String[] strings = history.split("/");
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
    }

    private void insertIntoVBox(String nickSender, String collectionOwner, String msg) {
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

    private String getNickCounter(String nickOriginal, String nickSenderCounter, int counter){
        String result;
        if (counter==0) {
            result = nickOriginal;
        }else {

            nickSenderCounter = nickSenderCounter.substring(0, nickOriginal.length());
            result =  nickSenderCounter + "(" + counter + ")";
        }

        return result;
    }

    private void addText(String msg){
        String[] tokens = msg.split(":",2);
        String nickSender = tokens[0];
        String nickSenderCounter = tokens[0];

        //ошибка при нажатии на список клиентов, если там никого нет
        //счетчик пояаляется даже, если у обоих клиентов активны чаты друг друга
        //выделение сбрасыватеся, когда кликаешь при непринятых сообщениях

//        if (!nickname.equals(nickSender)){
//            if (!messageCounterCollection.containsKey(nickSender)){
//                messageCounterCollection.put(nickSender,0);
//            }
//
//            if (clientsList.getSelectionModel().getSelectedItem()!=null) {
//                if (!clientsList.getSelectionModel().getSelectedItem().toString().equals(nickSender)) {
//                    //ищу вбокс в коллекции уже созданных. нашел - беру его, не нашел - создаю новый и пишу в коллекцию
//                    if (vBoxCollection.containsKey(nickSender)) {
//                        vBoxChat = vBoxCollection.get(nickSender);
//                    } else {
//                        vBoxChat = new VBox();
//                        vBoxCollection.put(nickSender, vBoxChat);
//                    }
//
//                    int messageCounter = messageCounterCollection.get(nickSender);
//                    int index = clientsList.getItems().indexOf(getNickCounter(nickSender,nickSenderCounter,messageCounter));
//                    messageCounter++;
//                    clientsList.getItems().set(index, getNickCounter(nickSender,nickSenderCounter,messageCounter));
//                    messageCounterCollection.replace(nickSender,messageCounter);
//                }
//            } else {
//                if (vBoxCollection.containsKey(nickSender)) {
//                    vBoxChat = vBoxCollection.get(nickSender);
//                } else {
//                    vBoxChat = new VBox();
//                    vBoxCollection.put(nickSender, vBoxChat);
//                }
//
//                int messageCounter = messageCounterCollection.get(nickSender);
//                int index = clientsList.getItems().indexOf(getNickCounter(nickSender,nickSenderCounter,messageCounter));
//                messageCounter++;
//                clientsList.getItems().set(index, getNickCounter(nickSender,nickSenderCounter,messageCounter));
//                messageCounterCollection.replace(nickSender,messageCounter);
//            }
//        }

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
        listView.scrollTo(listView.getItems().size());
    }

    public void clearWindow(){
        vBoxChat.getChildren().clear();
    }

    public void closeWindow(){
        Stage stage = Main.getPrimaryStage();
        stage.close();
    }

    public void sendMsg(){
        if (clientsList.getSelectionModel().getSelectedItem()!=null) {
            if (!textField.getText().isEmpty()) {
                try {
                    if (clientsList.getSelectionModel().getSelectedItem() != null) {
                        vBoxChat.setSpacing(10);
                        String nick = parseNick(clientsList.getSelectionModel().getSelectedItem());
                        out.writeUTF(nick + ":" + textField.getText());
                        textField.clear();
                        textField.requestFocus();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String parseNick(Object item) {
        HBox hBox = (HBox) item;

        String labelString = hBox.getChildren().get(0).toString();
        String[] parseLabelString = labelString.split("'",3);
        return parseLabelString[1];
    }

    public void tryToAuth(ActionEvent actionEvent) {
        connectToDataBase("/auth ");
    }

    public void tryToReg(ActionEvent actionEvent) {
        connectToDataBase("/reg ");
    }

    private void connectToDataBase(String operation){
        String login = loginField.getText();
        String password = passwordField.getText();
        String connectionString = "";
        boolean validData = false;

        if (operation.equals("/reg ")){
            String nick = nickField.getText();
            connectionString = login + " " + nick + " " + password;
            validData = !login.isEmpty() & !password.isEmpty() & !nick.isEmpty();
        }else if (operation.equals("/auth ")){
            connectionString = login + " " + password;
            validData = !login.isEmpty() & !password.isEmpty();
        }

        if (validData) {
            if (socket == null || socket.isClosed()) {
                connect();
            }
            try {

                out.writeUTF(operation + connectionString);
                loginField.clear();
                passwordField.clear();
                if (operation.equals("/reg ")) {
                    nickField.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlertWithHeaderText(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Внимание!");

        if (headerText.equals("Регистрация прошла успешно")){
            alert.setAlertType(Alert.AlertType.INFORMATION);
            alert.setTitle("Поздравляем!");
        }

        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        alert.showAndWait();
    }

    public void openRegistrationWindow(ActionEvent actionEvent){
        try {
            openWindow(regStage, "registration.fxml", "reg.jpg","Регистрация нового пользователя",350,40,"","");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openInfoWindow(){
        try {
            openWindow(infoStage,"sample_info.fxml","info.jpg","Info",350,100, "","");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openConfigWindow(){
        String[] configData = getConfigData();
        createConfigWindow(configData[0],configData[1]);
    }

    private String[] getConfigData(){
        if (Config.getConnection()==null){
            Config.connect();
        }
        return Config.getConfigData();
    }

    private void createConfigWindow(String s, String p) {
        Stage config = new Stage();
        config.setTitle("Network configuration");
        config.getIcons().add(new Image("file:images/network.jpg"));
        config.initModality(Modality.WINDOW_MODAL);
        config.initOwner(Main.getPrimaryStage());

        Label labelServer = new Label("Сервер");
        labelServer.setMinSize(150,20);
        Label labelPort = new Label("Порт");
        labelPort.setMinSize(150,20);

        TextField serv = new TextField(s);
        serv.setMinSize(150,20);
        TextField port = new TextField(p);
        port.setMinSize(150,20);

        Button buttonOk = new Button ("OK");
        buttonOk.setMinSize(150,20);
        Button buttonCancel = new Button ("Отмена");
        buttonCancel.setMinSize(150,20);

        buttonOk.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Config.setConfigData(serv.getText(),Integer.parseInt(port.getText()));
                configStage.close();
            }
        });
        buttonCancel.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                configStage.close();
            }
        });

        VBox vBoxMain = new VBox();
        HBox hBoxLabel = new HBox();
        HBox hBoxText = new HBox();
        HBox hBoxButtons = new HBox();
        hBoxLabel.getChildren().addAll(labelServer,labelPort);
        hBoxText.getChildren().addAll(serv,port);
        hBoxButtons.getChildren().addAll(buttonOk,buttonCancel);
        vBoxMain.getChildren().addAll(hBoxLabel,hBoxText,hBoxButtons);

        vBoxMain.setPadding(new Insets(10, 10, 10, 10));
        vBoxMain.setSpacing(7);
        Scene configScene = new Scene(vBoxMain, 320,110);
        config.setScene(configScene);
        configStage = config;
        config.show();
    }
    
    private void openWindow(Stage window, String fxmlFile, String iconFile, String title, int width, int height, String serv, String p) throws IOException {
        if (window.getScene()==null) {
            Image image = new Image("file:images/" + iconFile);

            Parent parent = FXMLLoader.load(getClass().getResource(fxmlFile));
            window.setTitle(title);
            window.getIcons().add(image);
            window.setScene(new Scene(parent, width, height));
            window.setResizable(false);
            window.initOwner(Main.getPrimaryStage());
            window.initModality(Modality.WINDOW_MODAL);
        }
        window.show();
    }

    public void selectClient(MouseEvent mouseEvent) {
        HBox hBox = (HBox) clientsList.getSelectionModel().getSelectedItem();
//        Label label = new Label("ttt");
//        label.getText();
        String labelString = hBox.getChildren().get(0).toString();
        String[] parseLabelString = labelString.split("'",3);
        String nickReceiver = parseLabelString[1];
        setVBox(nickReceiver);
        //где именно поставить? ведь подтягивать историю нужно только 1 раз!! когда вбокс еще не создан!
        //и что получается? нужно делать следующее: список клиентов должен быть всегда - независимо онлайн они или нет (если онлайн, то нужно просто как-то помечать это!)
        //и при открытии чата нужно сразу потдягивать историю ВСЕХ чатов, которые уже имел пользователь!
        //т.е. делать запросы в базу, создавать вбоксы, заносить в них историю в хронологическом порядке и уже иметь аозможность переключаться между ними!

        //значит при нажатии на пользователя уже не будет ситуации, при которой вбокс еще не будет создан. в списке пока будут все клиенты из базы
        //получается, что можно как в телеге будет отправлять сообщение клиенту офлайн, а ему после открытия чата все придет
        // вот только как реализовать непринятые сообщения?? ладно, об этом подумаю позже...

        //за то ситуация с историей (с запросом) несколько облегчается - не нужно теперь разные комбинации ников проверять... за то нужно группировать!
        //т.е. нужно выбрать все строки из таблицы, в которых фигурирует заданный ник (неважно в каком столбце).

        //важно еще продумать вот что - как все таки записывать историю переписки двух ников? думаю, что все таки лучше делать так: отправил сообщение - записал в историю
        //тогда хранение истории в таблице будет выглядеть аналгоично чату - ники будут чередоваться. скорее всего это облегчит их последующий разбор и добавление в вбокс

        //и как будет выглядеть запрос в базу? основой ляжет ник, пользователя, который открывает окно. т.е. нужны все строки из таблицы, где в одной из колонок фигурирует скажем ник1

        //а может поступить след образом? зачем делать именно с начала одну историю, а потом следующую и следующую и.т.д.?
        //записи выводятся в хронологическом порядке, ник, что справа - отправитель, ник слева - приемник. сразу создать коллекци. пришел ник, отличный от того, чью истории грузим -
        //проверка - есть в коллекции или нет. нет - создаем, есть подтягиваем из коллекции и заносим сообщение. следующий - так же... пришло из того же чата - значит опять ищем по
        //нику в коллекции и добавляем, из другого чата - ищем (создаем новый и добавляем) - вроде должно сработать

//        try {
//            out.writeUTF("/get_history " + nickname + " " + nickReceiver);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void setVBox(String nick) {
        if (nick.endsWith("*")) {
            int index = clientsList.getItems().indexOf(nick);
            nick = nick.replace("*", "");
            clientsList.getItems().set(index, nick);
        }

        if (vBoxCollection.containsKey(nick)) {
            vBoxChat = vBoxCollection.get(nick);
        }else {
            vBoxChat = new VBox();
            vBoxCollection.put(nick,vBoxChat);
        }
        splitPane.getItems().clear();
        splitPane.getItems().add(vBoxChat);
    }
}
