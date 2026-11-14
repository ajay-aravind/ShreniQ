package in.ajay.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public final class FrameIO {

    private FrameIO() {
    }

    //todo move all the helper methods in parser and connection handler and this class to a single class
    static ByteBuffer allocate(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
    }

    static void writeUUID(ByteBuffer buffer, UUID uuid) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }

    static UUID readUUID(ByteBuffer buffer) {
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }

    static void validateChannelId(int channelId) {
        if (channelId < 0 || channelId > 255) {
            throw new IllegalArgumentException(
                    "Invalid channel ID: " + channelId);
        }
    }

    static void validateEof(byte received, byte expected) {
        if (received != expected) {
            throw new IllegalArgumentException(
                    "Invalid EOF marker: " +
                            Byte.toUnsignedInt(received));
        }
    }

    static void write(OutputStream output, byte[] frame)
            throws IOException {
        output.write(frame);
    }
}
