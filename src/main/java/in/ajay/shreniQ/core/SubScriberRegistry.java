package in.ajay.shreniQ.core;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SubScriberRegistry implements ISubscriberRegistry{
    Set<Subscriber> subscribersList;

    public SubScriberRegistry() {
        subscribersList = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void AddSubscriber(Subscriber sub) {
        subscribersList.add(sub);
    }

    @Override
    public void RemoveSubscriber(UUID subId) {
        subscribersList.removeIf(subscriber -> subscriber.getId().equals(subId));
    }

    @Override
    public List<Subscriber> getAllSubscribers() {
        return subscribersList.stream().toList();
    }
}
