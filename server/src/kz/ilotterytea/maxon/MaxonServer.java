package kz.ilotterytea.maxon;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class MaxonServer extends WebSocketServer {
    private static MaxonServer instance;

    private final Logger log;

    private MaxonServer() {
        super(new InetSocketAddress(31084));
        this.log = LoggerFactory.getLogger(MaxonServer.class);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("{} connected!", conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("Connection {} has been closed! ({} {} {})", conn, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("{} says {}", conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Something went wrong", ex);
    }

    @Override
    public void onStart() {
        log.info("Running the server on port {}!", getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    public static MaxonServer getInstance() {
        if (instance == null) instance = new MaxonServer();
        return instance;
    }
}
