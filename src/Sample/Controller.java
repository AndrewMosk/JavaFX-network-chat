package Sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    TextField textField;
    @FXML
    MenuItem close;
    @FXML
    MenuItem clear;
    @FXML
    MenuItem about;
    @FXML
    VBox vBox1;
    @FXML
    VBox vBox2;

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

    String nickname = "";


    private void setAuthorized(boolean isAuthorized){
        this.isAuthorized = isAuthorized;
        if (!isAuthorized){
            VBoxUpperPanel.setVisible(true);
            VBoxUpperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
        }else {
            VBoxUpperPanel.setVisible(false);
            VBoxUpperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
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
                                setAuthorized(true);
                                Platform.runLater(() -> setNewTitle(tokens[1]));
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
                                    vBox1.getChildren().clear();
                                });

                            } else if (msg.equals("Ошибка добавления в черный список")) {
                                Platform.runLater(() -> showAlertWithHeaderText(msg, "Нельзя добавить в черный список самого себя"));
                            } else if (msg.startsWith("/blacklist")) {
                                String[] tokens = msg.split(" ", 3);
                                Platform.runLater(() -> showAlertWithHeaderText(tokens[2], "Пользователь " + tokens[1]));
                            } else {
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

    private void addText(String msg){
//        vBox1.setFillWidth(false);
//        vBox1.setSpacing(10);
//        VBox vb = new VBox();
//        vb.getChildren().add(new TextMessage(msg));
//        String[] tokens = msg.split(":",2);
//
//        if (tokens[0].equals(nickname)) {
//            vb.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
//        }else {
//            vb.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
//        }
//        vBox1.getChildren().add(vb);


        //ну вроде как с splitPane получилось более-менее... нужно подумать - как ее допилить... может вообще скролпэйн убрать и заменить на сплит?
        //два вбокса в параллель! в вбокс - шбокс, в него - два вбокса! в каждом из них разнесение по разным сторонам!

//        VBox vb = new VBox();
//        vb.getChildren().add(new TextMessage(msg));
//
//        SplitPane splitPane = new SplitPane();
//
//        StackPane child1 = new StackPane();
//        StackPane child2 = new StackPane();
//
//        String[] tokens = msg.split(":",2);
//
//        if (tokens[0].equals(nickname)) {
//            child1.getChildren().add(vb);
//        }else {
//            child2.getChildren().add(vb);
//        }
//
//        splitPane.getItems().addAll(child1, child2);
//        vBox1.getChildren().add(splitPane);


        VBox vb = new VBox();
        //vb.getChildren().add(new TextMessage(msg));

        HBox hb = new HBox();
        VBox vb1 = new VBox();
        VBox vb2 = new VBox();

        String[] tokens = msg.split(":",2);

        if (tokens[0].equals(nickname)) {
            vb1.getChildren().add(new TextMessage(msg));
            vb.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }else {
            vb2.getChildren().add(new TextMessage(msg));
            vb.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        hb.getChildren().addAll(vb1, vb2);
        vb.getChildren().add(hb);

        vBox1.getChildren().add(vb);

    }

    public void clearWindow(){
        vBox1.getChildren().clear();
    }

    public void closeWindow(){
        Stage stage = Main.getPrimaryStage();
        stage.close();
    }

    public void sendMsg(){
        if (!textField.getText().isEmpty()) {
            try {
//                vBox1.setFillWidth(false);
//                vBox1.setSpacing(10);
                out.writeUTF(textField.getText());
                textField.clear();
                textField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
}
