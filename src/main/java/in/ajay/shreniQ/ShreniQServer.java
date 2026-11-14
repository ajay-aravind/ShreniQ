package in.ajay.shreniQ;

import in.ajay.common.AppConfig;
import in.ajay.shreniQ.core.IShreniQueue;
import in.ajay.shreniQ.core.ISubscriberRegistry;
import in.ajay.shreniQ.core.ShreniQueue;
import in.ajay.shreniQ.core.SubScriberRegistry;
import in.ajay.shreniQ.storage.IShreniStorage;
import in.ajay.shreniQ.storage.ShreniStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShreniQServer {
    private static final String HOST = "localhost";
    private static final int PORT = 50001;
    private final Logger logger;
    private boolean running;
    private SSLServerSocket serverSocket;
    private IShreniQueue queue;
    private final ExecutorService clientExecutor =
            Executors.newVirtualThreadPerTaskExecutor();
    ISubscriberRegistry subscriberRegistry;
    IShreniStorage storage;

    public ShreniQServer(){
        logger = LoggerFactory.getLogger(ShreniQServer.class);
        this.subscriberRegistry = new SubScriberRegistry();
        this.storage = new ShreniStorage();
        this.queue = new ShreniQueue(subscriberRegistry, storage);
    }

    public void start() throws IOException, GeneralSecurityException {
        try
        {
            AppConfig.printConfig();
            AppConfig.getEnv().forEach(entry ->
                    logger.debug(entry.getKey() + "=" + entry.getValue()));

            if (running) {
                throw new IllegalStateException("Server is already running");
            }

            SSLContext sslContext = TlsContext.createServerContext();

            serverSocket =
                    (SSLServerSocket) sslContext.getServerSocketFactory()
                            .createServerSocket(
                                    AppConfig.getInt("port"),
                                    50,
                                    InetAddress.getByName(AppConfig.get("host")));

            serverSocket.setEnabledProtocols(
                    new String[]{"TLSv1.3", "TLSv1.2"});

            logger.info("TLS cache server listening on "+AppConfig.get("host")+":"+AppConfig.getInt("port"));
            running = true;

            try {
                while (running) {
                    try {
                        SSLSocket client = (SSLSocket) serverSocket.accept();
                        logger.info(
                                "Client connected:" +
                                        client.getRemoteSocketAddress());
                        clientExecutor.submit(() -> handleClient(client));
                    } catch (SocketException e) {
                        if (running) {
                            logger.error("Error while accepting the connection. Error: " + e.getMessage());
                        }
                        if (serverSocket.isClosed()) {
                            running = false;
                        }
                    }
                    catch(IllegalArgumentException ex){
                        logger.error("Error while handling connection. Error: "+ ex.getMessage());
                    }
                    catch(ProtocolException ex){
                        logger.error("Error in protocol. Error: "+ex.getMessage());
                    }
                    catch(IOException ex){
                        logger.error("I/O error in connection. Error: "+ex.getMessage());
                    }
                    catch(Exception ex){
                        logger.error("Unknown exception while handling connection. Error:"+ ex.getMessage());
                    }
                }
            } finally {
                close();
            }
        }
        catch(Exception ex)
        {
            logger.error("Error while starting server. Ex"+ ex.getMessage());
        }
        finally
        {
            running = false;
            clientExecutor.shutdown();
        }
    }

    private void handleClient(SSLSocket client) {
        String userName = System.getenv(AppConfig.QUEUE_USER_NAME);
        String password = System.getenv(AppConfig.QUEUE_PASSWORD);
        int connectionTimeout = AppConfig.getInt("connectionTimeout");
        int maxChannelsPerConnection = AppConfig.getInt("maxChannelsPerConnection");
        ArrayList<UUID> clientIdList =  this.parseUUIDs(AppConfig.get("clientIdList"));


        ConnectionHandler handler = new ConnectionHandler(client, userName, password, clientIdList, connectionTimeout, maxChannelsPerConnection, queue, this.subscriberRegistry);
        try {
            handler.handleClient();
        } catch (IOException e) {
            logger.error("Error while handling client: "+e.getMessage());
        }finally {
            try {
                handler.close();
            } catch (IOException e) {
                logger.error("Error while closing client connection :"+e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }


    public void close() throws IOException {
        running = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        clientExecutor.close();
    }

    private ArrayList<UUID> parseUUIDs(String input) {
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
}
