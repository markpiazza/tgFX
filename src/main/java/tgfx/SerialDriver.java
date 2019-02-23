package tgfx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;
import jssc.SerialPort;
import jssc.*;
import tgfx.utility.UtilityFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * SerialDriver
 *
 */
public class SerialDriver implements SerialPortEventListener {
    private static final Logger logger = LogManager.getLogger();

    private static byte[] lineBuffer = new byte[1024];
    private static int lineIdx = 0;

    private boolean connectionState = false;

    private SerialPort serialPort;

    public SerialDriver() {
    }


    /**
     * get serial port
     * @return serial port
     */
    public SerialPort getSerialPort(){
        return serialPort;
    }


    /**
     * write
     * @param str string to write
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
     * priority write by string
     * @param str string
     * @throws SerialPortException serial port exception
     */
    public void priorityWrite(String str) throws SerialPortException {
        serialPort.writeBytes(str.getBytes());
    }


    /**
     * priority write by byte
     * @param b byte
     * @throws SerialPortException serial port exception
     */
    public void priorityWrite(Byte b) throws SerialPortException {
        logger.debug("[*] Priority Write Sent\n");
        serialPort.writeByte(b);
    }


    /**
     * disconnect
     * @throws SerialPortException serial port exception
     */
    public synchronized void disconnect() throws SerialPortException {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            // Set our disconnected state
            setConnected(false);
        }
    }


    /**
     * is connected
     * @return is connected
     */
    public boolean isConnected() {
        return connectionState;
    }


    /**
     * set connected
     * @param connected is connected
     */
    public void setConnected(boolean connected) {
        connectionState = connected;
    }


    /**
     * serial event
     * @param event event
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        int bytesToRead = event.getEventValue();
        byte[] tmpBuffer;

        if (event.isRXCHAR()) {
            try {
                tmpBuffer = serialPort.readBytes(bytesToRead, serialPort.getInputBufferBytesCount());
            } catch (SerialPortException | SerialPortTimeoutException ex) {
                logger.error(ex);
                return;
            }
            
            for (int i = 0; i < bytesToRead; i++) {
                // We have to filter our XON or XOFF characters from JSON
                // if (tmpBuffer[i] == 0x11 || tmpBuffer[i] == 0x13) {
                //    continue;
                // }
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
     * list serial ports
     * @return array of serial ports
     */
    public String[] listSerialPorts() {
        String[] ports = jssc.SerialPortList.getPortNames();
        List<String> portList = new ArrayList<>();

        for (String port : ports) {
            SerialPort serialPort = new SerialPort(port);
//            if (!serialPort.getPortName().contains("Bluetooth")) {
//                logger.info("Found Bluetooth Serial Port");
//            }

            if (UtilityFunctions.getOperatingSystem().equals("mac")) {
                if (serialPort.getPortName().contains("tty")) {
                    // We want to remove the the duplicate tty's and just
                    // provide the "cu" ports in the drop down.
                    continue;
                }
            }

            //Go ahead and add the ports that made it though the logic above
            portList.add(serialPort.getPortName());
        }

        return portList.toArray(new String[0]);
    }


    /**
     * initialize
     * @param port port
     * @param DATA_RATE data rate
     * @return success
     * @throws SerialPortException serial port exception
     */
    public boolean initialize(String port, int DATA_RATE) throws SerialPortException {
        if (isConnected()) {
            logger.info("Port Already Connected.");
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
        setConnected(true);
        return true;
    }
}
