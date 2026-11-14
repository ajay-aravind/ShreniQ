package in.ajay.common;

import java.util.UUID;

public class PublisherContentFrameAck {

    private final UUID messageId;
    private final UUID publisherId;
    private final int channelId;

    public PublisherContentFrameAck(
            UUID messageId,
            UUID publisherId,
            int channelId) {

        this.messageId = messageId;
        this.publisherId = publisherId;
        this.channelId = channelId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getPublisherId() {
        return publisherId;
    }

    public int getChannelId() {
        return channelId;
    }

    @Override
    public String toString() {
        return "PublisherContentFrameAck{" +
                "messageId=" + messageId +
                ", publisherId=" + publisherId +
                ", channelId=" + channelId +
                '}';
    }
}
