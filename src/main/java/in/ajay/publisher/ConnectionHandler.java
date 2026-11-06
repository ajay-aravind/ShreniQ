package in.ajay.publisher;

import in.ajay.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class ConnectionHandler {

    private String userName;
    private String password;
    private UUID clientId;
    private URI queueAddress;
    private UUID publisherId;
    private long connectionTimeout;
    private int maxChannelsPerConnection;
    private SSLSocket socket;
    private Logger logger;

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;


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

            byte[] authResponseFrameBytes = this.readFrame(this.socket.getInputStream());
            var authResponse = AuthenticationResponseFrameParser.decode(authResponseFrameBytes);
            this.logger.debug("Authentication succeeded");

            this.closeConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection() throws IOException {
        if(this.socket == null){
            this.logger.debug("connection doesn't exist");
        }
        else if(this.socket != null && this.socket.isClosed()){
            this.logger.debug("connection already closed");
        }
        else if(this.socket != null && !this.socket.isClosed()){
            this.socket.close();
            this.logger.debug("closed connection");
        }
    }

    /**
     * Reads exactly one frame from an InputStream.
     */
    private byte[] readFrame(InputStream input)
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
     * Helper to read the requested number of bytes.
     */
    private void readFully(
            InputStream input,
            byte[] buffer) throws IOException {

        readFully(input, buffer, 0, buffer.length);
    }

    private void readFully(
            InputStream input,
            byte[] buffer,
            int offset,
            int length) throws IOException {

        int totalRead = 0;

        while (totalRead < length) {

            int bytesRead = input.read(
                    buffer,
                    offset + totalRead,
                    length - totalRead);

            if (bytesRead == -1) {
                throw new EOFException(
                        "Unexpected end of stream");
            }

            totalRead += bytesRead;
        }
    }


}
