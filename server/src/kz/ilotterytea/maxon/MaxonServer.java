package kz.ilotterytea.maxon;

import kz.ilotterytea.maxon.shared.Identity;
import kz.ilotterytea.maxon.shared.exceptions.PlayerKickException;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Optional;

public class MaxonServer extends WebSocketServer {
    private static MaxonServer instance;

    private final Logger log;
    private final ArrayList<PlayerConnection> connections;

    private MaxonServer() {
        super(new InetSocketAddress(31084));
        this.log = LoggerFactory.getLogger(MaxonServer.class);
        this.connections = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        PlayerConnection connection = new PlayerConnection(conn);
        log.info("{} ({}) connected!", connection, conn.getRemoteSocketAddress().getAddress().getHostAddress());
        this.connections.add(connection);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Optional<PlayerConnection> connection = this.connections.stream().filter((x) -> x.getConnection().equals(conn)).findFirst();
        if (connection.isPresent()) {
            log.info("{} has left! Reason: {} {}", connection.get(), code, reason);
            this.connections.remove(connection.get());
        } else {
            log.info("Unknown connection was closed! Reason: {} {}", code, reason);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        this.connections.removeIf((x) -> x.getConnection().equals(conn));
        conn.send("Invalid input.");
        conn.close(CloseFrame.UNEXPECTED_CONDITION);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Optional<PlayerConnection> playerConnection = this.connections.stream().filter((x) -> x.getConnection().equals(conn)).findFirst();

        if (playerConnection.isEmpty()) {
            conn.close(5001, "Your PlayerConnection was not found!");
            return;
        }

        PlayerConnection c = playerConnection.get();

        try {
            // Deserialize the object
            ByteArrayInputStream bais = new ByteArrayInputStream(message.array());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();

            if (obj instanceof Identity identity) ServerHandlers.handleIdentity(c, identity);
            else kickConnection(c, PlayerKickException.internalServerError());
        } catch (PlayerKickException e) {
            kickConnection(c, e);
        } catch (Exception e) {
            log.error("An exception was thrown while processing message", e);
            kickConnection(c, PlayerKickException.internalServerError());
        }
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

    public void kickConnection(PlayerConnection connection, PlayerKickException e) {
        connection.send(e);
        connection.getConnection().close();
        this.connections.remove(connection);
        log.debug("Kicked out {}! Reason: {}", connection, e.getMessage());
    }

    public static MaxonServer getInstance() {
        if (instance == null) instance = new MaxonServer();
        return instance;
    }

    public ArrayList<PlayerConnection> getPlayerConnections() {
        return connections;
    }
}
