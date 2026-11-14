package in.ajay.shreniQ;

import in.ajay.common.*;
import in.ajay.shreniQ.core.IShreniQueue;
import in.ajay.shreniQ.core.ISubscriberRegistry;
import in.ajay.shreniQ.core.QueueItem;
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

public class ConnectionHandler extends BaseConnectionHandler {

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
                    //handle subscriber connection
                }

            }else{
                logger.debug("Authentication failed, closing connection: " + clientConnection.getRemoteSocketAddress());
            }
        }catch(IOException ex){
            logger.error("Error while serving the client" + ex.getMessage());
        }
        catch(Exception ex){
            logger.error("Error while serving the client" + ex.getMessage());
        }
        finally {
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
            this.logger.debug("closed connection");
        }
    }

    private void HandlePublisherConnection() throws IOException {
        while(true)
        {
            byte[] contentFrameBytes = this.readContentFrame(this.clientConnection.getInputStream());
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

        int frameLength = this.readFrameLength(input);

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
    private byte[] readContentFrame(InputStream input)
            throws IOException {

        int frameLength = this.readFrameLength(input);

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
}
