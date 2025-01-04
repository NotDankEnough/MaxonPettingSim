package kz.ilotterytea.maxon;

import kz.ilotterytea.maxon.shared.Acknowledge;
import kz.ilotterytea.maxon.shared.Identity;
import kz.ilotterytea.maxon.shared.exceptions.PlayerKickException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandlers {
    private static final Logger log = LoggerFactory.getLogger(ServerHandlers.class);
    private static final MaxonServer server = MaxonServer.getInstance();

    public static void handleIdentity(PlayerConnection connection, Identity identity) {
        if (server.getPlayerConnections()
                .stream()
                .filter((x) -> x.getIdentity() != null)
                .anyMatch((x) -> x.getIdentity().equals(identity) && x.getId() != connection.getId())
        ) {
            server.kickConnection(connection, PlayerKickException.loggedIn());
            return;
        }

        connection.setIdentity(identity);
        connection.send(new Acknowledge(identity));
        log.debug("Successfully identified {} for {}", identity, connection);
    }
}
