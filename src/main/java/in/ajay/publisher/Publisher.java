package in.ajay.publisher;

import in.ajay.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Publisher {

    private List<Message> messagesToSend;
    private UUID clientId;
    private int channelId;
    private UUID publisherId;
    private IMessageDispatcher messageDispatcher;
    private Logger logger;

    public Publisher(List<Message> messagesToSend, UUID clientId, int channelId, UUID publisherId, IMessageDispatcher messageDispatcher) {
        this.messagesToSend = messagesToSend;
        this.clientId = clientId;
        this.channelId = channelId;
        this.publisherId = publisherId;
        this.messageDispatcher = messageDispatcher;
        this.logger = LoggerFactory.getLogger(Publisher.class);
    }

    void startPublishing(){
        this.logger.debug("started publishing on publisherId:" + publisherId);
        for (int i = 0; i < this.messagesToSend.size(); i++) {
            try {
                messageDispatcher.sendMessage(messagesToSend.get(i), publisherId, channelId);
            } catch (IOException e) {
                this.logger.error("Error while sending the message:"+e.getMessage());
            }catch(SecurityException ex){
                this.logger.error("Authentication issue while sendign message:"+ ex.getMessage());
            }
        }
        this.logger.debug("done publishing on publisherId:" + publisherId);
    }
}
