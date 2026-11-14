package in.ajay.shreniQ;

import in.ajay.common.BaseConnectionHandler;
import in.ajay.shreniQ.core.IShreniQueue;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;

import static in.ajay.common.PublisherContentFrameParser.*;

public class PublisherConnectionHandler extends BaseConnectionHandler {

    IShreniQueue queue;
    SSLSocket clientConnection;
    Logger logger;

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int CONTENT_ACK_FRAME_LENGTH = 39;
    private static final int MAX_CONTENT_FRAME_LENGTH = 1024*1024;

}
