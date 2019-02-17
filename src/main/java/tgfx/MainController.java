package tgfx;

import java.net.URL;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import jfxtras.labs.dialogs.MonologFX;
import jfxtras.labs.dialogs.MonologFXButton;
import javafx.stage.Stage;

import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

import tgfx.system.Machine;
import tgfx.tinyg.TinygDriver;
import tgfx.system.StatusCode;
import tgfx.render.Draw2d;
import tgfx.ui.gcode.GcodeHistory;
import tgfx.ui.gcode.GcodeTabController;
import tgfx.ui.machinesettings.MachineSettingsController;
import tgfx.ui.tinygconfig.TinyGConfigController;
import tgfx.ui.firmware.FirmwareUpdaterController;
import tgfx.utility.QueueUsingTimer;
import tgfx.utility.QueuedTimerable;

import static tgfx.TgFXConstants.*;

import static tgfx.tinyg.Commands.*;

/**
 * MainController controller for the application
 *
 */
public class MainController extends Stage implements Initializable, Observer, QueuedTimerable<String> {
    private static final Logger logger = LogManager.getLogger();

    private static StringProperty consoleText =  new SimpleStringProperty();

    private TinygDriver DRIVER = TinygDriver.getInstance();
    private Machine MACHINE = DRIVER.getMachine();

    private GcodeHistory commandHistory = new GcodeHistory();

    private QueueUsingTimer<String> connectionTimer =
            new QueueUsingTimer<>( CONNECTION_TIMEOUT, this, CONNECTION_TIMEOUT_STRING);

    //Time between config set'ers.
    private int delayValue = 150;

    private int oldLineNumber = 0;

    //this is checked upon initial connect.  Once this is set to true
    private boolean buildChecked = false;

    private final static StringConverter<Number> STRING_CONVERTER = new StringConverter<Number>() {
        @Override
        public String toString(Number n) {
            return String.valueOf(n.floatValue());
        }

        @Override
        public Number fromString(String s) {
            return Integer.valueOf(s);
        }
    };


    @FXML
    @SuppressWarnings("unused") // IDE says it's unused, but don't believe it
    private GcodeTabController gcodeTabController;

    @FXML
    private TabPane topTabPane;

    @FXML
    private ChoiceBox<String> serialPorts;

    @FXML
    private Button connectBtn;

    // holds the output console
    @FXML
    private VBox consoleVBox;

    // output console
    @FXML
    TextArea console;

    // input prompt
    @FXML
    TextField input;

    // status bar
    @FXML
    private Label srMomo,
            srState,
            srBuild,
            srBuffer,
            srGcodeLine,
            srVer,
            srUnits,
            srCoord;


    /**
     * handle rescan serial
     * @param event action event
     */
    @FXML
    private void handleRescanSerial(ActionEvent event) {
        this.rescanSerial();
    }


    /**
     * handle connect
     * @param event action event
     */
    @FXML
    private void handleConnect(ActionEvent event) {
        this.doConnect();
    }


    /**
     * FIXME: re-enable this
     */
    @FXML
    private void gcodeProgramClicks(MouseEvent me) {
/*
        TextField tField = (TextField) gcodesList.getSelectionModel().getSelectedItem();
        if (me.getButton() == MouseButton.SECONDARY) {
            // DRIVER.write("{\"gc\":\"" + lbl.getText() + "\"}\n");

        } else if (me.getButton() == MouseButton.PRIMARY && me.getClickCount() == 2) {
            tField.setEditable(true);

            if (lbl.getParent().getStyleClass().contains("breakpoint")) {
                lbl.getParent().getStyleClass().remove("breakpoint");
                tgfx.MainController.postConsoleMessage("BREAKPOINT REMOVED: " + lbl.getText() + "\n");
                logger.info("BREAKPOINT REMOVED");
            } else {

                logger.info("DOUBLE CLICKED");
                lbl.getStyleClass().removeAll(null);
                lbl.getParent().getStyleClass().add("breakpoint");
                logger.info("BREAKPOINT SET");
                tgfx.MainController.postConsoleMessage("BREAKPOINT SET: " + lbl.getText() + "\n");
            }
        }
*/
    }


