package in.ajay.shreniQ.core;

import in.ajay.common.Message;
import in.ajay.subscriber.Subscriber;

import java.util.UUID;

public interface IShreniQueue {
    void EnqueueMessage(QueueItem item);
    QueueItem DequeueMessage();
    void AckMessage(UUID messageId);
}
