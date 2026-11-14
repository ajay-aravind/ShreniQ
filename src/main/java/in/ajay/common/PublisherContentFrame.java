package in.ajay.common;

import java.util.UUID;

public class PublisherContentFrame {

    private final UUID messageId;
    private final long createdAt;
    private final String topicId;
    private final byte channelId;
    private final UUID publisherId;
    private final int ttl;
    private final byte[] payload;

    public PublisherContentFrame(
            UUID messageId,
            long createdAt,
            String topicId,
            byte channelId,
            UUID publisherId,
            int ttl,
            byte[] payload) {

        this.messageId = messageId;
        this.createdAt = createdAt;
        this.topicId = topicId;
        this.channelId = channelId;
        this.publisherId = publisherId;
        this.ttl = ttl;
        this.payload = payload.clone();
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

    public UUID getPublisherId() {
        return publisherId;
    }

    public int getTtl() {
        return ttl;
    }

    public byte[] getPayload() {
        return payload.clone();
    }

    @Override
    public String toString() {
        return "PublisherContentFrame{" +
                "ttl=" + ttl +
                ", publisherId=" + publisherId +
                ", channelId=" + channelId +
                ", topicId='" + topicId + '\'' +
                ", createdAt=" + createdAt +
                ", messageId=" + messageId +
                '}';
    }
}
