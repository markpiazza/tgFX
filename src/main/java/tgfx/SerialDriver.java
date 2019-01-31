package tgfx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;
import jssc.SerialPort;
import jssc.*;

import java.util.ArrayList;
import java.util.List;

/**
 * SerialDriver
 *
 */
public class SerialDriver implements SerialPortEventListener {
    private static final Logger logger = LogManager.getLogger();

    private static SerialDriver serialDriverInstance;

    private static byte[] lineBuffer = new byte[1024];
    private static int lineIdx = 0;

    private boolean connectionState = false;

    private SerialPort serialPort;

    /**
     * private constructor since this is a singleton
     */
    private SerialDriver() {
    }

    public static SerialDriver getInstance() {
        if(serialDriverInstance==null){
            serialDriverInstance = new SerialDriver();
        }
        return serialDriverInstance;
    }

    public SerialPort getSerialPort(){
        return serialPort;
    }

    public void write(String str) {
        try {
            serialPort.writeBytes(str.getBytes());
            logger.debug("Wrote Line: " + str);
        } catch (Exception ex) {
            logger.error("Error in SerialDriver Write");
            logger.error("\t" + ex.getMessage());
        }
    }

    public void priorityWrite(String str) throws SerialPortException {
        serialPort.writeBytes(str.getBytes());
    }

    public void priorityWrite(Byte b) throws SerialPortException {
        logger.debug("[*] Priority Write Sent\n");
        serialPort.writeByte(b);
    }

    public synchronized void disconnect() throws SerialPortException {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            setConnected(false); //Set our disconnected state
        }
    }

    public void setConnected(boolean c) {
        connectionState = c;
    }

    public boolean isConnected() {
        return connectionState;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        int bytesToRead;
        byte[] tmpBuffer = null;

        bytesToRead = event.getEventValue();

        if (event.isRXCHAR()) {
            try {
                tmpBuffer = serialPort.readBytes(bytesToRead, serialPort.getInputBufferBytesCount());
            } catch (SerialPortException | SerialPortTimeoutException ex) {
                logger.error(ex);
            }
            
            for (int i = 0; i < bytesToRead; i++) {
                //  We have to filter our XON or XOFF charaters from JSON
//                if (tmpBuffer[i] == 0x11 || tmpBuffer[i] == 0x13) {
//                    continue;
//                }
                if (tmpBuffer[i] == 0xA) { // inbuffer[i] is a \n
                    String f = new String(lineBuffer, 0, lineIdx);
                    if (!f.equals("")) { //Do not add "" to the jsonQueue..
                        TinygDriver.getInstance().appendJsonQueue(f);
                    }
                    lineIdx = 0;
                } else {
                    lineBuffer[lineIdx++] = tmpBuffer[i];
                }
            }
        }
    }


    public static String[] listSerialPorts() {
        String[] ports = jssc.SerialPortList.getPortNames();
        List<String> portList = new ArrayList<>();

        for (String port : ports) {
            SerialPort _tmpPort = new SerialPort(port);
            if (!_tmpPort.getPortName().contains("Bluetooth")) {

            }

//            if (UtilityFunctions.getOperatingSystem().equals("mac")) {
//                if (_tmpPort.getPortName().contains("tty")) {
//                    continue; // We want to remove the the duplicate tty's and just
//                              // provide the "cu" ports in the drop down.
//                }
//            }

            //Go ahead and add the ports that made it though the logic above
            portList.add(_tmpPort.getPortName());
        }

        return portList.toArray(new String[0]);
    }

    public boolean initialize(String port, int DATA_RATE) throws SerialPortException {
        if (isConnected()) {
            String returnMsg = "[*] Port Already Connected.\n";
            logger.info(returnMsg);
            return true;
        }

        // set port parameters
        serialPort = new SerialPort(port);
        serialPort.openPort();
        serialPort.setParams(DATA_RATE,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE);

        // open the streams
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
        serialPort.setRTS(true);

        // add event listeners
        serialPort.addEventListener(this);
        // notifyOnDataAvailable(true);
        
        logger.debug("Opened " + port + " successfully.");
        setConnected(true); //Register that this is connectionState.

        return true;
    }

}
