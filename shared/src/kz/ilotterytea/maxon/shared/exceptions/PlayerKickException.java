package kz.ilotterytea.maxon.shared.exceptions;

public class PlayerKickException extends RuntimeException {
    public PlayerKickException(String message) {
        super(message);
    }

    public static PlayerKickException loggedIn() {
        return new PlayerKickException("You logged in from another location");
    }

    public static PlayerKickException internalServerError() {
        return new PlayerKickException("Internal Server Error");
    }
}
