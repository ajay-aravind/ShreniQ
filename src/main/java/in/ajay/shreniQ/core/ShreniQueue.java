package in.ajay.shreniQ.core;

import in.ajay.common.Message;
import in.ajay.shreniQ.storage.IShreniStorage;
import in.ajay.subscriber.Subscriber;

import java.util.Queue;
import java.util.UUID;

public class ShreniQueue implements IShreniQueue{

    ISubscriberRegistry registry;
    IShreniStorage storage;

    public ShreniQueue(ISubscriberRegistry registry, IShreniStorage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    @Override
    public void EnqueueMessage(QueueItem item) {
        this.storage.EnqueueQueueItem(item);
    }

    @Override
    public synchronized QueueItem DequeueMessage() {
        var item = this.storage.DequeueItem();
        this.storage.EnqueueQueueItemToInflightQueue(item);
        return item;
    }

    /// Network layer invokes this when the subscriber sends ack for a message. This method adds the message id to acked message set
    /// @param messageId
    @Override
    public void AckMessage(UUID messageId) {
        this.storage.AddIdToAckedMessages(messageId);
    }
}
