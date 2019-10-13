package Sample;

public class Test {
    public static void main(String[] args) {
        Config.connect();

        String[] configData = Config.getConfigData();
        for (String el:configData) {
            System.out.println(el);
        }

        Config.setConfigData("smalltower", 8189);

        String[] newConfigData = Config.getConfigData();
        for (String el:newConfigData) {
            System.out.println(el);
        }

        Config.disconnect();
    }
}
