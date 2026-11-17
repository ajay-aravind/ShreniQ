package in.ajay.shreniQ.core;

import in.ajay.common.Message;

import java.io.IOException;
import java.util.UUID;

public interface IMessageDispatcher {
    void sendMessage(Message message, UUID subscriberId, int channelId) throws IOException;
}
