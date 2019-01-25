/*
 * tgFX Socket Monitor Class
 * Copyright Synthetos.com
 */
package tgfx.external;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.SerialDriver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.util.Observable;
import java.util.Observer;

import tgfx.Main;
import tgfx.tinyg.TinygDriver;

/**
 *
 * @author ril3y
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

    boolean initServer() {
        try {
            server = new ServerSocket(listenerPort);
            return (true);
        } catch (IOException e) {
            logger.error("Could not listen on port: " + listenerPort);
            return (false);
        }
    }

    public void handleConnections() {
        logger.info("[+]Remote Monitor Listening for Connections....");
//        while (ser.isConnected()) {
            try {
                final Socket socket = server.accept();
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            } catch (IOException ex) {
                logger.error("[!]Error: " + ex.getMessage());
            }
//        }
        logger.info("[!]Socket Monitor Terminated...");
    }

}


class ConnectionHandler implements Runnable, Observer {
    private static final Logger logger = LogManager.getLogger();

    private Socket socket;
    private boolean disconnect = false;
    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        SerialDriver ser = SerialDriver.getInstance();
        logger.info("[+]Opening Remote Listener Socket");
//        ser.addObserver(this);
       Thread t = new Thread(this);
//        t.start();
    }

    @Override
    public void update(Observable o, Object arg) {
        String[] message = (String[]) arg;
        if (message[0] == "JSON") {
            final String line = message[1];
            try {
                this.write(line + "\n");
            } catch (IOException ex) {
                disconnect = true;
            } catch (Exception ex) {
                logger.error("update(): " + ex.getMessage());
            }
        }
    }

    private void write(String l) throws Exception {
        //Method for writing to the socket
        socket.getOutputStream().write(l.getBytes());
    }

    public void run() {
        try {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            Main.print("GOT: " + stdIn.readLine());
//            try {
//                this.write("[+]Connected to tgFX\n");
//            } catch (Exception ex) {
//            }
            TinygDriver tg = TinygDriver.getInstance();
            SerialDriver ser = SerialDriver.getInstance();
            while (ser.isConnected() && !disconnect) {
                try {
                    String line = stdIn.readLine() + "\n";
                    tg.write(line);
                    Thread.sleep(100);
                } catch (IOException ex) {
                    disconnect = true;
                } catch (Exception ex) {
                    logger.error("run(): " + ex.getMessage());
                    break;
                }
            }
            logger.info("[+]Closing Remote Listener Socket");
            socket.close();

        } catch (IOException e) {
            logger.error(e);
        }
    }
}
