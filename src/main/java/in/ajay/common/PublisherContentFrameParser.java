package in.ajay.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PublisherContentFrameParser {

    public static final int MIN_TOPIC_LENGTH = 10;
    public static final int MAX_TOPIC_LENGTH = 200;

    /*
     * Fixed frame fields:
     *
     * 4  frame length
     * 1  frame type
     * 1  channel ID
     * 16 message ID
     * 8  created at
     * 2  topic length
     * 16 publisher ID
     * 4  TTL
     * 4  payload length
     * 1  EOF
     */
    public static final int FIXED_FRAME_LENGTH = 57;

    private static final byte EOF_MARKER = 0x7F;
    private static final int maxPayloadLength = 1024*1024;

    /**
     * Encode object into binary frame.
     */
    public static byte[] encode(PublisherContentFrame frame) {

        byte[] topicBytes =
                frame.getTopicId().getBytes(StandardCharsets.UTF_8);

        byte[] payload = frame.getPayload();

        validateTopicLength(topicBytes.length);
        validatePayloadLength(payload.length);
        FrameIO.validateChannelId(frame.getChannelId());

        long totalLength =
                (long) FIXED_FRAME_LENGTH +
                        topicBytes.length +
                        payload.length;

        if (totalLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Frame too large");
        }

        int frameLength = (int) totalLength;

        ByteBuffer buffer = FrameIO.allocate(frameLength);

        buffer.putInt(frameLength);

        buffer.put(FrameType.PUBLISHER_MESSAGE.code());

        buffer.put(frame.getChannelId());

        FrameIO.writeUUID(buffer, frame.getMessageId());

        buffer.putLong(frame.getCreatedAt());

        buffer.putShort((short) topicBytes.length);

        buffer.put(topicBytes);

        FrameIO.writeUUID(buffer, frame.getPublisherId());

        buffer.putInt(frame.getTtl());

        buffer.putInt(payload.length);

        buffer.put(payload);

        buffer.put(EOF_MARKER);

        return buffer.array();
    }

    /**
     * Decode binary frame into object.
     */
    public static PublisherContentFrame decode(byte[] data) {

        if (data == null || data.length < FIXED_FRAME_LENGTH +
                MIN_TOPIC_LENGTH) {
            throw new IllegalArgumentException("Incomplete frame");
        }

        ByteBuffer buffer = FrameIO.allocate(data.length);
        buffer.put(data);
        buffer.flip();

        int frameLength = buffer.getInt();

        if (frameLength != data.length) {
            throw new IllegalArgumentException(
                    "Frame length mismatch. Expected: " +
                            frameLength + ", actual: " + data.length);
        }

        byte frameType = buffer.get();

        if (frameType != FrameType.PUBLISHER_MESSAGE.code()) {
            throw new IllegalArgumentException(
                    "Unexpected frame type: " +
                            Byte.toUnsignedInt(frameType));
        }

        UUID messageId = FrameIO.readUUID(buffer);

        byte channelId = buffer.get();

        long createdAt = buffer.getLong();

        int topicLength = Short.toUnsignedInt(buffer.getShort());

        validateTopicLength(topicLength);

        if (buffer.remaining() < topicLength + 1 + 16 + 4 + 4 + 1) {
            throw new IllegalArgumentException("Incomplete topic/frame");
        }

        byte[] topicBytes = new byte[topicLength];
        buffer.get(topicBytes);

        String topicId = new String(
                topicBytes, StandardCharsets.UTF_8);

        UUID publisherId = FrameIO.readUUID(buffer);

        int ttl = buffer.getInt();

        int payloadLength = buffer.getInt();

        validatePayloadLength(payloadLength);

        if (buffer.remaining() != payloadLength + 1) {
            throw new IllegalArgumentException(
                    "Invalid payload length or frame structure");
        }

        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        byte receivedEof = buffer.get();

        FrameIO.validateEof(receivedEof, EOF_MARKER);

        return new PublisherContentFrame(
                messageId,
                createdAt,
                topicId,
                channelId,
                publisherId,
                ttl,
                payload
        );
    }

    /**
     * Encode and write one frame to connection output stream.
     */
    public void writeFrame(
            PublisherContentFrame frame,
            OutputStream output) throws IOException {

        FrameIO.write(output, encode(frame));
    }

    private static void validateTopicLength(int length) {
        if (length < MIN_TOPIC_LENGTH || length > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException(
                    "Topic length must be between 10 and 200 bytes. Got: " +
                            length);
        }
    }

    private static void validatePayloadLength(int length) {
        if (length < 0 || length > maxPayloadLength) {
            throw new IllegalArgumentException(
                    "Invalid payload length: " + length);
        }
    }
}
