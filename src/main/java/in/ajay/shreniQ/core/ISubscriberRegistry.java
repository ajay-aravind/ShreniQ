package in.ajay.shreniQ.core;

import java.util.List;
import java.util.UUID;

public interface ISubscriberRegistry {
    void AddSubscriber(Subscriber sub);
    void RemoveSubscriber(UUID subId);
    List<Subscriber> getAllSubscribers();
}
