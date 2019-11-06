package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private MainServ serv;
    private String nick;

    String getNick() {
        return nick;
    }

    ClientHandler(MainServ serv, Socket socket) {
        try {
            this.socket = socket;
            this.serv = serv;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true){
                            String str = in.readUTF();
                            if (str.startsWith("/auth")){
                                String[] tokens = str.split(" ");
                                String currentNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                if (currentNick!=null) {
                                    if (serv.checkNick(currentNick)) {
                                        sendMessage("/authOk " + currentNick);
                                        nick = currentNick;
                                        serv.subscribe(ClientHandler.this);
                                        break;
                                    }else {
                                        //sendMessage("Попытка повторного входа");
                                        sendMessage("/reentry");
                                    }
                                }else {
                                    sendMessage("/authentication_error");
                                }
                            }else if (str.startsWith("/reg")){
                                String[] tokens = str.split(" ");
                                boolean validRegData = AuthService.checkLoginAndNick(tokens[1], tokens[2]);
                                if (validRegData){
                                    if (AuthService.regNewUser(tokens[1], tokens[2], tokens[3])){
                                        sendMessage("/successful_registration");
                                    }else {
                                        sendMessage("/registration_failed");
                                    }
                                }else {
                                    sendMessage("/registration_denied");
                                }
                            }
                        }

                        while (true){
                            String msg = in.readUTF();
                            if (msg.equalsIgnoreCase("/end")) {
                                sendMessage("/end");
                                break;
                            }else if (msg.startsWith("/w")) {
                                //сообщение конкретному клиенту
                                String[] tokens = msg.split(" ", 3);
                                serv.singleMessage(tokens[2], nick, tokens[1]);
                            }else if (msg.startsWith("/blacklist")){
                                String[] tokens = msg.split(" ", 3);
                                if (nick.equals(tokens[1])){
                                    sendMessage("Ошибка добавления в черный список");
                                }else {
                                    if (serv.addToBlackList(nick, tokens[1])) {
                                        sendMessage("/blacklist " + tokens[1] + " Пользователь добавлен в ченый список");
                                    }else {
                                        sendMessage("/blacklist " + tokens[1] + " Не найден");
                                    }
                                }
                            }else if (msg.startsWith("/get_history ")) {
                                String[] tokens = msg.split(" ", 2);
                                serv.sendHistory(tokens[1]);
                            }else if (msg.startsWith("/get_clientList ")) {
                                String[] tokens = msg.split(" ", 2);
                                serv.sendClientList(tokens[1]);
                            }
                            else {
                                serv.sendMessage(nick + ":" + msg);
                            }
                        }
                    }catch (IOException | SQLException e){
                        e.printStackTrace();
                    }finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    serv.unsubscribe(ClientHandler.this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
