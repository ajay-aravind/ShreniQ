package in.ajay.shreniQ.storage;

import in.ajay.shreniQ.core.QueueItem;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface IShreniStorage {
    void EnqueueQueueItem(QueueItem item);
    QueueItem DequeueItem();
    void AddIdToAckedMessages(UUID id);
    boolean AckedMessagesListContainsId(UUID id);
    void RemoveIdFromAckedMessage(UUID id);
    void EnqueueQueueItemToInflightQueue(QueueItem item);
    QueueItem DequeueItemFromInFlightQueue();
    void EnqueueItemToDeadQueue(QueueItem item);
    QueueItem DequeueItemFromDeadQueue();
}
