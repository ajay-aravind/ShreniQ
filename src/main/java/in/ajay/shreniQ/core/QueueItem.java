package in.ajay.shreniQ.core;

import in.ajay.common.AppConfig;
import in.ajay.common.Message;

import java.time.Instant;

public class QueueItem {
    Message message;
    int currentDeliveryAttempt;
    static int maxDeliveryAttempts = AppConfig.getInt("maxDeliveryAttempts");
    private long lastDeliveredAt;

    public QueueItem(Message message, int currentDeliveryAttempt) {
        this.message = message;
        this.currentDeliveryAttempt = currentDeliveryAttempt;
    }

    public long getLastDeliveredAt() {
        return lastDeliveredAt;
    }

    boolean isMessageStillValid(){
        if(currentDeliveryAttempt <= maxDeliveryAttempts && message.getTtl() >= Instant.now().toEpochMilli()){
            return true;
        }
        return false;
    }
}
