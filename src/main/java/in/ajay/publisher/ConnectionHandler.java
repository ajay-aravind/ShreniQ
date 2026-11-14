package in.ajay.publisher;

import in.ajay.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static in.ajay.common.PublisherContentFrameParser.*;

public class ConnectionHandler extends BaseConnectionHandler  implements  AutoCloseable, IMessageDispatcher{

    private String userName;
    private String password;
    private UUID clientId;
    private URI queueAddress;
    private UUID publisherId;
    private long connectionTimeout;
    private int maxChannelsPerConnection;
    private SSLSocket socket;
    private Logger logger;
    private boolean isAuthenticated;

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int CONTENT_ACK_FRAME_LENGTH = 39;
    private static final int MAX_CONTENT_FRAME_LENGTH = 1024*1024;


    public ConnectionHandler(String userName, String password, UUID clientId, URI queueAddress, long connectionTimeout, int maxChannelsPerConnection, UUID publisherId) {
        this.userName = userName;
        this.password = password;
        this.clientId = clientId;
        this.queueAddress = queueAddress;
        this.connectionTimeout = connectionTimeout;
        this.maxChannelsPerConnection = maxChannelsPerConnection;
        this.publisherId = publisherId;
        this.logger = LoggerFactory.getLogger(ConnectionHandler.class);
    }

    public void initConnection(){
        SSLSocketFactory factory =
                (SSLSocketFactory) SSLSocketFactory.getDefault();

        try{
            this.socket = (SSLSocket) factory.createSocket(
                queueAddress.getHost(),
                queueAddress.getPort());

            this.socket.startHandshake();
            this.logger.debug("TLS connection established!");

            AuthenticationFrame frame = new AuthenticationFrame(this.userName, this.password, ClientRole.PUBLISHER, clientId, publisherId);
            this.logger.debug("Sending auth frame:" + frame);
            var binaryAuthFrame = AuthenticationFrameParser.encode(frame);
            this.socket.getOutputStream().write(binaryAuthFrame);
            this.socket.getOutputStream().flush();

            this.logger.debug("sent auth frame");

            byte[] authResponseFrameBytes = this.readAuthAckFrame(this.socket.getInputStream());
            var authResponse = AuthenticationResponseFrameParser.decode(authResponseFrameBytes);

            if(authResponse.getResponseCode() == AuthFrameResponseCode.AUTH_SUCCESSFUL) {
                this.isAuthenticated = true;
                this.logger.debug("Authentication succeeded");
            }else{
                this.logger.error("Authentication failed with code: "+ authResponse.getResponseCode().toString());
            }

        } catch (IOException e) {
            logger.error("Error: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void sendMessage(Message message, UUID publisherId, int channelId) throws IOException {
        if(isAuthenticated)
        {
            this.logger.debug("sending message :"+ message);
            PublisherContentFrame frame = new PublisherContentFrame(message.getId(), message.getCreatedAt(), message.getTopicId(), message.getChannelId(), publisherId, message.getTtl(), message.getPayload());
            byte[] frameBytes = PublisherContentFrameParser.encode(frame);
            OutputStream stream = this.socket.getOutputStream();
            stream.write(frameBytes);
            stream.flush();
            this.logger.debug("sent message :"+ message);

            byte[] contentAckBytes = readFrameContentAckFrame(this.socket.getInputStream());
            this.logger.debug("read ack message bytes:"+ message.getId());
            PublisherContentFrameAck ackFrame = PublisherContentFrameAckParser.decode(contentAckBytes);
            this.logger.debug("ack for message id:"+ackFrame.getMessageId()+" received");
        }
        else
        {
            this.logger.warn("aborting dispatching the message. Connection is not authenticated");
            throw new SecurityException("Connection is not authenticated");
        }
    }

    @Override
    public void close() throws IOException {
        if(this.socket == null){
            this.logger.debug("connection doesn't exist");
        }
        else if(this.socket.isClosed()){
            this.logger.debug("connection already closed");
        }
        else if(!this.socket.isClosed()){
            this.socket.close();
            this.logger.debug("closed connection");
        }
    }

    /**
     * Reads exactly one frame from an InputStream.
     */
    private byte[] readAuthAckFrame(InputStream input)
            throws IOException {

        byte[] lengthBytes = new byte[4];

        readFully(input, lengthBytes);

        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        lengthBuffer.order(ByteOrder.BIG_ENDIAN);

        int frameLength = lengthBuffer.getInt();

        if (frameLength >= MAX_FRAME_LENGTH) {
            throw new IOException(
                    "max frame length exceeded: " + frameLength);
        }

        byte[] frameBytes = new byte[frameLength];

        System.arraycopy(lengthBytes, 0, frameBytes, 0, 4);

        readFully(input, frameBytes, 4, frameLength - 4);

        return frameBytes;
    }

    /**
     * Read one acknowledgement frame from connection.
     */
    public byte[] readFrameContentAckFrame(InputStream input)
            throws IOException {

        int frameLength = this.readFrameLength(input);

        if (frameLength != CONTENT_ACK_FRAME_LENGTH) {
            throw new IOException(
                    "Invalid PublisherContentFrameAck length: " +
                            frameLength);
        }

        byte[] frameBytes =
                this.readRemainingFrame(input, frameLength);

        try {
            return frameBytes;
        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Failed to decode PublisherContentFrameAck", e);
        }
    }
}
