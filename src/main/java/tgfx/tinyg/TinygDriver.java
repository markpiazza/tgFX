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
import tgfx.Main;
import tgfx.ResponseParser;
import tgfx.SerialDriver;
import tgfx.SerialWriter;
import tgfx.system.enums.AxisType;
import tgfx.ui.gcode.GcodeLine;
import tgfx.system.Axis;
import tgfx.system.Machine;
import tgfx.system.Motor;
import tgfx.hardwarePlatforms.HardwarePlatformManager;
import tgfx.utility.AsyncTimer;

import static tgfx.tinyg.Commands.*;
import static tgfx.tinyg.Mnemonics.*;

/**
 * TinygDriver
 *
 */
public class TinygDriver extends Observable {
    private static final Logger logger = LogManager.getLogger();

    public final static int MAX_BUFFER = 1024;

    private static TinygDriver instance;

    private final HardwarePlatformManager hardwarePlatformManager = HardwarePlatformManager.getInstance();
    private final SerialDriver serialDriver = SerialDriver.getInstance();
    private final QueueReport QUEUE = QueueReport.getInstance();
    private final Machine MACHINE = Machine.getInstance();

    private final AtomicBoolean connectionSemaphore = new AtomicBoolean(false);

    private static ArrayBlockingQueue<GcodeLine[]> writerQueue = new ArrayBlockingQueue<>(50000);
    private static ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(30);
    private static ArrayBlockingQueue<String> jsonQueue = new ArrayBlockingQueue<>(10000);

    private String[] message = new String[2];

    private SimpleBooleanProperty connectionStatus = new SimpleBooleanProperty(false);

    private MnemonicManager mnemonicManager = new MnemonicManager();
    private ResponseManager responseManager = new ResponseManager();
    private CommandManager commandManager = new CommandManager();

    private AsyncTimer connectionTimer;

    private ArrayList<String> connections = new ArrayList<>();
    private boolean paused = false;
    private boolean timedout = false;

    private ResponseParser responseParser = new ResponseParser();
    private SerialWriter serialWriter = new SerialWriter(writerQueue);


    /**
     * make TingygDriver private so the caller needs to call get instance
     */
    private TinygDriver() {
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
     * get the queue report
     * @return queue report
     */
    public QueueReport getQueryReport(){
        return QUEUE;
    }


    /**
     * get the MACHINE
     * @return MACHINE
     */
    public Machine getMachine(){
        return MACHINE;
    }


    /**
     *
     * @return
     */
    public static ArrayBlockingQueue<String> getJsonQueue() {
        return jsonQueue;
    }


    /**
     * get the platform manager
     * @return hardware platform manager
     */
    public HardwarePlatformManager getHardwarePlatformManager() {
        return hardwarePlatformManager;
    }


    /**
     * get the mnemonic manager
     * @return mnemonic manager
     */
    public MnemonicManager getMnemonicManager(){
        return mnemonicManager;
    }


    /**
     * get the response manager
     * @return response manager
     */
    public ResponseManager getResponseManager(){
        return responseManager;
    }


    /**
     * get the command manager
     * @return command manager
     */
    public CommandManager getCommandManager(){
        return commandManager;
    }


    /**
     *
     * @return SerialWriter
     */
    public SerialWriter getSerialWriter() {
        return serialWriter;
    }


    /**
     *
     * @param serialWriter
     */
    public void setSerialWriter(SerialWriter serialWriter) {
        this.serialWriter = serialWriter;
    }


    /**
     *
     * @return ResponseParser
     */
    public ResponseParser getResponseParser() {
        return responseParser;
    }


    /**
     *
     * @param responseParser
     */
    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }


    /**
     * get the connection status
     * @return connection status
     */
    public  SimpleBooleanProperty getConnectionStatus(){
        return connectionStatus;
    }


    /**
     *
     * @param value
     */
    public void setAsyncTimer(AsyncTimer value){
        connectionTimer = value;
    }


    /**
     *
     * @return
     */
    public AsyncTimer getAsyncTimer(){
        return connectionTimer;
    }


