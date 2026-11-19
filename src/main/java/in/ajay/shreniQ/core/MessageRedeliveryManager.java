package in.ajay.shreniQ.core;

import in.ajay.common.AppConfig;
import in.ajay.shreniQ.storage.IShreniStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageRedeliveryManager {
    IShreniQueue queue;
    IShreniStorage storage;
    Logger logger = LoggerFactory.getLogger(MessageRedeliveryManager.class);
    ScheduledExecutorService scheduler;
    int deliveryAckTimeoutInSeconds = AppConfig.getInt("deliveryAckTimeOutInSeconds");

    public MessageRedeliveryManager(IShreniQueue queue, IShreniStorage storage) {
        this.queue = queue;
        this.storage = storage;
    }

    public void startMessageRedeliveryManager(){
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(this::manageMessageRedelivery, 0, AppConfig.getInt("MessageRedeliveryManagerInterval"), TimeUnit.SECONDS);
    }

    private void manageMessageRedelivery(){
        try {
            this.logger.trace("Message redelivery manager starting");
            var queueItem = this.storage.DequeueItemFromInFlightQueue();
            while(queueItem!=null){
                var messageId = queueItem.message.getId();
                if(this.storage.AckedMessagesListContainsId(messageId)){
                    this.storage.RemoveIdFromAckedMessage(messageId);
                }
                else
                {
                    if(queueItem.isMessageStillValid() && isTheMessageAckTimedout(queueItem)){
                        this.queue.EnqueueMessage(new QueueItem(queueItem.message, queueItem.currentDeliveryAttempt++));
                    }else if (queueItem.isMessageStillValid() && !isTheMessageAckTimedout(queueItem)){
                        break;
                    }else{
                        this.storage.EnqueueItemToDeadQueue(queueItem);
                    }
                }
                queueItem = this.storage.DequeueItemFromInFlightQueue();
            }

        } catch (Exception e) {
            logger.error("Error while running message redelivery thread");
        }

        this.logger.trace("Message redelivery manager exiting");
    }

    private boolean isTheMessageAckTimedout(QueueItem queueItem) {
        long secondsElapsedFromDelivery = (long)Math.floor(((double) (Instant.now().toEpochMilli() - queueItem.getLastDeliveredAt()) / 1000)) ;
        return secondsElapsedFromDelivery > deliveryAckTimeoutInSeconds;
    }

    public void stopMessageRedeliveryManager(){
        if(this.scheduler != null){
            this.scheduler.shutdown();
            this.logger.info("Message redelivery manager stopped");
        }
    }
}
