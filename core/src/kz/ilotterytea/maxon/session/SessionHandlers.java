package kz.ilotterytea.maxon.session;

import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.shared.Acknowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHandlers {
    private static final Logger log = LoggerFactory.getLogger(SessionHandlers.class);
    private static final SessionClient client = MaxonGame.getInstance().getSessionClient();

    public static void handleAcknowledge(Acknowledge acknowledge) {
        log.info("alright: {}", acknowledge);
    }
}
