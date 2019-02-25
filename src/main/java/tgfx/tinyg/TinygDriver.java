package tgfx.tinyg;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import tgfx.MainController;
import tgfx.SerialDriver;
import tgfx.SerialWriter;
import tgfx.TgFXConstants;
import tgfx.system.enums.AxisType;
import tgfx.ui.gcode.GcodeLine;
import tgfx.system.Axis;
import tgfx.system.Machine;
import tgfx.system.Motor;
import tgfx.hardwarePlatforms.HardwarePlatformManager;
import tgfx.utility.AsyncTimer;

import static tgfx.tinyg.CommandConstants.*;
import static tgfx.tinyg.MnemonicConstants.*;

/**
 * TinygDriver
 *
 */
public class TinygDriver extends Observable {
    private static final Logger logger = LogManager.getLogger();

    final static int MAX_BUFFER = 1024;

    private static TinygDriver instance;

    private SerialDriver serialDriver;
    private HardwarePlatformManager hardwarePlatformManager;
    private ResponseParser responseParser;
    private SerialWriter serialWriter;
    private MnemonicManager mnemonicManager;
    private CommandManager commandManager;
    private QueueReport queueReport;
    private Machine machine;
    private AsyncTimer connectionTimer;

    private final AtomicBoolean connectionSemaphore = new AtomicBoolean(false);

    private static ArrayBlockingQueue<GcodeLine[]> writerQueue = new ArrayBlockingQueue<>(50000);
    private static ArrayBlockingQueue<String> jsonQueue = new ArrayBlockingQueue<>(10000);
    private static ArrayBlockingQueue<byte[]> responseQueue = new ArrayBlockingQueue<>(30);

    private SimpleBooleanProperty connectionStatus;

    private String[] message = new String[2];
    private boolean paused = false;
    private boolean timedout = false;

    public static final String X="x", Y="y", Z="z", A="a", B="b", C="c";


    /**
     * make TingygDriver private so the caller needs to call get instance
     */
    private TinygDriver() {
        logger.info("Setting up TinygDriver");
        serialDriver = new SerialDriver();
        serialWriter = new SerialWriter(serialDriver, writerQueue);
        hardwarePlatformManager = new HardwarePlatformManager(this);
        responseParser = new ResponseParser(this);
        commandManager = new CommandManager(this);
        mnemonicManager = new MnemonicManager();
        queueReport = new QueueReport();
        machine = new Machine();

        connectionStatus = new SimpleBooleanProperty(false);
    }


    /**
     * get an instance of TingyDriver
     * @return static instance of TinygDriver
     */
    public static TinygDriver getInstance() {
        if(instance == null){
            instance = new TinygDriver();
        }
        return instance;
    }


    /**
     * get serial writer
     * @return SerialWriter
     */
    public SerialWriter getSerialWriter() {
        return serialWriter;
    }


    /**
     * get the platform manager
     * @return hardware platform manager
     */
    public HardwarePlatformManager getHardwarePlatformManager() {
        return hardwarePlatformManager;
    }


    /**
     * get response parser
     * @return ResponseParser
     */
    public ResponseParser getResponseParser() {
        return responseParser;
    }


    /**
     * get the command manager
     * @return command manager
     */
    public CommandManager getCommandManager(){
        return commandManager;
    }


    /**
     * get the mnemonic manager
     * @return mnemonic manager
     */
    public MnemonicManager getMnemonicManager(){
        return mnemonicManager;
    }


    /**
     * get the queue report
     * @return queue report
     */
    public QueueReport getQueryReport(){
        return queueReport;
    }


    /**
     * get the machine
     * @return machine
     */
    public Machine getMachine(){
        return machine;
    }


    /**
     * get json queue
     * @return json queue
     */
    public static ArrayBlockingQueue<String> getJsonQueue() {
        return jsonQueue;
    }


    /**
     * get the connection status
     * @return connection status
     */
    public  SimpleBooleanProperty getConnectionStatus(){
        return connectionStatus;
    }


    /**
     * get connection timer
     * @return connection timer
     */
    public AsyncTimer getAsyncTimer(){
        return connectionTimer;
    }


