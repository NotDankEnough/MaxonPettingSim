package kz.ilotterytea.maxon.session;

import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.screens.MenuScreen;
import kz.ilotterytea.maxon.shared.Acknowledge;
import kz.ilotterytea.maxon.shared.Identity;
import kz.ilotterytea.maxon.shared.exceptions.PlayerKickException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;

public class SessionClient extends WebSocketClient {
    private final Logger log;
    private final MaxonGame game;

    public SessionClient() {
        super(URI.create(MaxonConstants.SESSION_WSS_URL));
        this.log = LoggerFactory.getLogger(SessionClient.class);
        this.game = MaxonGame.getInstance();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected!");
        updateIdentity();
    }

    @Override
    public void onMessage(String message) {
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            // Deserialize the object
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes.array());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();

            if (obj instanceof Acknowledge acknowledge) SessionHandlers.handleAcknowledge(acknowledge);
            else if (obj instanceof PlayerKickException exception) throw exception;
        } catch (PlayerKickException e) {
            log.info("Kicked out!", e);
        } catch (Exception e) {
            log.error("An exception was thrown while processing message", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Connection closed! Reason: {} {}", code, reason);
        game.getIdentityClient().invalidateToken();
        if (!game.getScreen().getClass().equals(MenuScreen.class)) {
            game.setScreen(new MenuScreen());
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("Failed to connect", ex);
    }

    @Override
    public void connect() {
        super.connect();
    }

    public void updateIdentity() {
        IdentityClient identityClient = game.getIdentityClient();

        Identity identity = new Identity(identityClient.getClientToken(), identityClient.getAccessToken());

        send(identity);
    }

    public void send(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            send(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to serialize and send an object", e);
        }
    }
}
