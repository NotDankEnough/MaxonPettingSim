package kz.ilotterytea.maxon;

public class ServerLauncher {
    public static void main(String[] args) {
        MaxonServer server = MaxonServer.getInstance();
        server.start();
    }
}
