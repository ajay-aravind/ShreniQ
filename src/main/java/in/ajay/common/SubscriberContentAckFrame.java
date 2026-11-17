package in.ajay.common;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class SubscriberContentAckFrame {

    public static final int FRAME_LENGTH = 4 + 1 + 16 + 16 + 1 + 1;

    private final byte frameType;
    private final UUID messageId;
    private final UUID subscriberId;
    private final byte channelId;
    private final byte endMarker;

    public SubscriberContentAckFrame(
            byte frameType,
            UUID messageId,
            UUID subscriberId,
            byte channelId,
            byte endMarker) {

        if (messageId == null || subscriberId == null) {
            throw new IllegalArgumentException("UUID fields cannot be null");
        }

        this.frameType = frameType;
        this.messageId = messageId;
        this.subscriberId = subscriberId;
        this.channelId = channelId;
        this.endMarker = endMarker;
    }

    public static byte[] encode(SubscriberContentAckFrame frame) {

        ByteBuffer buffer = ByteBuffer.allocate(FRAME_LENGTH)
                .order(ByteOrder.BIG_ENDIAN);

        // Frame length
        buffer.putInt(FRAME_LENGTH);

        // Frame type
        buffer.put(frame.frameType);

        // Message ID
        FrameIO.writeUUID(buffer, frame.messageId);

        // Subscriber ID
        FrameIO.writeUUID(buffer, frame.subscriberId);

        // Channel ID
        buffer.put(frame.channelId);

        // End marker
        buffer.put(frame.endMarker);

        return buffer.array();
    }

    public static SubscriberContentAckFrame decode(byte[] data) {

        if (data == null || data.length != FRAME_LENGTH) {
            throw new IllegalArgumentException("Invalid frame size");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN);

        // Frame length
        int frameLength = buffer.getInt();

        if (frameLength != FRAME_LENGTH) {
            throw new IllegalArgumentException("Invalid frame length");
        }

        // Frame type
        byte frameType = buffer.get();

        // Message ID
        UUID messageId = FrameIO.readUUID(buffer);

        // Subscriber ID
        UUID subscriberId = FrameIO.readUUID(buffer);

        // Channel ID
        byte channelId = buffer.get();

        // End marker
        byte endMarker = buffer.get();

        return new SubscriberContentAckFrame(
                frameType,
                messageId,
                subscriberId,
                channelId,
                endMarker
        );
    }

    public byte getFrameType() {
        return frameType;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }

    public byte getChannelId() {
        return channelId;
    }

    public byte getEndMarker() {
        return endMarker;
    }
}
