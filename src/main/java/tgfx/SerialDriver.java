/*
 * Copyright (C) 2013-2014 Synthetos LLC. All Rights reserved.
 * http://www.synthetos.com
 */
package tgfx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;
import jssc.SerialPort;
import jssc.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author ril3y
 */
public class SerialDriver implements SerialPortEventListener {
    private static final Logger logger = LogManager.getLogger();

    private static SerialDriver serialDriverInstance;

    private static byte[] lineBuffer = new byte[1024];
    private static int lineIdx = 0;

    private boolean connectionState = false;
    private boolean CANCELLED = false;

    public SerialPort serialPort;
    public InputStream input;
    public OutputStream output;
    public String[] portArray = null;
    public String debugFileBuffer = "";
    public List<String> lastRes = new ArrayList<>();
    public byte[] debugBuffer = new byte[1024];
    public double offsetPointer = 0;

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

    public void write(String str) {
        try {
            serialPort.writeBytes(str.getBytes());
            //this.output.write(str.getBytes());
            logger.debug("Wrote Line: " + str);
        } catch (Exception ex) {
            logger.error("Error in SerialDriver Write");
            logger.error("\t" + ex.getMessage());
        }
    }

    public void priorityWrite(String str) throws SerialPortException {
        serialPort.writeBytes(str.getBytes());
        //this.output.write(str.getBytes());
    }

    public void priorityWrite(Byte b) throws SerialPortException {
        logger.debug("[*] Priority Write Sent\n");
        serialPort.writeByte(b);
        //this.output.write(b);
    }

    public synchronized void disconnect() throws SerialPortException {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            setConnected(false); //Set our disconnected state
        }
    }

    public boolean isCANCELLED() {
        return CANCELLED;
    }

    public void setCANCELLED(boolean choice) {
        this.CANCELLED = choice;
    }

    public void setConnected(boolean c) {
        this.connectionState = c;
    }

    public String getDebugFileString() {
        return (debugFileBuffer);
    }

    public boolean isConnected() {
        return this.connectionState;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] inbuffer = new byte[1024];
        int bytesToRead;
        byte[] tmpBuffer = null;

        bytesToRead = event.getEventValue();
        //tmpBuffer = serialPort.readBytes(bytesToRead);

        if (event.isRXCHAR()) {
            try {
//                int bytesToRead = input.read(inbuffer, 0, inbuffer.length);
                tmpBuffer = serialPort.readBytes(bytesToRead, serialPort.getInputBufferBytesCount());
            } catch (SerialPortException | SerialPortTimeoutException ex) {
                logger.error(ex);
            }
            
            for (int i = 0; i < bytesToRead; i++) {
//                if (tmpBuffer[i] == 0x11 || tmpBuffer[i] == 0x13) {  //We have to filter our XON or XOFF charaters from JSON
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
//            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            SerialPort _tmpPort = new SerialPort(port);
            if (!_tmpPort.getPortName().contains("Bluetooth")) {

            }

//            if (UtilityFunctions.getOperatingSystem().equals("mac")) {
//                if (_tmpPort.getPortName().contains("tty")) {
//                    continue; //We want to remove the the duplicate tty's and just provide the "cu" ports in the drop down.
//                }
//            }

            //Go ahead and add the ports that made it though the logic above
            portList.add(_tmpPort.getPortName());
        }

        return portList.toArray(new String[0]);
    }

    public boolean initialize(String port, int DATA_RATE) throws SerialPortException {
        int TIME_OUT = 2000;

        if (isConnected()) {
            String returnMsg = "[*] Port Already Connected.\n";
            logger.info(returnMsg);
            return (true);
        }

//            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(port);
        // Get the port's ownership
//            serialPort = (SerialPort) portId("TG", TIME_OUT);
        // set port parameters
        serialPort = new SerialPort(port);
        serialPort.openPort();
        serialPort.setParams(DATA_RATE,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE);

        // open the streams
        //input = serialPort.getInputBufferBytesCount;
        //output = serialPort.getOutputStream();
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
        serialPort.setRTS(true);

        // add event listeners
        serialPort.addEventListener(this);
        //            serialPort.addEventListener(this);notifyOnDataAvailable(true);
        
        logger.debug("[+]Opened " + port + " successfully.");
        setConnected(true); //Register that this is connectionState.

        return true;
    }

}
