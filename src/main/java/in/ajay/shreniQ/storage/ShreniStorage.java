package in.ajay.shreniQ.storage;

import in.ajay.shreniQ.core.QueueItem;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShreniStorage implements IShreniStorage{

    ConcurrentLinkedQueue<QueueItem> mainQueue; // this is drained by QueueDrainer
    Set<UUID> ackedMessageIds; // this contains messages which are acked but not removed from the inflight queue
    ConcurrentLinkedQueue<QueueItem> inFlightQueue;
    ConcurrentLinkedQueue<QueueItem> deadQueue;

    public ShreniStorage() {
        this.mainQueue = new ConcurrentLinkedQueue<>();
        this.ackedMessageIds = ConcurrentHashMap.newKeySet();
        this.inFlightQueue = new ConcurrentLinkedQueue<>();
        this.deadQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void EnqueueQueueItem(QueueItem item) {
        this.mainQueue.add(item);
    }

    @Override
    public QueueItem DequeueItem() {
        return this.mainQueue.poll();
    }

    @Override
    public void AddIdToAckedMessages(UUID id) {
        this.ackedMessageIds.add(id);
    }

    @Override
    public boolean AckedMessagesListContainsId(UUID id) {
        return this.ackedMessageIds.contains(id);
    }

    @Override
    public void RemoveIdFromAckedMessage(UUID id) {
        this.ackedMessageIds.remove(id);
    }

    @Override
    public void EnqueueQueueItemToInflightQueue(QueueItem item) {
        this.inFlightQueue.add(item);
    }

    @Override
    public QueueItem DequeueItemFromInFlightQueue() {
        return this.inFlightQueue.poll();
    }

    @Override
    public void EnqueueItemToDeadQueue(QueueItem item) {
        this.deadQueue.add(item);
    }

    @Override
    public QueueItem DequeueItemFromDeadQueue() {
        return this.deadQueue.poll();
    }
}