    /**
     *
     * @return
     */
    public AtomicBoolean getConnectionSemaphore(){
        return connectionSemaphore;
    }


    /**
     *
     * @return
     */
    public boolean isTimedout() {
        return timedout;
    }


    /**
     *
     * @param timedout
     */
    public void setTimedout(boolean timedout) {
        this.timedout = timedout;
    }


    /**
     *
     * @throws JSONException
     */
    public void notifyBuildChanged() throws JSONException {
        if(MACHINE.getHardwarePlatform().getMinimalBuildVersion() < this.MACHINE.getFirmwareBuildVersion()){
            // This checks to see if the current build version on
            // TinyG is greater than what tgFX's hardware profile needs.
        }

        if (MACHINE.getFirmwareBuildVersion() < MACHINE
                .getHardwarePlatform().getMinimalBuildVersion() &&
                this.MACHINE.getFirmwareBuildVersion() != 0.0) {
            // too old of a build  we need to tell the GUI about this...
            // This is where PUB/SUB will fix this
            // bad way of alerting the gui about model changes.
            message[0] = "BUILD_ERROR";
            message[1] = Double.toString(MACHINE.getFirmwareBuildVersion());
            setChanged();
            notifyObservers(message);
            logger.debug("Build Version: " + MACHINE.getFirmwareBuildVersion() + " is NOT OK");
        } else if(MACHINE.getFirmwareBuildVersion() == 0.0){
            // FIXME: remove no op
        } else {
            logger.debug("Build Version: " + MACHINE.getFirmwareBuildVersion() + " is OK");
            message[0] = "BUILD_OK";
            message[1] = null;
            setChanged();
            notifyObservers(message);
        }
    }


    /**
     *
     */
    public void sendReconnectRequest(){
        Main.postConsoleMessage("Attempting to reconnect to TinyG...");
        logger.info("Reconnect Request Sent.");
        message[0] = "RECONNECT";
        message[1] = null;
        setChanged();
        notifyObservers(message);
    }


    /**
     *
     */
    public void sendDisconnectRequest(){
        logger.info("Disconnect Request Sent.");
        message[0] = "DISCONNECT";
        message[1] = null;
        setChanged();
        notifyObservers(message);
    }


    /**
     *
     * @param c
     */
    public void queryHardwareSingleAxisSettings(char c) {
        //Our queryHardwareSingleAxisSetting function for chars
        queryHardwareSingleAxisSettings(String.valueOf(c));
    }


    /**
     *
     * @param axis axis to query
     */
    public void queryHardwareSingleAxisSettings(String axis) {
        String command = null;
        switch (axis.toLowerCase()) {
            case "x":
                command = CMD_QUERY_AXIS_X;
                break;
            case "y":
                command = CMD_QUERY_AXIS_Y;
                break;
            case "z":
                command = CMD_QUERY_AXIS_Z;
                break;
            case "a":
                command = CMD_QUERY_AXIS_A;
                break;
            case "b":
                command = CMD_QUERY_AXIS_B;
                break;
            case "c":
                command = CMD_QUERY_AXIS_C;
                break;
        }
        if(command!=null){
            serialDriver.write(command);
        }
    }


