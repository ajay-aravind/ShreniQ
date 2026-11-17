package in.ajay.shreniQ.core;

import in.ajay.common.AppConfig;
import in.ajay.shreniQ.storage.IShreniStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageRedeliveryManager {
    IShreniQueue queue;
    IShreniStorage storage;
    Logger logger = LoggerFactory.getLogger(MessageRedeliveryManager.class);
    ScheduledExecutorService scheduler;

    public MessageRedeliveryManager(IShreniQueue queue, IShreniStorage storage) {
        this.queue = queue;
        this.storage = storage;
    }

    public void startMessageRedeliveryManager(){
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {

                var queueItem = this.storage.DequeueItemFromInFlightQueue();
                while(queueItem!=null){
                    var messageId = queueItem.message.getId();
                    if(this.storage.AckedMessagesListContainsId(messageId)){
                        this.storage.RemoveIdFromAckedMessage(messageId);
                    }
                    else
                    {
                        if(queueItem.isMessageStillValid()){
                            this.queue.EnqueueMessage(new QueueItem(queueItem.message, queueItem.currentDeliveryAttempt++));
                            
                        }else{
                            this.storage.EnqueueItemToDeadQueue(queueItem);
                        }
                    }
                }

            } catch (Exception e) {

            }
        }, 0, AppConfig.getInt("MessageRedeliveryManagerInterval"), TimeUnit.SECONDS);

    }

    public void stopMessageRedeliveryManager(){
        if(this.scheduler != null){
            this.scheduler.shutdown();
        }
    }
}
