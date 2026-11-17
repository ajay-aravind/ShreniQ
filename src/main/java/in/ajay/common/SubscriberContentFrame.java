package in.ajay.common;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SubscriberContentFrame {

    private static final int UUID_LENGTH = 16;
    private static final int FIXED_FRAME_LENGTH =
            4 + 1 + UUID_LENGTH + 8 + 2 + 1 + UUID_LENGTH + 4 + 1;

    private static final int MIN_TOPIC_LENGTH = 10;
    private static final int MAX_TOPIC_LENGTH = 200;
    private static final byte EOF_MARKER = 0x7F;

    private final byte frameType;
    private final UUID messageId;
    private final long createdAt;
    private final String topicId;
    private final byte channelId;
    private final UUID subscriberId;
    private final byte[] payload;
    private final byte endMarker = EOF_MARKER;

    public SubscriberContentFrame(
            byte frameType,
            UUID messageId,
            long createdAt,
            String topicId,
            byte channelId,
            UUID subscriberId,
            byte[] payload) {

        if (messageId == null || subscriberId == null ||
                topicId == null || payload == null) {
            throw new IllegalArgumentException("Frame fields cannot be null");
        }

        int topicLength = topicId.getBytes(StandardCharsets.UTF_8).length;

        if (topicLength < MIN_TOPIC_LENGTH || topicLength > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException(
                    "Topic length must be between 10 and 200 bytes");
        }

        this.frameType = frameType;
        this.messageId = messageId;
        this.createdAt = createdAt;
        this.topicId = topicId;
        this.channelId = channelId;
        this.subscriberId = subscriberId;
        this.payload = payload.clone();
    }

    public static byte[] encode(SubscriberContentFrame frame) {

        byte[] topicBytes = frame.topicId.getBytes(StandardCharsets.UTF_8);

        int frameLength = FIXED_FRAME_LENGTH
                + topicBytes.length
                + frame.payload.length;

        ByteBuffer buffer = ByteBuffer.allocate(frameLength)
                .order(ByteOrder.BIG_ENDIAN);

        // Frame length
        buffer.putInt(frameLength);

        // Frame type
        buffer.put(frame.frameType);

        // Message ID
        FrameIO.writeUUID(buffer, frame.messageId);

        // Created at
        buffer.putLong(frame.createdAt);

        // Topic length
        buffer.putShort((short) topicBytes.length);

        // Topic ID
        buffer.put(topicBytes);

        // Channel ID
        buffer.put(frame.channelId);

        // Subscriber ID
        FrameIO.writeUUID(buffer, frame.subscriberId);

        // Payload length
        buffer.putInt(frame.payload.length);

        // Payload
        buffer.put(frame.payload);

        // End marker
        buffer.put(frame.endMarker);

        return buffer.array();
    }

    public static SubscriberContentFrame decode(byte[] data) {

        if (data == null || data.length < FIXED_FRAME_LENGTH + MIN_TOPIC_LENGTH) {
            throw new IllegalArgumentException("Incomplete frame");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN);

        // Frame length
        int frameLength = buffer.getInt();

        if (frameLength != data.length) {
            throw new IllegalArgumentException("Invalid frame length");
        }

        // Frame type
        byte frameType = buffer.get();

        // Message ID
        UUID messageId = FrameIO.readUUID(buffer);

        // Created at
        long createdAt = buffer.getLong();

        // Topic length
        int topicLength = Short.toUnsignedInt(buffer.getShort());

        if (topicLength < MIN_TOPIC_LENGTH || topicLength > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException("Invalid topic length");
        }

        if (buffer.remaining() < topicLength + 1 + UUID_LENGTH + 4 + 1) {
            throw new IllegalArgumentException("Incomplete frame");
        }

        // Topic ID
        byte[] topicBytes = new byte[topicLength];
        buffer.get(topicBytes);
        String topicId = new String(topicBytes, StandardCharsets.UTF_8);

        // Channel ID
        byte channelId = buffer.get();

        // Subscriber ID
        UUID subscriberId = FrameIO.readUUID(buffer);

        // Payload length
        int payloadLength = buffer.getInt();

        if (payloadLength < 0 ||
                buffer.remaining() != payloadLength + 1) {
            throw new IllegalArgumentException("Invalid payload length");
        }

        // Payload
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        // End marker
        byte endMarker = buffer.get();

        return new SubscriberContentFrame(
                frameType,
                messageId,
                createdAt,
                topicId,
                channelId,
                subscriberId,
                payload
        );
    }

    // Getters

    public byte getFrameType() {
        return frameType;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTopicId() {
        return topicId;
    }

    public byte getChannelId() {
        return channelId;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }

    public byte[] getPayload() {
        return payload.clone();
    }

    public byte getEndMarker() {
        return endMarker;
    }
}

