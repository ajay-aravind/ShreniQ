package in.ajay.shreniQ.core;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SubScriberRegistry implements ISubscriberRegistry{
    // Thread safe set of subscribers
    Set<Subscriber> subscribersList;
    int nextIndex;

    public SubScriberRegistry() {
        subscribersList = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void AddSubscriber(Subscriber sub) {
        subscribersList.add(sub);
    }

    @Override
    public synchronized void  RemoveSubscriber(UUID subId) {
        // no documentation if this particular operation is thread-safe so adding synchronized on the method for now
        // todo: finalize this
        subscribersList.removeIf(subscriber -> subscriber.getId().equals(subId));
    }

    @Override
    public List<Subscriber> getAllSubscribers() {
        return subscribersList.stream().toList();
    }

    @Override
    public synchronized Subscriber getNextSubscriber() {
        nextIndex = (nextIndex+1)%(subscribersList.size());
        return (Subscriber) this.subscribersList.toArray()[nextIndex];
    }
}
