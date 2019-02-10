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


    /**
     *
     * @return
     */
    public static SerialDriver getInstance() {
        if(serialDriverInstance==null){
            serialDriverInstance = new SerialDriver();
        }
        return serialDriverInstance;
    }


    /**
     *
     * @return
     */
    public SerialPort getSerialPort(){
        return serialPort;
    }


    /**
     *
     * @param str
     */
    public void write(String str) {
        try {
            serialPort.writeBytes(str.getBytes());
            logger.debug("Wrote Line: " + str);
        } catch (SerialPortException ex) {
            logger.error(ex);
        }
    }


    /**
     *
     * @param str
     * @throws SerialPortException
     */
    public void priorityWrite(String str) throws SerialPortException {
        serialPort.writeBytes(str.getBytes());
    }


    /**
     *
     * @param b
     * @throws SerialPortException
     */
    public void priorityWrite(Byte b) throws SerialPortException {
        logger.debug("[*] Priority Write Sent\n");
        serialPort.writeByte(b);
    }


    /**
     *
     * @throws SerialPortException
     */
    public synchronized void disconnect() throws SerialPortException {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            setConnected(false); //Set our disconnected state
        }
    }


    /**
     *
     * @return
     */
    public boolean isConnected() {
        return connectionState;
    }


    /**
     *
     * @param c
     */
    public void setConnected(boolean c) {
        connectionState = c;
    }


    /**
     *
     * @param event
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        int bytesToRead = event.getEventValue();
        byte[] tmpBuffer = null;

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


    /**
     *
     * @return
     */
    public String[] listSerialPorts() {
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


    /**
     *
     * @param port
     * @param DATA_RATE
     * @return
     * @throws SerialPortException
     */
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