    /**
     * set connection timer
     * @param connectionTimer connection timer
     */
    public void setAsyncTimer(AsyncTimer connectionTimer){
        this.connectionTimer = connectionTimer;
    }


    /**
     * get connection semaphore
     * @return connection semaphore
     */
    public AtomicBoolean getConnectionSemaphore(){
        return connectionSemaphore;
    }


    /**
     * is timed out
     * @return is timed out
     */
    public boolean isTimedout() {
        return timedout;
    }


    /**
     * set timed out
     * @param timedout is timed out
     */
    public void setTimedout(boolean timedout) {
        this.timedout = timedout;
    }


    /**
     * notify build version change
     * @throws JSONException json exception
     */
    public void notifyBuildChanged() throws JSONException {
        if (machine.getFirmwareBuild() < machine
                .getHardwarePlatform().getMinimalBuildVersion() &&
                this.machine.getFirmwareBuild() != 0.0) {
            // too old of a build  we need to tell the GUI about this...
            // This is where PUB/SUB will fix this
            // bad way of alerting the gui about model changes.
            message[0] = TgFXConstants.ROUTING_BUILD_ERROR;
            message[1] = Double.toString(machine.getFirmwareBuild());
            setChanged();
            notifyObservers(message);
            logger.debug("Build Version: " + machine.getFirmwareBuild() + " is NOT OK");
        } else if(machine.getFirmwareBuild() != 0.0){
            logger.debug("Build Version: " + machine.getFirmwareBuild() + " is OK");
            message[0] = TgFXConstants.ROUTING_BUILD_OK;
            message[1] = null;
            setChanged();
            notifyObservers(message);
        }
    }


    /**
     * send reconnect request
     */
    public void sendReconnectRequest(){
        MainController.postConsoleMessage("Attempting to reconnect to TinyG...");
        logger.info("Reconnect Request Sent.");
        message[0] = TgFXConstants.ROUTING_RECONNECT;
        message[1] = null;
        setChanged();
        notifyObservers(message);
    }


    /**
     * send disconnect request
     */
    public void sendDisconnectRequest(){
        logger.info("Disconnect Request Sent.");
        message[0] = TgFXConstants.ROUTING_DISCONNECT;
        message[1] = null;
        setChanged();
        notifyObservers(message);
    }


    /**
     * query hardware single axis settings by character
     * @param c axis to query
     */
    public void queryHardwareSingleAxisSettings(char c) {
        queryHardwareSingleAxisSettings(String.valueOf(c));
    }


    /**
     * query hardware single axis settings by string
     * @param axis axis to query
     */
    public void queryHardwareSingleAxisSettings(String axis) {
        String command = null;
        switch (axis.toLowerCase()) {
            case X:
                command = CMD_QUERY_AXIS_X;
                break;
            case Y:
                command = CMD_QUERY_AXIS_Y;
                break;
            case Z:
                command = CMD_QUERY_AXIS_Z;
                break;
            case A:
                command = CMD_QUERY_AXIS_A;
                break;
            case B:
                command = CMD_QUERY_AXIS_B;
                break;
            case C:
                command = CMD_QUERY_AXIS_C;
                break;
        }
        if(command!=null){
            serialDriver.write(command);
        }
    }



