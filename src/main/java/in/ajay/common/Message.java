package in.ajay.common;

import java.util.Date;
import java.util.UUID;

public class Message {
    UUID Id; //unique id per message
    Date createdAt; // time at which this is created
    String topicId; //unique string to identify the topic; string would be more readable than an id
    Byte ChannelId; // useful for distinguishing channel with in connection
    int ttl; // how long broker should keep the message
    byte[] payload; // actual message payload
}