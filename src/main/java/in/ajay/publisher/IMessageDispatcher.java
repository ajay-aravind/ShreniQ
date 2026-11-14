package in.ajay.publisher;

import in.ajay.common.Message;

import java.io.IOException;
import java.util.UUID;

public interface IMessageDispatcher {
    void sendMessage(Message message, UUID publisherId, int channelId) throws IOException;
}
