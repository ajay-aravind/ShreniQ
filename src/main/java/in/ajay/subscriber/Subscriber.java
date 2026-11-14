package in.ajay.subscriber;

import in.ajay.common.Message;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public class Subscriber {
    private UUID clientId;
    private int channelId;
    private UUID publisherId;
    private List<Message> messageList;
    private Logger logger;

    public Subscriber(UUID clientId, int channelId, UUID publisherId) {
        this.clientId = clientId;
        this.channelId = channelId;
        this.publisherId = publisherId;
    }


}
