package kz.ilotterytea.maxon;

import kz.ilotterytea.maxon.shared.Identity;
import org.java_websocket.WebSocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;

public class PlayerConnection {
    private static int TOTAL_CONNECTION_IDS = 0;

    private final int id;
    private final WebSocket connection;
    private Identity identity;

    private final Timestamp connectedTimestamp;

    public PlayerConnection(WebSocket connection) {
        this.connection = connection;
        this.connectedTimestamp = new Timestamp(System.currentTimeMillis());

        this.id = TOTAL_CONNECTION_IDS;
        TOTAL_CONNECTION_IDS++;
    }

    public int getId() {
        return id;
    }

    public WebSocket getConnection() {
        return connection;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Timestamp getConnectedTimestamp() {
        return connectedTimestamp;
    }

    public void send(Object object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(object);
        } catch (IOException ignored) {

        }
        
        this.connection.send(byteArrayOutputStream.toByteArray());
    }

    @Override
    public String toString() {
        return "PlayerConnection{" +
                "id=" + id +
                '}';
    }
}
