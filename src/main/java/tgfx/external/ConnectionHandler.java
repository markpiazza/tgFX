package tgfx.external;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.SerialDriver;
import tgfx.tinyg.TinygDriver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

class ConnectionHandler implements Runnable, Observer {
    private static final Logger logger = LogManager.getLogger();

    private Socket socket;
    private boolean disconnect = false;

    ConnectionHandler(Socket socket) {
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