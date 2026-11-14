package in.ajay.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PublisherContentFrameAckParser {

    private static final int FRAME_LENGTH = 39;

    private static final byte EOF_MARKER = 0x7F;

    /**
     * Encode acknowledgement object into binary frame.
     */
    public static byte[] encode(PublisherContentFrameAck frame) {

        FrameIO.validateChannelId((byte)frame.getChannelId());

        ByteBuffer buffer = FrameIO.allocate(FRAME_LENGTH);

        buffer.putInt(FRAME_LENGTH);

        buffer.put(FrameType.PUBLISHER_MESSAGE_ACK.code());

        FrameIO.writeUUID(buffer, frame.getMessageId());

        FrameIO.writeUUID(buffer, frame.getPublisherId());

        buffer.put((byte) frame.getChannelId());

        buffer.put(PublisherContentFrameAckParser.EOF_MARKER);

        return buffer.array();
    }

    /**
     * Decode binary frame into acknowledgement object.
     */
    public static PublisherContentFrameAck decode(byte[] data) {

        if (data == null || data.length != FRAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid acknowledgement frame length");
        }

        ByteBuffer buffer = FrameIO.allocate(FRAME_LENGTH);
        buffer.put(data);
        buffer.flip();

        int frameLength = buffer.getInt();

        if (frameLength != FRAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid frame length: " + frameLength);
        }

        byte frameType = buffer.get();

        if (frameType != FrameType.PUBLISHER_MESSAGE_ACK.code()) {
            throw new IllegalArgumentException(
                    "Unexpected frame type: " +
                            Byte.toUnsignedInt(frameType));
        }

        UUID messageId = FrameIO.readUUID(buffer);

        UUID publisherId = FrameIO.readUUID(buffer);

        int channelId = Byte.toUnsignedInt(buffer.get());

        byte receivedEof = buffer.get();

        FrameIO.validateEof(receivedEof, EOF_MARKER);

        return new PublisherContentFrameAck(
                messageId,
                publisherId,
                channelId
        );
    }
}
