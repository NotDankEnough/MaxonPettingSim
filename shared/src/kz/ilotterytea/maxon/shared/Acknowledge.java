package kz.ilotterytea.maxon.shared;

import java.io.Serializable;

public record Acknowledge(Object payload) implements Serializable {
    @Override
    public String toString() {
        return "Acknowledge{" +
                "payload='" + payload + '\'' +
                '}';
    }
}
