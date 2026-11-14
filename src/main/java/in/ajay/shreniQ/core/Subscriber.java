package in.ajay.shreniQ.core;

import java.util.Objects;
import java.util.UUID;

public class Subscriber {
    private UUID clientId;
    private IMessageDispatcher messageDispatcher;
    private int channelId;
    private UUID id;

    public Subscriber(UUID clientId, IMessageDispatcher messageDispatcher, int channelId, UUID id) {
        this.clientId = clientId;
        this.messageDispatcher = messageDispatcher;
        this.channelId = channelId;
        this.id = id;
    }

    public IMessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Subscriber that)) return false;
        return channelId == that.channelId && Objects.equals(clientId, that.clientId) && Objects.equals(messageDispatcher, that.messageDispatcher) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, messageDispatcher, channelId, id);
    }
}
