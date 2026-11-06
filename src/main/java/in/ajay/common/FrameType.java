package in.ajay.common;

import java.io.IOException;

public enum FrameType {
    AUTHENTICATION(1),
    AUTHENTICATION_RESPONSE(2),
    PUBLISHER_MESSAGE(3),
    PUBLISHER_MESSAGE_ACK(4),
    SUBSCRIBER_MESSAGE(5),
    SUBSCRIBER_MESSAGE_ACK(6),
    ;

    private final byte code;

    FrameType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static FrameType fromCode(byte code) throws IOException {
        for (FrameType command : values()) {
            if (command.code == code) {
                return command;
            }
        }
        throw new IOException("Unknown FrameType: " + code);
    }
}
