package Sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
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
    VBox vBox;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;
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
        }else {
            String newTitle = stage.getTitle() + " " + nick;
            stage.setTitle(newTitle);
        }
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS,PORT);
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
                                    vBox.getChildren().clear();
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
        vBox.setFillWidth(false);
        vBox.setSpacing(10);
        vBox.getChildren().add(new TextMessage(msg));
    }

    public void clearWindow(){
        vBox.getChildren().clear();
    }

    public void closeWindow(){
        Stage stage = Main.getPrimaryStage();
        stage.close();
    }

    public void sendMsg(){
        if (!textField.getText().isEmpty()) {
            try {
                vBox.setFillWidth(false);
                vBox.setSpacing(10);
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
            openWindow(regStage, "registration.fxml", "reg.jpg","Регистрация нового пользователя",350,40);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openInfoWindow(){
        try {
            openWindow(infoStage,"sample_info.fxml","info.jpg","Info",350,100);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openWindow(Stage window, String fxmlFile, String iconFile, String title, int width, int height) throws IOException {
        if (window.getScene()==null) {
            Image image = new Image("file:images/" + iconFile);

            Parent parent = FXMLLoader.load(getClass().getResource(fxmlFile));
            window.setTitle(title);
            window.getIcons().add(image);
            window.setScene(new Scene(parent, width, height));
            window.setResizable(false);
            window.initOwner(Main.getPrimaryStage());
            window.initModality(Modality.WINDOW_MODAL);
            window.show();
        }else {
            window.show();
        }
    }
}