    /**
     *
     * @param tab
     */
    public void applyHardwareAxisSettings(Tab tab) {
        GridPane gridPane = (GridPane) tab.getContent();
        Axis axis = this.MACHINE.getAxisByName(String.valueOf(gridPane.getId().charAt(0)));

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
                @SuppressWarnings("unchecked")
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
     *
     * @param motor
     * @param textField
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
     *
     * @param axis
     * @param textField
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
     *
     * @param motorNumber
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
     *
     * @param rc
     */
    public void applyResponseCommand(ResponseCommand rc) {
        char _ax;
        switch (rc.getSettingKey()) {
            case (MNEMONIC_STATUS_REPORT_LINE):
                MACHINE.setLineNumber(Integer.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_MOTION_MODE):
                TinygDriver.logger.debug("[DID NOT APPLY NEED TO CODE THIS IN:" +
                        rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
//                getInstance().m.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_POSA):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                MACHINE.getAxisByName(String.valueOf(_ax))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_POSX):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                MACHINE.getAxisByName(String.valueOf(_ax))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_POSY):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                MACHINE.getAxisByName(String.valueOf(_ax))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_POSZ):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                MACHINE.getAxisByName(String.valueOf(_ax))
                        .setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_STAT):
                //TinygDriver.getInstance()(Float.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            case (MNEMONIC_STATUS_REPORT_VELOCITY):
                MACHINE.setVelocity(Double.valueOf(rc.getSettingValue()));
                logger.debug("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
            default:
                logger.error("[ERROR] in ApplyResponseCommand:  Command Was:" +
                        rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
        }
    }


    /**
     *
     * @param tab
     */
    public void applyHardwareMotorSettings(Tab tab) {
        /*
         * Apply Motor Settings to TinyG from GUI
         */
        Tab selectedTab = tab.getTabPane().getSelectionModel().getSelectedItem();
        int motorNumber = Integer.valueOf(selectedTab.getText().split(" ")[1]);
        Motor motor = MACHINE.getMotorByNumber(motorNumber);

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
                        case "X":
                            mapAxis = 0;
                            break;
                        case "Y":
                            mapAxis = 1;
                            break;
                        case "Z":
                            mapAxis = 2;
                            break;
                        case "A":
                            mapAxis = 3;
                            break;
                        case "B":
                            mapAxis = 4;
                            break;
                        case "C":
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
     *
     * @param motorNumber
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
     *
     * @param obsrvr
     */
    @Override
    public synchronized void addObserver(Observer obsrvr) {
        super.addObserver(obsrvr);
    }


    /**
     *
     * @param line
     */
    public void appendJsonQueue(String line) {
        // This adds full normalized json objects to our jsonQueue.
        getJsonQueue().add(line);
    }


    /**
     *
     * @param queue
     */
    public synchronized void appendResponseQueue(byte[] queue) {
        // Add byte arrays to the buffer queue from tinyG's responses.
        try {
            TinygDriver.queue.put(queue);
        } catch (InterruptedException e) {
            logger.error("ERROR appending to the Response Queue");
        }
    }


    /**
     *
     * @return
     */
    public boolean isPaused() {
        return paused;
    }


    /**
     *
     * @param choice
     * @throws SerialPortException
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
     *
     * @param choice
     */
    public void setConnected(boolean choice) {
        serialDriver.setConnected(choice);
    }


    /**
     *
     * @param portName
     * @param dataRate
     * @return
     * @throws SerialPortException
     */
    public boolean initialize(String portName, int dataRate) throws SerialPortException {
        return serialDriver.initialize(portName, dataRate);
    }


    /**
     *
     * @throws SerialPortException
     */
    public void disconnect() throws SerialPortException {
        serialDriver.disconnect();
    }


    /**
     *
     * @return
     */
    public SimpleBooleanProperty isConnected() {
        //Our binding to keep tabs in the us of if we are connected to TinyG or not.
        //This is mostly used to disable the UI if we are not connected.
        connectionStatus.set(serialDriver.isConnected());
        return connectionStatus;
    }


    /**
     *
     * @param msg
     */
    public synchronized void write(String msg) {
        getSerialWriter().addCommandToBuffer(msg);
    }


    /**
     *
     * @param b
     * @throws SerialPortException
     */
    public void priorityWrite(Byte b) throws SerialPortException {
        serialDriver.priorityWrite(b);
    }


    /**
     *
     * @param msg
     */
    public void priorityWrite(String msg){
        logger.info("Priority write: {}", msg);
        if (!msg.contains("\n")) {
            msg = msg + "\n";
        }
        serialDriver.write(msg);
    }


    /**
     *
     * @return
     */
    public String[] listSerialPorts() {
        // Get a listing current system serial ports
        return serialDriver.listSerialPorts();
    }


    /**
     *
     * @return
     */
    public String getPortName() {
        // Return the serial port name that is connected.
        return serialDriver.getSerialPort().getPortName();
    }


    /**
     *
     * @return
     */
    public List<Axis> getInternalAllAxis() {
        return MACHINE.getAllAxis();
    }


}