    /**
     * TODO: track down what was calling this
     */
    @FXML
    private void handleGuiRefresh() {
        //Refreshed all gui settings from TinyG Responses.
        if (DRIVER.isConnected().get()) {
            MainController.postConsoleMessage("System GUI Refresh Requested....");
            DRIVER.getCommandManager().queryAllHardwareAxisSettings();
            DRIVER.getCommandManager().queryAllMachineSettings();
            DRIVER.getCommandManager().queryAllMotorSettings();
        } else {
            MainController.postConsoleMessage("TinyG Not Connected.. Ignoring System GUI Refresh Request....");
        }
    }


    /**
     * handle console key press
     * @param event input event
     */
    @FXML
    private void handleKeyPress(final InputEvent event) {
        final KeyEvent keyEvent = (KeyEvent) event;

        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            String command = input.getText();

            logger.info("Entered Command: " + command);
            if (!DRIVER.isConnected().get()) {
                logger.error("TinyG is not connected....\n");
                MainController.postConsoleMessage("TinyG is not connected....\n");
                input.setPromptText(PROMPT);
                return;
            }

            // TinyG is connected... Proceed with processing command.
            // This will send the command to get a OK prompt if the buffer is empty.
            if (command.isEmpty()) {
                DRIVER.write(CMD_QUERY_OK_PROMPT);
            }

            DRIVER.write(command+"\n");
            MainController.postConsoleMessage(command);
            // Add this command to the history
            commandHistory.addCommandToHistory(command);
            input.clear();
            input.setPromptText(PROMPT);
        } else if (keyEvent.getCode().equals(KeyCode.UP)) {
            input.setText(commandHistory.getNextHistoryCommand());
            input.positionCaret(input.lengthProperty().get());
        } else if (keyEvent.getCode().equals(KeyCode.DOWN)) {
            input.setText(commandHistory.getPreviousHistoryCommand());
            input.positionCaret(input.lengthProperty().get());
        }
    }


    /**
     * @param message message
     */
    @Override
    public void addToQueue(String message) {
        DRIVER.appendJsonQueue(message);
    }


    /**
     * initialize main
     * @param url url
     * @param rb resource bundle
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("tgFX is starting....");
        gcodeTabController.setGcodeText("TinyG Disconnected.");

        // Add the tinyg driver to this observer
        DRIVER.getResponseParser().addObserver(this);
        DRIVER.addObserver(this);

        // Populate serial ports
        this.rescanSerial();

        //This disables the UI if we are not connected.
        if (!DISABLE_UI_CONNECTION_CHECK){
            consoleVBox.disableProperty().bind(DRIVER.getConnectionStatus().not());
            topTabPane.disableProperty().bind(DRIVER.getConnectionStatus().not());
        }

        // console text update binding
        console.textProperty().bindBidirectional(consoleText);

        // start the serial writer thread
        startSerialWriterThread();
        // start the response parser thread
        startResponseParserThread();

        srMomo.textProperty().bind(MACHINE.getMotionMode());
        srVer.textProperty().bind(MACHINE.getFirmwareVersion());
        srBuild.textProperty().bindBidirectional(MACHINE.getFirmwareBuild(), STRING_CONVERTER);
        srState.textProperty().bind(MACHINE.getMachineState());
        srCoord.textProperty().bind(MACHINE.getCoordinateSystem());
        srUnits.textProperty().bind(MACHINE.getGcodeUnitMode());
        srCoord.textProperty().bind(MACHINE.getGcodeCoordinateManager()
                .getCurrentGcodeCoordinateSystemName());
        srGcodeLine.textProperty().bind(MACHINE.getLineNumberSimple().asString());

    }



    /**
     * update
     * @param o observable
     * @param arg arguments
     */
    @Override
    public synchronized void update(Observable o, Object arg) {
        //We process status code messages here first.
        // TODO: can we use StatusCode.class and not the canonical name?
        if (arg.getClass().getCanonicalName().equals("tgfx.system.StatusCode")) {
            //We got an error condition.. lets route it to where it goes!
            StatusCode statuscode = (StatusCode) arg;
            MainController.postConsoleMessage("[->] TinyG Response: " + statuscode.getStatusType() +
                    ":" + statuscode.getMessage() + "\n");
        } else {
            try {
                final String[] updateMessage = (String[]) arg;
                final String routingKey = updateMessage[0];
                final String keyArgument = updateMessage[1];

                /*
                 * This is our update routing switch From here we update
                 * different parts of the GUI that is not bound to properties.
                 */
                switch (routingKey) {
                    case ROUTING_STATUS_REPORT:
                        doStatusReport();
                        break;
                    case ROUTING_CMD_GET_AXIS_SETTINGS:
                        TinyGConfigController.updateGuiAxisSettings(keyArgument);
                        break;
                    case ROUTING_CMD_GET_MACHINE_SETTINGS:
                        //updateGuiMachineSettings(ROUTING_KEY);
                        break;
                    case ROUTING_CMD_GET_MOTOR_SETTINGS:
                        TinyGConfigController.updateGuiMotorSettings(keyArgument);
                        break;
                    case ROUTING_NETWORK_MESSAGE:
                        //updateExternal();
                        break;
                    case ROUTING_MACHINE_UPDATE:
                        MachineSettingsController.updateGuiMachineSettings();
                        break;
                    case ROUTING_TEXTMODE_REPORT:
                        MainController.postConsoleMessage(keyArgument);
                        break;
                    case ROUTING_BUFFER_UPDATE:
                        srBuffer.setText(keyArgument);
                        break;
                    case ROUTING_UPDATE_LINE_NUMBER:
                        srGcodeLine.setText(keyArgument);
                        break;
                    case ROUTING_BUILD_OK:
                        doBuildOK();
                        break;
                    case ROUTING_TINYG_USER_MESSAGE:
                        doTinyGUserMessage(keyArgument);
                        break;
                    // This fires if your tinyg is not responding to tgFX in a timely manner.
                    case ROUTING_TINYG_CONNECTION_TIMEOUT:
                        doTinyGConnectionTimeout();
                        break;
                    case ROUTING_BUILD_ERROR:
                        doBuildError(keyArgument);
                        break;
                    // These 2 messages are sent when the firmware updater has begun
                    // updating or finished updating.
                    case ROUTING_DISCONNECT:
                        onDisconnectActions();
                        break;
                    case ROUTING_RECONNECT:
                        handleConnect(new ActionEvent());
                        break;
                    default:
                        logger.error("Invalid Routing Key: " + keyArgument);
                }
            } catch (SerialPortException ex) {
                logger.error(ex);
            }
        }
    }


    /**
     * update the console text property
     *
     * @param message message to send to the MainController windows console
     */
    public static void postConsoleMessage(String message){
        logger.info("postConsoleMessage : {}", message);
        consoleText.setValue(consoleText.getValueSafe() + "\n" +message);
    }


    /**
     *
     * @param keyArgument key argument
     * @throws SerialPortException exception
     */
    private void doTinyGUserMessage(String keyArgument) throws SerialPortException {
        if (keyArgument.trim().equals("SYSTEM READY")) {
            //The board has been reset and is ready to re-init our internal tgFX models
            onDisconnectActions();
            gcodeTabController.getCncMachinePane().resetDrawingCoords();
            // onConnectActions();  WE ARE DISABLING THIS FOR NOW.
            // THIS SHOULD KICK OF A RE-QUERY OF THE TINYG ON RESET.
            // HOWEVER IT IS MAKING OnConnectionActions run 2x.  Need to fix this.
        } else if (keyArgument.contains("WARNING")) {
            MainController.postConsoleMessage(keyArgument);
        }
    }


    private void doConnect(){
        Platform.runLater(() -> {
            if (serialPorts.getSelectionModel().getSelectedItem() == null) {
                MainController.postConsoleMessage("Error connecting to serial port.");
                return;
            }
            if (connectBtn.getText().equals("Connect")) {
                try {
                    logger.info("Attempting to Connect to TinyG.");
                    String serialPortSelected = serialPorts.getSelectionModel().getSelectedItem();

                    // This will be true if we connected when we tried to!
                    if (!DRIVER.initialize(serialPortSelected, SERIAL_DATA_RATE)) {
                        MainController.postConsoleMessage("There was an error connecting to " +
                                serialPortSelected + " please verify that the port is not in use.");
                    }
                    if (DRIVER.isConnected().get()) {
                        MainController.postConsoleMessage("Opened Port: " + serialPortSelected +
                                " Attempting to get TinyG Build Version Now...");
                        connectBtn.setText("Disconnect");
                        onConnectActions();
                    }
                } catch (SerialPortException ex) {
                    logger.error(ex);
                }
            } else {
                try {
                    onDisconnectActions();
                    if (!DRIVER.isConnected().get()) {
                        MainController.postConsoleMessage("Disconnected from " + DRIVER.getPortName() +
                                " Serial Port Successfully.");
                        connectBtn.setText("Connect");
                    }
                } catch (SerialPortException ex) {
                    logger.error(ex);
                }

            }
        });
    }

    /**
     * status report
     */
    private void doStatusReport() {
        gcodeTabController.drawCanvasUpdate();
        int lineNumber = DRIVER.getMachine().getLineNumber();

        // Scroll Gcode view to stay in sync with TinyG acks during file send
        if (lineNumber != oldLineNumber && gcodeTabController.isSendingFile().get()) {
            gcodeTabController.updateProgress(lineNumber);
            oldLineNumber = lineNumber;
        }
    }


    /**
     * build version is okay
     */
    private void doBuildOK() {
        // TinyG's build version is up to date to run tgfx.
        if (!buildChecked && DRIVER.isConnected().get()) {
            //we do this once on connect, disconnect will reset this flag
            onConnectActionsTwo();
        }
    }


    /**
     * error with the build version
     * @param keyValue key value
     */
    private void doBuildError(String keyValue) {
        //This is the code to manage the build error window and checking system.
        logger.error("Your TinyG firmware is too old.  System is exiting.");
        Platform.runLater(() -> {
            MonologFXButton btnYes =  new MonologFXButton();
            btnYes.setDefaultButton(true);
            btnYes.setIcon("/testmonologfx/dialog_apply.png");
            btnYes.setType(MonologFXButton.Type.YES);

            MonologFXButton btnNo =  new MonologFXButton();
            btnNo.setCancelButton(true);
            btnNo.setIcon("/testmonologfx/dialog_cancel.png");
            btnNo.setType(MonologFXButton.Type.NO);

            MonologFX mono = new MonologFX();
            mono.setTitleText("TinyG Firware Build Outdated...");
            mono.setMessage("Your TinyG firmware is too old to be used with tgFX. \n" +
                    "Your build version: " +
                    DRIVER.getMachine().getFirmwareBuild() + "\n" +
                    "Minimal Needed Version: " +
                    DRIVER.getMachine().getHardwarePlatform()
                            .getMinimalBuildVersion().toString() + "\n\n" +
                    "Click ok to attempt to auto upgrade your TinyG. \n" +
                    " A Internet Connection is Required. \n." +
                    "Clicking No will exit tgFX.");
            mono.addButton(btnYes);
            mono.addButton(btnNo);
            mono.setType(MonologFX.Type.ERROR);

            switch (mono.show()) {
                case YES:
                    logger.info("Clicked Yes");
                    WebView firmwareUpdate = new WebView();
                    final WebEngine webEngFirmware = firmwareUpdate.getEngine();
                    Stage stage = new Stage();
                    stage.setTitle("TinyG Firmware Update Guide");
                    Scene s = new Scene(firmwareUpdate, 1280, 800);

                    stage.setScene(s);
                    stage.show();

                    Platform.runLater(() -> {
                        webEngFirmware.load(FIRMWARE_UPDATE_URL);
                        try {
                            DRIVER.disconnect();
                        } catch (SerialPortException ex) {
                            logger.error(ex);
                        }
                        connectBtn.setText("Connect");
                    });
                    break;
                case NO:
                    logger.info("Clicked No");
                    try {
                        DRIVER.disconnect();
                    } catch (SerialPortException ex) {
                        logger.error(ex);
                    }
                    System.exit(0);
                    break;
            }
        });
    }


    /**
     * there was a connection timeout
     */
    private void doTinyGConnectionTimeout() {
        MainController.postConsoleMessage("ERROR! - tgFX timed out while attempting to connect to TinyG.  \n" +
                "Verify the port you selected and that power is applied to your TinyG.");
        // we set this to tell the firmware updater that we have no
        // clue what platform we are dealing with because it timed out.
        DRIVER.setTimedout(true);
        Platform.runLater(() -> {
            //set the text back to "connect" since we are disconnected
            connectBtn.setText("Connect");
            MonologFXButton btnYes = new MonologFXButton();
            btnYes.setDefaultButton(true);
            btnYes.setIcon("/testmonologfx/dialog_apply.png");
            btnYes.setType(MonologFXButton.Type.CUSTOM2);
            btnYes.setLabel("Auto Upgrade");

            MonologFXButton btnNo = new MonologFXButton();
            btnNo.setCancelButton(true);
            btnNo.setIcon("/testmonologfx/dialog_cancel.png");
            btnNo.setType(MonologFXButton.Type.CUSTOM1);
            btnNo.setLabel("Skip");

            MonologFX mono = new MonologFX();
            mono.setTitleText("TinyG Connection Timeout");
            mono.setMessage("tgFX timed out while trying to connect to your TinyG.\n" +
                    " Your TinyG might have a version of firmware that is too old or" +
                    " you might have selected the wrong serial port.\n" +
                    " Click Auto Upgrade to attempt to upgrade your TinyG." +
                    " This feature only works for TinyG boards not the Arduino" +
                    " Due port of TinyG.\n A Internet Connection is Required." +
                    " Clicking No will allow you to select a different serial" +
                    " port to try to connect to a different serial port.");
            mono.addButton(btnYes);
            mono.addButton(btnNo);
            mono.setType(MonologFX.Type.ERROR);

            switch (mono.show()) {
                case CUSTOM2:
                    logger.info("Clicked Auto Upgrade");
                    Platform.runLater(() -> {
                        FirmwareUpdaterController.handleUpdateFirmware(null);
                        try {
                            DRIVER.disconnect();
                        } catch (SerialPortException ex) {
                            logger.error(ex);
                        }
                    });
                    break;

                case CUSTOM1:
                    logger.info("Clicked No");
                    try {
                        if (DRIVER.isConnected().get()) {
                            //free up the serial port to be able to try another one.
                            DRIVER.disconnect();
                        }
                    } catch (SerialPortException ex) {
                        logger.error(ex);
                    }
                    break;
            }
        });
    }


    /**
     * rescan the serial bus for ports
     */
    private void rescanSerial() {
        serialPorts.getItems().clear();
        String[] portArray = DRIVER.listSerialPorts();
        serialPorts.getItems().addAll(Arrays.asList(portArray));
    }


    /**
     * These are the actions that need to be ran upon successful serial port
     * connection. If you have something that you want to "auto run" on connect.
     * This is the place to do so. This method is called in handleConnect.
     */
    private void onConnectActions() {
        connectionTimer = new QueueUsingTimer<>(CONNECTION_TIMEOUT, this, CONNECTION_TIMEOUT_STRING);
        Platform.runLater(() -> {
            gcodeTabController.setGcodeText("Attempting to Connect to TinyG.");
            // If the serialWriter is in a wait state.. wake it up
            DRIVER.getSerialWriter().notifyAck();
            DRIVER.write(CMD_APPLY_NOOP); //Just waking things up.
            DRIVER.write(CMD_APPLY_NOOP);
            DRIVER.write(CMD_APPLY_NOOP);
            DRIVER.write(CMD_QUERY_HARDWARE_PLATFORM);
            DRIVER.write(CMD_QUERY_HARDWARE_VERSION);
            DRIVER.write(CMD_QUERY_HARDWARE_BUILD_NUMBER);
            MainController.postConsoleMessage("Getting TinyG Firmware Build Version....");
            connectionTimer.start();
        });
    }


    /**
     * on connect actions part two
     */
    private void onConnectActionsTwo() {
        buildChecked = true;
        Platform.runLater(() -> {
            try {
                if (connectionTimer != null) {
                    connectionTimer.disarm();
                }

                // Priority Write's Must Observe the delays or you will smash TinyG
                // as it goes into a "disable interrupt mode" to write values to EEPROM
                DRIVER.write(CMD_APPLY_JSON_VERBOSITY);
                Thread.sleep(delayValue);
                DRIVER.write(CMD_APPLY_STATUS_UPDATE_INTERVAL);
                Thread.sleep(delayValue);
                DRIVER.write(CMD_APPLY_TEXT_VERBOSITY);
                Thread.sleep(delayValue);
                DRIVER.write(CMD_APPLY_FLOWCONTROL);
                Thread.sleep(delayValue);

                // Setting the status report takes some time!
                // Just leave this alone. This is a hardware limit..
                DRIVER.write(CMD_APPLY_STATUS_REPORT_FORMAT);
                Thread.sleep(600);

                // Query Code gets the regular write method
                DRIVER.getCommandManager().queryAllMachineSettings();  //SIXtH
                Thread.sleep(delayValue);
                DRIVER.getCommandManager().queryStatusReport();
                Thread.sleep(delayValue);
                DRIVER.getCommandManager().queryAllMotorSettings();
                Thread.sleep(delayValue);
                DRIVER.getCommandManager().queryAllHardwareAxisSettings();
                Thread.sleep(delayValue);
                DRIVER.write(CMD_APPLY_TEXT_VERBOSITY);

                //Once we connected we should show the drawing envelope.
                gcodeTabController.setCNCMachineVisible(true);
                MainController.postConsoleMessage("Showing CNC Machine Preview...");
                gcodeTabController.setGcodeText("TinyG Connected.");

            } catch (InterruptedException ex) {
                logger.error("Error in OnConnectActions()", ex);
            }
        });
    }


    /**
     * on disconnect actions
     * @throws SerialPortException serial port exception
     */
    private void onDisconnectActions() throws SerialPortException {
        DRIVER.disconnect();
        Platform.runLater(() -> {
            try {
                connectBtn.setText("Connect");
                Machine machine = DRIVER.getMachine();
                machine.setFirmwareBuild(0.0);
                machine.setFirmwareBuild(0);
                machine.setFirmwareVersion("");
                machine.setMachineState(0);
                machine.setLineNumber(0);
                machine.setMotionMode(0);
                Draw2d.setFirstDraw(true);
                //Once we disconnect we hide our gcode preview.
                gcodeTabController.setCNCMachineVisible(false);

                //Reset this so we can enable checking the build again on disconnect
                DRIVER.getSerialWriter().resetBuffer();
                DRIVER.getSerialWriter().clearQueueBuffer();
                DRIVER.getSerialWriter().notifyAck();
                buildChecked = false;
                gcodeTabController.setGcodeText("TinyG Disconnected.");
            } catch (JSONException ex) {
                logger.error(ex);
            }
        });

    }


    /**
     * start the serial writer thread
     */
    private void startSerialWriterThread() {
        Thread serialWriterThread = new Thread(DRIVER.getSerialWriter());
        serialWriterThread.setName("SerialWriter");
        serialWriterThread.setDaemon(true);
        serialWriterThread.start();
    }

    /**
     * start the response parser thread
     */
    private void startResponseParserThread() {
        Thread threadResponseParser = new Thread(DRIVER.getResponseParser());
        threadResponseParser.setName("ResponseParser");
        threadResponseParser.setDaemon(true);
        threadResponseParser.start();
    }
}
