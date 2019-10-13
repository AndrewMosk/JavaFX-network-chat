package Server;

public class Test {
    public static void main(String[] args) {
        AuthService.connect();
        //если истина, то такой ник уже в черном списке, ложь - нет
        boolean flag = AuthService.addUserToBlackList("nick2", "nick3");
        System.out.println(flag);
        AuthService.disconnect();
    }
}
