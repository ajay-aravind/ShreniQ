package in.ajay.publisher;

import in.ajay.common.AppConfig;
import in.ajay.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class PublisherOrchestrator{
    // creates publishers
    // creates connections
    // creates a map of publishers, connections

    private List<Publisher> publisherList;
    private List<ConnectionHandler> connectionPool;
    private Map<Publisher, ConnectionHandler> publisherConnectionHandlerMap;
    private Logger logger;

    public PublisherOrchestrator() {
        this.logger = LoggerFactory.getLogger(PublisherOrchestrator.class);
    }

    void startPublishers() {

        AppConfig.getEnv().forEach(entry ->
                logger.debug(entry.getKey() + "=" + entry.getValue()));

        AppConfig.printConfig();

        String userName = System.getenv(AppConfig.QUEUE_USER_NAME);
        String password = System.getenv(AppConfig.QUEUE_PASSWORD);
        URI queueAddress = URI.create(AppConfig.get("queueAddress"));
        int connectionTimeout = AppConfig.getInt("connectionTimeout");
        int maxChannelsPerConnection = AppConfig.getInt("maxChannelsPerConnection");
        int maxConnectionsPerPool = AppConfig.getInt("maxConnectionsPerPool");
        ArrayList<UUID> clientIdList =  this.parseUUIDs(AppConfig.get("clientIdList"));


        publisherList = new ArrayList<>();
        connectionPool = new ArrayList<>();
        this.publisherConnectionHandlerMap = new HashMap<>();

        logger.debug("Creating connection pool");

        for (int i = 0; i < maxConnectionsPerPool; i++) {
            ConnectionHandler handler = new ConnectionHandler(userName, password, clientIdList.get(i), queueAddress, connectionTimeout, maxChannelsPerConnection, UUID.randomUUID());

            try{
                handler.initConnection();
                connectionPool.add(handler);
            }catch(Exception ex){
                logger.error("Error while initiating connection: "+ex.getMessage());
                return;
            }
        }

        logger.debug("Creating publishers");

        for (int i = 0; i < maxConnectionsPerPool; i++) {
            for (int j = 0; j < maxChannelsPerConnection; j++) {
                Publisher publisher = new Publisher(this.getListOfRandomMessages(), clientIdList.get(i) , 1, UUID.randomUUID(), connectionPool.get(i));
                publisherList.add(publisher);
                publisherConnectionHandlerMap.put(publisher, connectionPool.get(i));
            }
        }

        logger.debug("starting publishers");
        for(var publisher: publisherList) {
            publisher.startPublishing();
        }

        logger.debug("Closing connections");

        for (var connectionHandler: connectionPool) {
            try {
                connectionHandler.close();
            } catch (IOException e) {
                logger.error("Error while closing the connection:"+ e.getMessage());
            }
        }
    }

    private static ArrayList<UUID> parseUUIDs(String input) {
        ArrayList<UUID> uuids = new ArrayList<>();

        if (input == null || input.isBlank()) {
            return uuids;
        }

        for (String value : input.split(";")) {
            String trimmed = value.trim();

            if (!trimmed.isEmpty()) {
                uuids.add(UUID.fromString(trimmed));
            }
        }

        return uuids;
    }

    private List<Message> getListOfRandomMessages(){

        List<Message> messageList = new ArrayList<>();

        for (int i = 0; i < 1; i++) {
            String s = "hello world"+i;
            byte b = 0;
            Message message = new Message(UUID.randomUUID(), Instant.now().toEpochMilli(), "in/ajay/shreni/general", b , 10*60*1000, s.getBytes(StandardCharsets.UTF_8));
            messageList.add(message);
        }

        return messageList;
    }
}