    /**
     * apply hardware settings to tab
     * @param tab
     */
    public void applyHardwareAxisSettings(Tab tab) {
        GridPane gridPane = (GridPane) tab.getContent();
        Axis axis = this.machine.getAxisByName(String.valueOf(gridPane.getId().charAt(0)));

        if(axis==null){
            logger.error("Invalid Axis)");
            return;
        }

        for(Node node : gridPane.getChildren()) {
            // FIXME: something not right about this..
            if (node.getClass().toString().contains("TextField")) {
                //This ia a TextField... Lets get the value and apply it if it needs to be applied.
                 applyHardwareAxisSettings(axis, (TextField) node);

            } else if (node instanceof ChoiceBox) {
                //This ia a ChoiceBox... Lets get the value and apply it if it needs to be applied.
                ChoiceBox<Object> choiceBox = (ChoiceBox<Object>) node;
                if (choiceBox.getId().contains("AxisMode")) {
                    int axisMode = choiceBox.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            axis.getAxisName().toLowerCase(), MNEMONIC_AXIS_AXIS_MODE, axisMode);
                    this.write(configObj);
                } else if (choiceBox.getId().contains("switchModeMax")) {
                    int switchMode = choiceBox.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            axis.getAxisName().toLowerCase(), MNEMONIC_AXIS_MAX_SWITCH_MODE, switchMode);
                    this.write(configObj);
                } else if (choiceBox.getId().contains("switchModeMin")) {
                    int switchMode = choiceBox.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            axis.getAxisName().toLowerCase(), MNEMONIC_AXIS_MIN_SWITCH_MODE, switchMode);
                    this.write(configObj);
                }
            }
        }
        logger.info("Applying Axis Settings...");
    }


    /**
     * apply hardware motor settings
     * @param motor moter
     * @param textField field
     */
    public void applyHardwareMotorSettings(Motor motor, TextField textField)  {
        if (textField.getId().contains("StepAngle")) {
            if (motor.getStepAngle() != Float.valueOf(textField.getText())) {
                this.write("{\"" + motor.getIdNumber() +
                    MNEMONIC_MOTOR_STEP_ANGLE + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("TravelPer")) {
            if (motor.getStepAngle() != Float.valueOf(textField.getText())) {
                this.write("{\"" + motor.getIdNumber() +
                    MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION + "\":" + textField.getText() + "}\n");
            }
        }
    }


    /**
     * apply hardware axis settings
     * @param axis axis
     * @param textField field
     */
    public void applyHardwareAxisSettings(Axis axis, TextField textField) {
        /*
         * Apply Axis Settings to TinyG from GUI
         */
        if (textField.getId().contains("maxVelocity")) {
            if (axis.getVelocityMaximum() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_VELOCITY_MAXIMUM + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("maxFeed")) {
            if (axis.getFeedRateMaximum() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_FEEDRATE_MAXIMUM + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("maxTravel")) {
            if (axis.getTravelMaximum() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_TRAVEL_MAXIMUM + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("maxJerk")) {
            if (axis.getJerkMaximum() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_JERK_MAXIMUM + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("junctionDeviation")) {
            if (Double.valueOf(axis.getJunctionDeviation()).floatValue() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_JUNCTION_DEVIATION + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("radius")) {
            if (axis.getAxisType().equals(AxisType.ROTATIONAL)) {
                //Check to see if its a ROTATIONAL AXIS...
                if (axis.getRadius() != Double.valueOf(textField.getText())) {
                    this.write("{\"" + axis.getAxisName().toLowerCase() +
                            MNEMONIC_AXIS_RADIUS + "\":" + textField.getText() + "}\n");
                }
            }
        } else if (textField.getId().contains("searchVelocity")) {
            if (axis.getSearchVelocity() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_SEARCH_VELOCITY + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("latchVelocity")) {
            if (axis.getLatchVelocity() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_LATCH_VELOCITY + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("latchBackoff")) {
            if (axis.getLatchBackoff() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_LATCH_BACKOFF + "\":" + textField.getText() + "}\n");
            }
        } else if (textField.getId().contains("zeroBackoff")) {
            if (axis.getZeroBackoff() != Double.valueOf(textField.getText())) {
                this.write("{\"" + axis.getAxisName().toLowerCase() +
                        MNEMONIC_AXIS_ZERO_BACKOFF + "\":" + textField.getText() + "}\n");
            }
        }
        logger.info("Applying " + axis.getAxisName() + " settings");
    }


    /**
     * get motor settings by number
     * @param motorNumber motor number
     */
    public void getMotorSettings(int motorNumber) {
        String command = null;
        switch (motorNumber) {
            case 1:
                command = CMD_QUERY_MOTOR_1_SETTINGS;
                break;
            case 2:
                command = CMD_QUERY_MOTOR_2_SETTINGS;
                break;
            case 3:
                command = CMD_QUERY_MOTOR_3_SETTINGS;
                break;
            case 4:
                command = CMD_QUERY_MOTOR_4_SETTINGS;
                break;
            default:
                logger.error("Invalid Motor Number.. Please try again..");
                break;
        }
        if(command!=null){
            serialDriver.write(command);
        }
    }


    /**
     * apply response command
     * @param rc response command
     */
    public void applyResponseCommand(ResponseCommand rc) {
        char axis;
        switch (rc.getSettingKey()) {
            case MNEMONIC_STATUS_REPORT_LINE:
                machine.setLineNumber(Integer.valueOf(rc.getSettingValue()));
                logger.info("applied line number: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_MOTION_MODE:
                machine.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                logger.info("not applying motion mode: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_POSA:
                axis = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                machine.getAxisByName(String.valueOf(axis))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("applied POS A: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_POSX:
                axis = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                machine.getAxisByName(String.valueOf(axis))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("applied POS X: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_POSY:
                axis = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                machine.getAxisByName(String.valueOf(axis))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("applied POS Y: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_POSZ:
                axis = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                machine.getAxisByName(String.valueOf(axis))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("applied POS Z: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_STAT:
                //TinygDriver.getInstance()(Float.valueOf(rc.getSettingValue()));
                logger.info("not applying stat: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_STATUS_REPORT_VELOCITY:
                machine.setVelocity(Double.valueOf(rc.getSettingValue()));
                logger.info("applied velocity: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            default:
                logger.error("unknown status report property: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
        }
    }


    /**
     * apply hardware motor settings by tab
     * @param tab tab
     */
    public void applyHardwareMotorSettings(Tab tab) {
        /*
         * Apply Motor Settings to TinyG from GUI
         */
        Tab selectedTab = tab.getTabPane().getSelectionModel().getSelectedItem();
        int motorNumber = Integer.valueOf(selectedTab.getText().split(" ")[1]);
        Motor motor = machine.getMotorByNumber(motorNumber);

        GridPane gridPane = (GridPane) tab.getContent();

        //Iterate though each gridpane child... Picking out text fields and choice boxes

        for(Node node : gridPane.getChildren()){
            if (node.toString().contains("TextField")) {
                TextField textField = (TextField) node;
                applyHardwareMotorSettings(motor, textField);
            } else if (node instanceof ChoiceBox) {
                @SuppressWarnings("unchecked")
                ChoiceBox<Object> choiceBox = (ChoiceBox<Object>) node;
                if (choiceBox.getId().contains("MapAxis")) {
                    int mapAxis;
                    switch (choiceBox.getSelectionModel().getSelectedItem().toString()) {
                        case X:
                            mapAxis = 0;
                            break;
                        case Y:
                            mapAxis = 1;
                            break;
                        case Z:
                            mapAxis = 2;
                            break;
                        case A:
                            mapAxis = 3;
                            break;
                        case B:
                            mapAxis = 4;
                            break;
                        case C:
                            mapAxis = 5;
                            break;
                        default:
                            mapAxis = 0;  //Defaults to map to X
                    }
                    String configObj = String.format("{\"%s\":{\"%s\":%s}}\n",
                            motorNumber, MNEMONIC_MOTOR_MAP_AXIS, mapAxis);
                    this.write(configObj);

                } else if (choiceBox.getId().contains("MicroStepping")) {
                    //This is the MapAxis Choice Box... Lets apply that
                    int microSteps;
                    switch (choiceBox.getSelectionModel().getSelectedIndex()) {
                        case 0:
                            microSteps = 1;
                            break;
                        case 1:
                            microSteps = 2;
                            break;
                        case 2:
                            microSteps = 4;
                            break;
                        case 3:
                            microSteps = 8;
                            break;
                        default:
                            microSteps = 1;
                    }
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            motorNumber, MNEMONIC_MOTOR_MICROSTEPS,
                            microSteps);
                    this.write(configObj);

                } else if (choiceBox.getId().contains("Polarity")) {
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            motorNumber, MNEMONIC_MOTOR_POLARITY,
                            choiceBox.getSelectionModel().getSelectedIndex());
                    this.write(configObj);

                } else if (choiceBox.getId().contains("PowerMode")) {
                    String configObj = String.format("{\"%s%s\":%s}\n",
                            motorNumber, MNEMONIC_MOTOR_POWER_MANAGEMENT,
                            choiceBox.getSelectionModel().getSelectedIndex());
                    this.write(configObj);
                }
            }
        }
    }


    /**
     * query hardware single motor settings by motor number
     * @param motorNumber motor number
     */
    public void queryHardwareSingleMotorSettings(int motorNumber) {
        String command = null;
        switch (motorNumber) {
            case 1:
                command = CMD_QUERY_MOTOR_1_SETTINGS;
                break;
            case 2:
                command = CMD_QUERY_MOTOR_2_SETTINGS;
                break;
            case 3:
                command = CMD_QUERY_MOTOR_3_SETTINGS;
                break;
            case 4:
                command = CMD_QUERY_MOTOR_4_SETTINGS;
                break;
            default:
                logger.warn("Invalid Motor Number.. Please try again..");
                setChanged();
                break;
        }
        if(command!=null){
            serialDriver.write(command);
        }
    }


    /**
     * add observer
     * @param observer observer
     */
    @Override
    public synchronized void addObserver(Observer observer) {
        super.addObserver(observer);
    }


    /**
     * append json queue
     * @param line line to queue
     */
    public void appendJsonQueue(String line) {
        // This adds full normalized json objects to our jsonQueue.
        getJsonQueue().add(line);
    }


    /**
     * append response responseQueue
     * @param queue bytes to append
     */
    public synchronized void appendResponseQueue(byte[] queue) {
        // Add byte arrays to the buffer responseQueue from tinyG's responses.
        try {
            responseQueue.put(queue);
        } catch (InterruptedException e) {
            logger.error("ERROR appending to the Response Queue");
        }
    }


    /**
     * is paused
     * @return is paused
     */
    public boolean isPaused() {
        return paused;
    }


    /**
     * set paused
     * @param choice is paused
     * @throws SerialPortException serial exception
     */
    public void setPaused(boolean choice) throws SerialPortException {
        if (choice) { // if set to pause
            serialDriver.priorityWrite(CMD_APPLY_PAUSE);
            paused = choice;
        } else { // set to resume
            serialDriver.priorityWrite(CMD_QUERY_OK_PROMPT);
            serialDriver.priorityWrite(CMD_APPLY_RESUME);
            serialDriver.priorityWrite(CMD_QUERY_OK_PROMPT);
            paused = false;
        }
    }


    /**
     * set connected
     * @param choice is connected
     */
    public void setConnected(boolean choice) {
        serialDriver.setConnected(choice);
    }


    /**
     * initialize serial port
     * @param portName port name
     * @param dataRate data rate
     * @return success
     * @throws SerialPortException serial port exception
     */
    public boolean initialize(String portName, int dataRate) throws SerialPortException {
        return serialDriver.initialize(portName, dataRate);
    }


    /**
     * disconnect
     * @throws SerialPortException serial port exception
     */
    public void disconnect() throws SerialPortException {
        serialDriver.disconnect();
    }


    /**
     * is connected
     * @return is connected
     */
    public SimpleBooleanProperty isConnected() {
        //Our binding to keep tabs in the us of if we are connected to TinyG or not.
        //This is mostly used to disable the UI if we are not connected.
        connectionStatus.set(serialDriver.isConnected());
        return connectionStatus;
    }


    /**
     * write
     * @param msg message to write
     */
    public synchronized void write(String msg) {
        serialWriter.addCommandToBuffer(msg);
    }


    /**
     * priority write byte
     * @param b byte to write
     * @throws SerialPortException serial port exception
     */
    public void priorityWrite(Byte b) throws SerialPortException {
        serialDriver.priorityWrite(b);
    }


    /**
     * priority write string
     * @param msg message string to write
     */
    public void priorityWrite(String msg){
        logger.info("Priority write: {}", msg.replace("\n",""));
        if (!msg.contains("\n")) {
            msg = msg + "\n";
        }
        serialDriver.write(msg);
    }


    /**
     * list serial ports available
     * @return array of serial port names
     */
    public String[] listSerialPorts() {
        // Get a listing current system serial ports
        return serialDriver.listSerialPorts();
    }


    /**
     * get port name
     * @return port name
     */
    public String getPortName() {
        // Return the serial port name that is connected.
        return serialDriver.getSerialPort().getPortName();
    }


    /**
     * get internal all axis
     * @return list of axises
     */
    public List<Axis> getInternalAllAxis() {
        return machine.getAllAxis();
    }

}