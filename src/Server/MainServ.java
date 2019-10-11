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

    void broadcastMessage(String msg){
        String[] tokens = msg.split(":");
        ArrayList<String> blacklist = AuthService.getBlacklist(tokens[0]);
        for (ClientHandler client: clients) {
            //проверка на черный список...
            //как лучше реализовать? конечно, лучше было бы не дергать базу по каждому нику, а сделать все сразу
            //получить весь черный списиок по данному клиенту  и проверять включен ли ник текущего клиента в список, и если нет - отправлять!
            //использовать коллекцию ArrayList<String> arr = new ArrayList<>(); у нее есть метод contains


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

    boolean addToBlackList(String nick, String nickToBlackList){
        //чтобы не проверять есть ли пришедший ник уже в списке у пользователя, соотвествующим образом настроил БД - у
        // колонки id_black установил свойство уникальности и в случае конфликта просто перезаписываю строку
        return AuthService.addUserToBlackList(nick,nickToBlackList);


    }

}
