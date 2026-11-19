package in.ajay.shreniQ.core;

import in.ajay.common.AppConfig;
import in.ajay.common.Message;
import in.ajay.shreniQ.storage.IShreniStorage;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueDrainer {

    IShreniQueue queue;
    ISubscriberRegistry registry;
    boolean shouldStopDrain;
    Logger logger = LoggerFactory.getLogger(QueueDrainer.class);

    public QueueDrainer(IShreniQueue queue, ISubscriberRegistry registry) {
        this.queue = queue;
        this.registry = registry;
    }

    public void startDrain() {
        logger.info("Queue drainer started!");
        while (!shouldStopDrain) {
            QueueItem item = queue.DequeueMessage();
            if (item != null) {
                var sub = this.registry.getNextSubscriber();
                if(sub!=null) {
                    int retryCount = 0;
                    while (retryCount < AppConfig.getInt("maxNetworkRetryCount") && !shouldStopDrain) {
                        try {
                            sub.getMessageDispatcher().sendMessage(new Message
                                    (item.message.getId(),
                                            item.message.getCreatedAt(),
                                            item.message.getTopicId(),
                                            item.message.getChannelId(),
                                            item.message.getTtl(),
                                            item.message.getPayload()), sub.getId(), 0);
                            logger.debug("Sent message and added to inflight queue, id:"+item.message.getId());
                        } catch (IOException e) {
                            // this will cover all connection related issues, hoping that the connection itself is still exists, so we will have retry policy here
                            // note: this retry policy is different from maxDeliveryRetry count.
                            this.logger.error("I/O error while publishing the message:" + item.message.getId() + ", retryCount:" + retryCount + ".Error" + e.getMessage());
                            retryCount++;
                        } catch (Exception ex) {
                            this.logger.error("Unknown error while publishing the message:" + item.message.getId() + ", retryCount:" + retryCount + ".Error" + ex.getMessage());
                            retryCount++;
                        }
                    }
                }else{
                    logger.debug("No subscribers found for the queue, unable to dispatch messages");
                }
            }else{
                logger.debug("Queue is empty, unable to dequeue messages");
            }
        }
        logger.info("Queue drainer exiting");
    }

    public void stopDrain(){
        this.shouldStopDrain = true;
        this.logger.info("Stopped queue drainer");
    }
}
