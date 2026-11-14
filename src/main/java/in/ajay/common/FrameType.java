package in.ajay.common;

import java.io.IOException;

public enum FrameType {
    AUTHENTICATION((byte) 1),
    AUTHENTICATION_RESPONSE((byte) 2),
    PUBLISHER_MESSAGE((byte) 3),
    PUBLISHER_MESSAGE_ACK((byte) 4),
    SUBSCRIBER_MESSAGE((byte) 5),
    SUBSCRIBER_MESSAGE_ACK((byte) 6),
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
