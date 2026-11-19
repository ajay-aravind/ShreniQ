package in.ajay.shreniQ;

import in.ajay.common.*;
import in.ajay.shreniQ.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

import static in.ajay.common.AuthenticationFrameParser.MIN_AUTH_FRAME_LENGTH;
import static in.ajay.common.PublisherContentFrameParser.*;

public class ConnectionHandler extends BaseConnectionHandler  implements IMessageDispatcher {

    IShreniQueue queue;
    ISubscriberRegistry subscriberRegistry;
    SSLSocket clientConnection;
    private String userName;
    private String password;
    private UUID clientId;
    private ClientRole role;
    private List<UUID> whilteListedClientIds;
    private int connectionTimeout;
    private int maxChannelsPerConnection;
    private Logger logger;
    private boolean isAuthenticated;

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int CONTENT_ACK_FRAME_LENGTH = 39;
    private static final int MAX_CONTENT_FRAME_LENGTH = 1024*1024;

    public ConnectionHandler(SSLSocket clientConnection,
                             String userName,
                             String password,
                             List<UUID> whilteListedClientIds,
                             int connectionTimeout,
                             int maxChannelsPerConnection,
                             IShreniQueue queue,
                             ISubscriberRegistry subscriberRegistry) {
        this.clientConnection = clientConnection;
        this.userName = userName;
        this.password = password;
        this.whilteListedClientIds = whilteListedClientIds;
        this.connectionTimeout = connectionTimeout;
        this.maxChannelsPerConnection = maxChannelsPerConnection;
        this.logger = LoggerFactory.getLogger(ConnectionHandler.class);
        this.queue = queue;
        this.subscriberRegistry = subscriberRegistry;
    }

    public void handleClient() throws IOException {

        clientConnection.setSoTimeout(this.connectionTimeout);
        logger.info("Starting handshake with client:" + clientConnection.getRemoteSocketAddress());
        clientConnection.startHandshake();
        logger.info("handshake completed with client:" + clientConnection.getRemoteSocketAddress());
        logger.info("Reading auth frame from client: " + clientConnection.getRemoteSocketAddress());
        try {
            byte[] frameBytes = this.readAuthFrame(this.clientConnection.getInputStream());
            if(frameBytes == null){
                this.logger.info("Expected authentication but client closed connection: "+this.clientConnection.getRemoteSocketAddress());
                return;
            }
            AuthenticationFrame authFrame = AuthenticationFrameParser.parse(frameBytes);
            Authenticator authenticator = new Authenticator(this.userName, this.password);
            boolean isAuthenticated = authenticator.authenticate(authFrame);
            if(isAuthenticated){
                logger.debug("Authentication successful: " + clientConnection.getRemoteSocketAddress());
                this.clientId = authFrame.getClientId();
                this.role = authFrame.getRole();

                // sending auth ack back
                AuthResponseFrame responseFrame = new AuthResponseFrame(AuthFrameResponseCode.AUTH_SUCCESSFUL);
                byte[] ackFrameBytes = AuthenticationResponseFrameParser.encode(responseFrame);
                this.clientConnection.getOutputStream().write(ackFrameBytes);
                this.clientConnection.getOutputStream().flush();

                logger.debug("sent auth ack back to client: " + clientConnection.getRemoteSocketAddress());

                if(this.role.equals(ClientRole.PUBLISHER)){
                    this.HandlePublisherConnection();
                }else if(this.role.equals(ClientRole.SUBSCRIBER)){
                    Subscriber subscriber = new Subscriber(this.clientId, this, 0, authFrame.getPublisherSubscriberId());
                    subscriberRegistry.AddSubscriber(subscriber);
                }
            }else{
                logger.debug("Authentication failed, closing connection: " + clientConnection.getRemoteSocketAddress());
                this.close();
            }
        }catch(IOException ex){
            logger.error("Error while serving the client" + ex.getMessage());
            this.close();
        }
        catch(Exception ex){
            logger.error("Error while serving the client" + ex.getMessage());
            this.close();
        }
    }

    public void close() throws IOException {
        if(this.clientConnection == null){
            this.logger.debug("connection doesn't exist");
        }
        else if(this.clientConnection.isClosed()){
            this.logger.debug("connection already closed");
        }
        else if(!this.clientConnection.isClosed()){
            this.clientConnection.close();
            this.logger.debug("closed connection" + this.clientConnection.getRemoteSocketAddress());
        }
    }

