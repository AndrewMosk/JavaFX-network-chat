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
        AuthService.saveHistory(nickSender,nickReceiver,message);

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
        broadcastClientsStatus(client.getNick(), true);
    }

    void unsubscribe(ClientHandler client){
        clients.remove(client);
        broadcastClientsStatus(client.getNick(), false);
    }
    private void broadcastClientsStatus(String nick, boolean online) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("/clientslist ");
//        for (ClientHandler client : clients) {
//            sb.append(client.getNick() + " ");
//        }
//        String out = sb.toString();
//        for (ClientHandler client : clients) {
//            client.sendMessage(out);
//        }

        //рассылает всем только свой статус
        //но здесь еще вот над чем нужно подумать - как только что подключившийся клиент будет узнавать - кто онлайн, а кто - нет?
        for (ClientHandler client : clients) {
            if (!client.getNick().equals(nick)) {
                client.sendMessage("/clientStatus " + nick + " " + online);
            }
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

    void sendHistory(String nickSender) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nickSender)) {
                String history = AuthService.getHistory(nickSender);
                client.sendMessage("/history " + history); //убрал из кавычек getClientList - случайно может добавил?
                return;
            }
        }
    }

    void sendClientList(String nickSender) {
        // отсюда я возьму список клиентов онлайн!!
        ArrayList<String> nicksOnline = new ArrayList<>();
        for (ClientHandler client : clients) {
            nicksOnline.add(client.getNick());
        }

        for (ClientHandler client : clients) {
            if (client.getNick().equals(nickSender)) {
                String clientList = AuthService.getClientList(nickSender, nicksOnline);
                client.sendMessage("/clientList " + clientList);
                return;
            }
        }
    }

}
