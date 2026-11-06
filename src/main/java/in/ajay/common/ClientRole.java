package in.ajay.common;

import java.io.IOException;

public enum ClientRole {
    PUBLISHER(1),
    SUBSCRIBER(2);

    private final int code;

    ClientRole(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ClientRole fromCode(int code) throws IOException {
        for (ClientRole command : values()) {
            if (command.code == code) {
                return command;
            }
        }
        throw new IOException("Unknown client role: " + code);
    }
}
