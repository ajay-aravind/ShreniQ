package in.ajay.common;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class Message {
    UUID Id; //unique id per message
    long createdAt; // time at which this is created, in epoch milli seconds
    String topicId; //unique string to identify the topic; string would be more readable than an id
    Byte ChannelId; // useful for distinguishing channel with in connection
    int ttl; // how long broker should keep the message
    byte[] payload; // actual message payload

    public Message(UUID id, long createdAt, String topicId, Byte channelId, int ttl, byte[] payload) {
        Id = id;
        this.createdAt = createdAt;
        this.topicId = topicId;
        ChannelId = channelId;
        this.ttl = ttl;
        this.payload = payload;
    }

    public UUID getId() {
        return Id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTopicId() {
        return topicId;
    }

    public Byte getChannelId() {
        return ChannelId;
    }

    public int getTtl() {
        return ttl;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "ttl=" + ttl +
                ", ChannelId=" + ChannelId +
                ", topicId='" + topicId + '\'' +
                ", createdAt=" + createdAt +
                ", Id=" + Id +
                ", payload="+ new String(this.payload, StandardCharsets.UTF_8) +
                '}';
    }
}