package kz.ilotterytea.maxon.shared;

import java.io.Serializable;

public record Identity(String accessToken, String clientToken) implements Serializable {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identity i) {
            return i.accessToken.equals(accessToken) && i.clientToken.equals(clientToken);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Identity{" +
                "clientToken='" + clientToken + '\'' +
                ", accessToken='" + accessToken + '\'' +
                '}';
    }
}
