package tgfx.external;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.SerialDriver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SocketMonitor
 * external Socket monitor for remote connections
 */
public class SocketMonitor {
    private static final Logger logger = LogManager.getLogger();

    private SerialDriver ser = SerialDriver.getInstance();
    private ServerSocket server;
    private int listenerPort;
    private int clientCount = 0;

    public SocketMonitor(String tmpport) {
        listenerPort = Integer.parseInt(tmpport);
        this.initServer();
        this.handleConnections();
    }

    public SocketMonitor(ServerSocket server) {
        this.server = server;
    }

    int countClientConnections() {
        return (clientCount);
    }

    private void initServer() {
        try {
            server = new ServerSocket(listenerPort);
        } catch (IOException e) {
            logger.error("Could not listen on port: " + listenerPort);
        }
    }

    private void handleConnections() {
        logger.info("Remote Monitor Listening for Connections....");
//        while (ser.isConnected()) {
            try {
                final Socket socket = server.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            } catch (IOException ex) {
                logger.error("Error: " + ex.getMessage());
            }
//        }
        logger.info("Socket Monitor Terminated...");
    }

}