    private void HandlePublisherConnection() throws IOException {
        while(true)
        {
            byte[] contentFrameBytes = this.readContentFrame(this.clientConnection.getInputStream());
            if(contentFrameBytes==null){
                logger.info("Client closed connection"+ this.clientConnection.getRemoteSocketAddress());
                break;
            }
            PublisherContentFrame contentFrame = PublisherContentFrameParser.decode(contentFrameBytes);

            Message message = new Message(contentFrame.getMessageId(), contentFrame.getCreatedAt(), contentFrame.getTopicId(), (byte)contentFrame.getChannelId(), contentFrame.getTtl(), contentFrame.getPayload());
            this.logger.debug("Received message:"+ message);
            QueueItem item = new QueueItem(message, 1);
            this.queue.EnqueueMessage(item);
            this.logger.debug("Enqueued message:"+contentFrame.getMessageId());

            PublisherContentFrameAck ackFrame = new PublisherContentFrameAck(message.getId(), contentFrame.getPublisherId(), message.getChannelId());
            byte[] ackFrameBytes = PublisherContentFrameAckParser.encode(ackFrame);
            OutputStream opStream = this.clientConnection.getOutputStream();
            opStream.write(ackFrameBytes);
            opStream.flush();
            this.logger.debug("acked message:"+contentFrame.getMessageId());
        }
    }

    /**
     * Read one complete frame from connection input stream.
     */
    private byte[] readAuthFrame(InputStream input)
            throws IOException {

        // Read the first byte separately to detect clean EOF.
        int firstByte = input.read();

        if (firstByte == -1) {
            // No bytes of the next frame were received.
            // The connection ended at a frame boundary.
            return null;
        }

        int frameLength = this.readFrameLength(input, (byte)firstByte);

        if (frameLength < MIN_AUTH_FRAME_LENGTH) {
                throw new ProtocolException("Invalid auth frame length: " + frameLength);
        }

        try {
            byte[] frameBytes = this.readRemainingFrame(input, frameLength);
            return frameBytes;
        } catch (IOException e) {
            throw new IOException(
                    "Failed to read publisher auth frame", e);
        }
    }

    /**
     * Read one complete frame from connection input stream.
     */
    private byte[] readSubscriberContentAckFrame(InputStream input) throws IOException {

        // Read the first byte separately to detect clean EOF.
        int firstByte = input.read();

        if (firstByte == -1) {
            // No bytes of the next frame were received.
            // The connection ended at a frame boundary.
            return null;
        }

        int frameLength = this.readFrameLength(input, (byte)firstByte);

        if (frameLength < SubscriberContentAckFrame.FRAME_LENGTH) {
            throw new ProtocolException("Invalid subscriber content ack frame length: " + frameLength);
        }

        try {
            byte[] frameBytes = this.readRemainingFrame(input, frameLength);
            return frameBytes;
        } catch (IOException e) {
            throw new IOException(
                    "Failed to read subscriber content ack frame", e);
        }
    }

    /**
     * Read one complete frame from connection input stream.
     */
    private byte[] readContentFrame(InputStream input)
            throws IOException {

        // Read the first byte separately to detect clean EOF.
        int firstByte = input.read();

        if (firstByte == -1) {
            // No bytes of the next frame were received.
            // The connection ended at a frame boundary.
            return null;
        }

        int frameLength = this.readFrameLength(input, (byte)firstByte);

        int minimumLength = FIXED_FRAME_LENGTH + MIN_TOPIC_LENGTH;

        int maximumLength = FIXED_FRAME_LENGTH +
                MAX_TOPIC_LENGTH + MAX_CONTENT_FRAME_LENGTH;

        if (frameLength < minimumLength ||
                frameLength > maximumLength) {

            throw new IOException(
                    "Invalid PublisherContentFrame length: " +
                            frameLength);
        }

        byte[] frameBytes =
                this.readRemainingFrame(input, frameLength);

        try {
            return frameBytes;
        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Failed to decode PublisherContentFrame", e);
        }
    }

    @Override
    public void sendMessage(Message message, UUID subscriberId, int channelId) throws IOException {
        if(!this.clientConnection.isClosed()) {
            if (isAuthenticated) {
                byte defaultChannelId = 0;
                SubscriberContentFrame contentFrame = new SubscriberContentFrame(
                        FrameType.SUBSCRIBER_MESSAGE.code(),
                        message.getId(),
                        message.getCreatedAt(),
                        message.getTopicId(),
                        defaultChannelId,
                        subscriberId,
                        message.getPayload());
                byte[] contentFrameBytes = SubscriberContentFrame.encode(contentFrame);
                var opStream = this.clientConnection.getOutputStream();
                opStream.write(contentFrameBytes);
                opStream.flush();
                this.logger.debug("Sent message to subscriber. message: " + message);

                byte[] ackFrameBytes = this.readSubscriberContentAckFrame(this.clientConnection.getInputStream());
                SubscriberContentAckFrame ackFrame = SubscriberContentAckFrame.decode(ackFrameBytes);
                this.logger.debug("ack message received for message: " + ackFrame.getMessageId());
                this.queue.AckMessage(ackFrame.getMessageId());
            } else {
                this.logger.warn("aborting dispatching the message. Connection is not authenticated");
                throw new SecurityException("Connection is not authenticated");
            }
        }else{
            throw new IOException("Connection closed");
        }
    }
}
