package tgfx;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SerialWriter
 */
public class SerialWriter implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private static final Object MUTEX = new Object();
    private static final int BUFFER_SIZE = 180;

    private static SerialDriver SERIAL = SerialDriver.getInstance();
    private static boolean THROTTLED = false;

    private AtomicInteger bufferAvailable = new AtomicInteger(BUFFER_SIZE);
    private BlockingQueue<String> queue;
    private boolean run = true;
    private boolean cleared  = false;

    private SimpleBooleanProperty isSendingFile = new SimpleBooleanProperty(false);
    private SimpleStringProperty gcodeComment = new SimpleStringProperty("");


    /**
     *
     * @param queue
     */
    public SerialWriter(BlockingQueue queue) {
        this.queue = queue;
    }


    /**
     *
     */
    void resetBuffer() {
        //Called onDisconnectActions
        bufferAvailable.set(BUFFER_SIZE);
        notifyAck();
    }


    /**
     *
     */
    public void clearQueueBuffer() {
        queue.clear();
        // We set this to tell the MUTEX with waiting for an ack to
        // send a line that it should not send a line.. we were asked to be cleared.
        this.cleared = true;
        //This is done in resetBuffer is this needed?
        bufferAvailable.set(BUFFER_SIZE);
        this.setThrottled(false);
        this.notifyAck();
    }


    /**
     *
     * @return
     */
    public boolean isRun() {
        return run;
    }


    /**
     *
     * @param run
     */
    public void setRun(boolean run) {
        this.run = run;
    }


    /**
     *
     * @return
     */
    synchronized int getBufferValue() {
        return bufferAvailable.get();
    }


    /**
     *
     * @param val
     */
    public synchronized void setBuffer(int val) {
        logger.debug("Got a BUFFER Response.. reset it to: " + val);
        bufferAvailable.set(val);
    }


    /**
     *
     * @param lenBytesReturned
     */
    synchronized void addBytesReturnedToBuffer(int lenBytesReturned) {
        bufferAvailable.set(getBufferValue() + lenBytesReturned);
        logger.debug("Returned " + lenBytesReturned + " to buffer. " +
                "Buffer is now at " + bufferAvailable);
    }


    /**
     *
     * @param cmd
     */
    public void addCommandToBuffer(String cmd) {
        this.queue.add(cmd);
    }


    /**
     *
     * @param t
     */
    public void setThrottled(boolean t) {
        synchronized (MUTEX) {
            if (t == THROTTLED) {
                logger.debug("Throttled already set");
                return;
            }
            logger.debug("Setting Throttled " + t);
            THROTTLED = t;
        }
    }


    /**
     *
     */
    public void notifyAck() {
        // This is called by the response parser when an ack packet is recvd.  This
        // Will wake up the MUTEX that is sleeping in the write method of the serialWriter
        // (this) class.
        synchronized (MUTEX) {
            logger.debug("Notifying the SerialWriter we have recvd an ACK");
            MUTEX.notify();
        }
    }


    /**
     *
     * @param str
     */
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


    /**
     *
     * @param str
     */
    public void write(String str) {
        try {
            synchronized (MUTEX) {
//                int _currentPlanningBuffer = TinygDriver.getInstance().getQueryReport().getAvailableBufferSize();

//                if(_currentPlanningBuffer < 28){
//                    //if we have less that 28 moves in the planning buffer send a line
//                }

                while (THROTTLED) {
                    if (str.length() > getBufferValue()) {
                        logger.debug("Throttling: Line Length: " + str.length() +
                                " is smaller than buffer length: " + bufferAvailable);
                        setThrottled(true);
                    } else {
                        setThrottled(false);
                        bufferAvailable.set(getBufferValue() - str.length());
                        break;
                    }
                    logger.debug("We are Throttled in the write method for SerialWriter");
                    // We wait here until the an ack comes in to the response parser
                    // frees up some buffer space.  Then we unlock the MUTEX and write the next line.
                    MUTEX.wait();
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

            SERIAL.write(str);
            
        } catch (InterruptedException ex) {
            logger.error("Error in SerialDriver Write");
        }
    }


    /**
     *
     * @return
     */
    public SimpleBooleanProperty getIsSendingFile(){
        return isSendingFile;
    }


    /**
     *
     * @return
     */
    public SimpleStringProperty getGcodeComment(){
        return gcodeComment;
    }


    /**
     *
     */
    @Override
    public void run() {
        logger.info("Serial Writer Thread Running...");
        while (run) {
            try {
                String tmpCmd = queue.take();  //Grab the line
                if(tmpCmd.equals("**FILEDONE**")){
                    //Our end of file sending token has been detected.
                    //We will not enable jogging by setting isSendingFile to false
                    // TODO: this might work better as a an event instead of a binding
                    //GcodeTabController.setIsFileSending(false);
                    isSendingFile.setValue(false);
                }else if(tmpCmd.startsWith("**COMMENT**")){
                    //Display current gcode comment
                    // TODO: this might work better as a an event instead of a binding
                    //GcodeTabController.setGcodeTextTemp("Comment: " + tmpCmd);
                    gcodeComment.setValue("Comment: "+tmpCmd);
                    continue;
                }
                this.write(tmpCmd);
            } catch (InterruptedException ex) {
                logger.error("Exception in SerialWriter Thread");
            }
        }
        logger.info("SerialWriter thread exiting...");
    }
}