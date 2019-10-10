package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    void broadcastMessage(String msg){
        for (ClientHandler client: clients) {
            client.sendMessage(msg);
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
    }

    void unsubscribe(ClientHandler client){
        clients.remove(client);
    }

    void singleMessage(String msg, String sourceNick, String recipientNick) {
        for (ClientHandler client: clients) {
            if (client.getNick().equals(recipientNick) || client.getNick().equals(sourceNick)){
                client.sendMessage(sourceNick + ": " + msg);
            }
        }
    }
}
