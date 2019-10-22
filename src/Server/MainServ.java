package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

class MainServ {
    private Vector<ClientHandler> clients;

    MainServ() {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        int port = 8189;

        try {
            AuthService.connect();
            server = new ServerSocket(port);
            System.out.println("Сервер запущен");

            while (true){
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        AuthService.disconnect();
    }

    void sendMessage(String msg){
        String[] msfTokens = msg.split(":", 3);
        String nickSender = msfTokens[0];
        String nickReceiver = msfTokens[1];
        String message = msfTokens[2];

        //String[] tokens = msg.split(":");
        //черный список того, кто отправялет
        ArrayList<String> blacklist = AuthService.getBlacklist(nickSender);
        //проверяю есть ли отправитель в чьем-нибудт черном списке
        ArrayList<String> InverseBlacklist = AuthService.getInverseBlacklist(nickSender);

        for (ClientHandler client: clients) {
            if (client.getNick().equals(nickReceiver)) {
                if (!blacklist.contains(client.getNick()) & !InverseBlacklist.contains(client.getNick())) {
                    client.sendMessage(nickSender + ": " + message);
                }
            }
            if (client.getNick().equals(nickSender)) {
                client.sendMessage(nickSender + ": " + message);
            }
        }

    }

    boolean checkNick(String nick){
        boolean result = true;
        for (ClientHandler client: clients) {
            if (client.getNick().equals(nick)){
                result = false;
                break;
            }
        }
        return result;
    }

    void subscribe(ClientHandler client){
        clients.add(client);
        broadcastClientsList();
    }

    void unsubscribe(ClientHandler client){
        clients.remove(client);
        broadcastClientsList();
    }
    void broadcastClientsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientslist ");
        for (ClientHandler client : clients) {
            sb.append(client.getNick() + " ");
        }
        String out = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(out);
        }
    }

    void singleMessage(String msg, String sourceNick, String recipientNick) {
        for (ClientHandler client: clients) {
            if (client.getNick().equals(recipientNick) || client.getNick().equals(sourceNick)){
                client.sendMessage(sourceNick + ": " + msg);
            }
        }
    }

    boolean addToBlackList(String nick, String nickToBlackList){
        return AuthService.addUserToBlackList(nick,nickToBlackList);
    }

    void sendHistory(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick)) {
                String history = AuthService.getHistory();
                client.sendMessage(history);
                return;
            }
        }
    }

}
