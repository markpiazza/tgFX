/*
 * Copyright (C) 2013-2014 Synthetos LLC. All Rights reserved.
 * http://www.synthetos.com
 */
package tgfx;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;
import tgfx.ui.gcode.GcodeTabController;

/**
 *
 * @author ril3y
 */
public class SerialWriter implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private BlockingQueue<String> queue;
    private boolean RUN = true;
    private boolean cleared  = false;
    private int BUFFER_SIZE = 180;
    private AtomicInteger buffer_available = new AtomicInteger(BUFFER_SIZE);
    private SerialDriver ser = SerialDriver.getInstance();
    private static final Object mutex = new Object();
    private static boolean throttled = false;

    public SerialWriter(BlockingQueue q) {
        this.queue = q;
    }

    void resetBuffer() {
        //Called onDisconnectActions
        buffer_available.set(BUFFER_SIZE);
        notifyAck();
    }

    public void clearQueueBuffer() {
        queue.clear();
        // We set this to tell the mutex with waiting for an ack to
        // send a line that it should not send a line.. we were asked to be cleared.
        this.cleared = true;
        try {
            //This is done in resetBuffer is this needed?
            buffer_available.set(BUFFER_SIZE);
            this.setThrottled(false);
            this.notifyAck();
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    public boolean isRUN() {
        return RUN;
    }

    public void setRun(boolean RUN) {
        this.RUN = RUN;
    }

    synchronized int getBufferValue() {
        return buffer_available.get();
    }

    public synchronized void setBuffer(int val) {
        buffer_available.set(val);
        logger.debug("Got a BUFFER Response.. reset it to: " + val);
    }

    synchronized void addBytesReturnedToBuffer(int lenBytesReturned) {
        buffer_available.set(getBufferValue() + lenBytesReturned);
        logger.debug("Returned " + lenBytesReturned + " to buffer. " +
                "Buffer is now at " + buffer_available);
    }

    public void addCommandToBuffer(String cmd) {
        this.queue.add(cmd);
    }

    public void setThrottled(boolean t) {
        synchronized (mutex) {
            if (t == throttled) {
                logger.debug("Throttled already set");
                return;
            }
            logger.debug("Setting Throttled " + t);
            throttled = t;
        }
    }

    public void notifyAck() {
        // This is called by the response parser when an ack packet is recvd.  This
        // Will wake up the mutex that is sleeping in the write method of the serialWriter
        // (this) class.
        synchronized (mutex) {
            logger.debug("Notifying the SerialWriter we have recvd an ACK");
            mutex.notify();
        }
    }

    private void sendUiMessage(String str) {
        //Used to send messages to the console on the GUI
        StringBuilder gcodeComment = new StringBuilder();
        int startComment = str.indexOf("(");
        int endComment = str.indexOf(")");
        for (int i = startComment; i <= endComment; i++) {
            gcodeComment.append(str.charAt(i));
        }
        Main.postConsoleMessage(" Gcode Comment << " + gcodeComment);
    }
    
    

    public void write(String str) {
        try {
            synchronized (mutex) {
                int _currentPlanningBuffer = TinygDriver.getInstance().getQueryReport().getPba();
                
                if(_currentPlanningBuffer < 28){
                    //if we have less that 28 moves in the planning buffer send a line
                }

                while (throttled) {
                    if (str.length() > getBufferValue()) {
                        logger.debug("Throttling: Line Length: " + str.length() +
                                " is smaller than buffer length: " + buffer_available);
                        setThrottled(true);
                    } else {
                        setThrottled(false);
                        buffer_available.set(getBufferValue() - str.length());
                        break;
                    }
                    logger.debug("We are Throttled in the write method for SerialWriter");
                    // We wait here until the an ack comes in to the response parser
                    // frees up some buffer space.  Then we unlock the mutex and write the next line.
                    mutex.wait();
                    if(cleared){
                       //clear out the line we were waiting to send.. we were asked to clear our buffer
                        //including this line that is waiting to be sent.
                        cleared = false;  //Reset this flag now...
                        return;
                    }
                    logger.debug("We are free from Throttled!");
                }
            }
            if (str.contains("(")) {
                //Gcode Comment Push it back to the UI
                sendUiMessage(str);
            }

            ser.write(str);
            
        } catch (InterruptedException ex) {
            logger.error("Error in SerialDriver Write");
        }
    }

    @Override
    public void run() {
        logger.info("[+]Serial Writer Thread Running...");
        while (RUN) {
            try {
                String tmpCmd = queue.take();  //Grab the line
                if(tmpCmd.equals("**FILEDONE**")){
                    //Our end of file sending token has been detected.
                    //We will not enable jogging by setting isSendingFile to false
                    GcodeTabController.setIsFileSending(false);
                }else if(tmpCmd.startsWith("**COMMENT**")){
                    //Display current gcode comment
                    GcodeTabController.setGcodeTextTemp("Comment: " + tmpCmd);
                    continue;
                }
                this.write(tmpCmd);
            } catch (Exception ex) {
                logger.error("[!]Exception in SerialWriter Thread");
            }
        }
        logger.info("[+]SerialWriter thread exiting...");
    }
}