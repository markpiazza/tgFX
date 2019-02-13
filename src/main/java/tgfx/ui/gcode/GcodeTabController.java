package tgfx.ui.gcode;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import eu.hansolo.medusa.Gauge;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.Main;
import tgfx.SerialWriter;
import tgfx.TgFXConstants;
import tgfx.render.CNCMachine;
import tgfx.render.Draw2d;
import tgfx.system.Machine;
import tgfx.system.enums.GcodeDistanceMode;
import tgfx.tinyg.CommandManager;
import tgfx.tinyg.TinygDriver;
import tgfx.ui.tgfxsettings.TgfxSettingsController;


import static tgfx.tinyg.Commands.*;

/**
 * GcodeTabController
 *
 * FXML Controller class
 *
 */
public class GcodeTabController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    private static TinygDriver DRIVER = TinygDriver.getInstance();
    private static Machine MACHINE = DRIVER.getMachine();
    private static SerialWriter WRITER = DRIVER.getSerialWriter();

    private CommandManager commandManager = new CommandManager();

    private static final byte[] BAD_BYTES = {(byte) 0x21, (byte) 0x18, (byte) 0x7e};
    private static double TRAVERSE_FEED_RATE = 1;  //%100
    private static double NUDGE_FEED_RATE = .05;  //%5

    private double feedRatePercentage = NUDGE_FEED_RATE;

    private Date timeStartDt;
    private int totalGcodeLines = 0;

    private String buildDate;
    private int buildNumber;

    private double scaleAmount;
    private double jogDial = 0;
    private float zScale = 0.1f;

    private boolean taskActive = false;
    private boolean isKeyPressed = false;

    /*
     * Data models for FXML elements
     */
    private static SimpleBooleanProperty isSendingFile = new SimpleBooleanProperty(false);
    private static SimpleBooleanProperty cncMachineVisible = new SimpleBooleanProperty(true);
    private static SimpleStringProperty gcodeStatusMessageValue = new SimpleStringProperty("");
    private static SimpleBooleanProperty gcodeStatusMessageVisible = new SimpleBooleanProperty(false);

    private static SimpleStringProperty xPosition = new SimpleStringProperty("");
    private static SimpleStringProperty yPosition = new SimpleStringProperty("");

    private static SimpleStringProperty timeElapsed = new SimpleStringProperty("00:00");
    private static SimpleStringProperty timeLeft = new SimpleStringProperty("00:00");

    //List to store the gcode file
    private ObservableList<GcodeLine> data;


    /*  ######################## FXML ELEMENTS ############################*/
    @FXML
    private HBox gcodeTabHbox; // commented out

    /* left vbox */

    @FXML
    private TableView<GcodeLine> gcodeView;
    @FXML
    private TableColumn<GcodeLine, String> gcodeCol;
    @FXML
    private Button pauseResumeBtn;

    /* center hbox */

    @FXML
    private StackPane gcodePane;

    //lives inside of the gcodePane
    @FXML
    private CNCMachine cncMachinePane;

    // below the gcode Pane
    @FXML
    private GridPane coordLocationGridPane;

    @FXML
    private Text timeElapsedTxt,
            timeLeftTxt,
            xAxisLocationTxt,
            yAxisLocationTxt,
            gcodeStatusMessageTxt;

    /* right vbox */

    @FXML
    private VBox lcdGauges;

    @FXML
    private Gauge xLcd,
            yLcd,
            zLcd,
            aLcd,
            velLcd;

//    @FXML
//    private ChoiceBox<?> zMoveScaleChoiceBox;


    /**
     * handleHomeXYZ
     *
     * @param evt action event
     */
    @FXML
    private void handleHomeXYZ(ActionEvent evt) {
        logger.info("handleHomeXYZ");
        if (DRIVER.isConnected().get()) {
            DRIVER.write(CMD_APPLY_SYSTEM_HOME_XYZ_AXES);
        }
    }


    /**
     * handleHomeAxisClick
     *
     * @param evt action event
     */
    @FXML
    private void handleHomeAxisClick(ActionEvent evt) {
        logger.info("handleHomeAxisClick");
        MenuItem m = (MenuItem) evt.getSource();
        String axis = String.valueOf(m.getId().charAt(0));

        if (DRIVER.isConnected().get()) {
            switch (axis) {
                case "x":
                    DRIVER.write(CMD_APPLY_HOME_X_AXIS);
                    break;
                case "y":
                    DRIVER.write(CMD_APPLY_HOME_Y_AXIS);
                    break;
                case "z":
                    DRIVER.write(CMD_APPLY_HOME_Z_AXIS);
                    break;
                case "a":
                    DRIVER.write(CMD_APPLY_HOME_A_AXIS);
                    break;
            }
        }
        Main.postConsoleMessage("Homing " + axis.toUpperCase() + " Axis...\n");
    }


    /**
     * handleZeroAxisClick
     *
     * @param evt action event
     */
    @FXML
    private void handleZeroAxisClick(ActionEvent evt) {
        logger.info("handleZeroAxisClick");
        MenuItem m = (MenuItem) evt.getSource();
        String axis = String.valueOf(m.getId().charAt(0));

        if (DRIVER.isConnected().get()) {
            //We set this so we do not draw lines for the previous position to the new zero.
            Draw2d.setFirstDraw(true);
            switch (axis) {
                case "x":
                    DRIVER.write(CMD_APPLY_ZERO_X_AXIS);
                    break;
                case "y":
                    DRIVER.write(CMD_APPLY_ZERO_Y_AXIS);
                    break;
                case "z":
                    DRIVER.write(CMD_APPLY_ZERO_Z_AXIS);
                    break;
                case "a":
                    DRIVER.write(CMD_APPLY_ZERO_A_AXIS);
                    break;
            }
        }
        Main.postConsoleMessage("Zeroed " + axis.toUpperCase() + " Axis...\n");
    }


    /**
     * handleDroMouseClick
     *
     * @param me mouse event
     */
    @FXML
    private void handleDroMouseClick(MouseEvent me) {
        logger.info("handleDroMouseClick");
        if (me.isSecondaryButtonDown()) { //Check to see if its a Right Click
            Gauge l = (Gauge) me.getSource();
            String t = String.valueOf(l.idProperty().get().charAt(0));
        }
    }


    /**
     * handleZeroSystem
     *
     * @param evt action event
     */
    @FXML
    private void handleZeroSystem(ActionEvent evt) {
        logger.info("handleSystemZero");
        cncMachinePane.zeroSystem();
    }


    /**
     * handlePauseResume
     *
     * @param evt action event
     */
    @FXML
    private void handlePauseResume(ActionEvent evt) {
        logger.info("handlePauseResume");
        if ("Pause".equals(pauseResumeBtn.getText())) {
            pauseResumeBtn.setText("Resume");
            DRIVER.priorityWrite(CMD_APPLY_PAUSE);
        } else {
            pauseResumeBtn.setText("Pause");
            DRIVER.priorityWrite(CMD_APPLY_RESUME);
        }
    }


    /**
     * handleClearScreen
     *
     * @param evt action event
     */
    @FXML
    private void handleClearScreen(ActionEvent evt) {
        logger.info("handleClearScreen");
        Main.postConsoleMessage("Clearing Screen...\n");
        cncMachinePane.clearScreen();
        //clear this so our first line added draws correctly
        Draw2d.setFirstDraw(true);
    }


    /**
     * handleReset
     *
     * @param evt action event
     */
    @FXML
    private void handleReset(ActionEvent evt) {
        logger.info("handleReset");
        Platform.runLater(() -> {
            try {
                DRIVER.getSerialWriter().clearQueueBuffer();
                //This sends the 0x18 byte
                DRIVER.priorityWrite(CMD_APPLY_RESET);

                //We disable everything while waiting for the board to reset
//                 topAnchorPane.setDisable(true);
//                 topTabPane.setDisable(true);

//                Thread.sleep(8000);
//                onConnectActions();
                Main.postConsoleMessage("Resetting TinyG....\n.");
                DRIVER.getSerialWriter().notifyAck();
                DRIVER.getSerialWriter().clearQueueBuffer();
                cncMachinePane.clearScreen();
                // We set this to false to allow us to jog again
                isSendingFile.set(false);
            } catch (SerialPortException ex) {
                logger.error(ex);
            }
        });
    }


    /**
     * handleStop
     *
     * @param evt action event
     */
    @FXML
    private void handleStop(ActionEvent evt) {
        logger.info("handleStop");
        Platform.runLater(() -> {
            logger.info("Stopping Job Clearing Serial Queue...\n");
            commandManager.stopTinyGMovement();
            // We set this to false to allow us to jog again
            isSendingFile.set(false);
        });
    }


    /**
     * handleOpenFile
     *
     * @param event action event
     */
    @FXML
    private void handleOpenFile(ActionEvent event) {
        logger.info("handleOpenFile");
        Platform.runLater(() -> {
            try {
                Main.postConsoleMessage("Loading a gcode file.....\n");
                // FIXME: Canceling FileChooser, throws a FileNotFoundException, handle this better
                FileChooser fc = new FileChooser();
                fc.setTitle("Open GCode File");

                // TODO: Save last directory for easier browsing?
                String HOME_DIR = System.getenv("HOME"); //Get Home DIR in OSX
                if (HOME_DIR == null) {
                    HOME_DIR = System.getProperty("user.home");  //Get Home DIR in Windows
                }

                fc.setInitialDirectory(new File(HOME_DIR));
                File f = fc.showOpenDialog(null);
                FileInputStream fstream = new FileInputStream(f);
                DataInputStream in = new DataInputStream((fstream));
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;

                //Clear the list if there was a previous file loaded
                data.clear();
                int lineNumber = 0;
                while ((strLine = br.readLine()) != null) {
                    if (!strLine.equals("")) {
                        //Do not add empty lines to the list
                        //gcodesList.appendText(strLine + "\n");
                        if (!strLine.toUpperCase().startsWith("N")) {
                            strLine = "N" + lineNumber + " " + strLine;
                        }
                        if (normalizeGcodeLine(strLine)) {
                            data.add(new GcodeLine(strLine, lineNumber));
                            lineNumber++;
                        } else {
                            Main.postConsoleMessage("ERROR: Your gcode file contains an invalid character.. " +
                                    "Either !,% or ~. Remove this character and try again.");
                            Main.postConsoleMessage("  Line " + lineNumber);
                            data.clear(); //Remove all other previous entered lines
                            break;
                        }

                    }
                }
                totalGcodeLines = lineNumber;
                logger.info("File Loading Complete");
            } catch (FileNotFoundException ex) {
                logger.error("File Not Found.");
            } catch (IOException ex) {
                logger.error(ex);
            }
        });
    }


    /**
     * handleRunFile
     *
     * @param evt action event
     */
    @FXML
    private void handleRunFile(ActionEvent evt) {
        logger.info("handleRunFile");
        if (!isSendingFile.get()) {
            isSendingFile.set(true); //disables jogging while file is running
            taskActive = true; //Set the thread condition to start
            Task fileSend = fileSenderTask();
            Thread fsThread = new Thread(fileSend);
            fsThread.setName("FileSender");
            timeStartDt = new Date();
//            updateProgress(1);
            fsThread.start();
        }
    }


    /**
     * initialize
     *
     * @param url url
     * @param rb resource bundle
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Initializing GcodeTabController");

        cncMachinePane.setOnMouseMoved(mouseEvent -> {
            yPosition.setValue(cncMachinePane.getNormalizedYasString(mouseEvent.getY()));
            xPosition.setValue(cncMachinePane.getNormalizedXasString(mouseEvent.getX()));
        });

        /*
         * keyPress EventHandler
         * TODO: Jogging needs to be broken into a new class
         */
        cncMachinePane.setOnKeyPressed(keyEvent -> {
            //logger.info("Start jogging.");
            //If we are sending a file.. Do NOT jog right now
            if (!isSendingFile.get()) {
                // Initialize to no valid axis set
                String axis = " ";

                //If this event has already sent a jog in need to pass this over.
                if (!isKeyPressed) {
                    KeyCode keyCode = keyEvent.getCode();
                    if (keyCode.equals(KeyCode.SHIFT)) {
                        // This is going to toss out our initial SHIFT press for the z axis key combination.
                        return;
                    }

                    if (keyEvent.isShiftDown()) {
                        // Alt is down so we make this into a Z movement
                        feedRatePercentage = TRAVERSE_FEED_RATE;
                    } else {
                        feedRatePercentage = NUDGE_FEED_RATE;
                    }

                    //Y Axis Jogging Movement
                    if (keyCode.equals(KeyCode.UP) || keyCode.equals(KeyCode.DOWN)) {
                        //This is and Y Axis Jog action
                        axis = "Y"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.UP)) {
                            //Invert this value by multiplying by -1
                            jogDial = MACHINE.getJoggingIncrementByAxis(axis);
                        } else {
                            jogDial = -1 * MACHINE.getJoggingIncrementByAxis(axis);
                        }

                        //X Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.RIGHT) || keyCode.equals(KeyCode.LEFT)) {
                        //This is a X Axis Jog Action
                        axis = "X"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.LEFT)) {
                            //Invert this value by multiplying by -1
                            jogDial = -1 * MACHINE.getJoggingIncrementByAxis(axis);
                        } else {
                            jogDial = MACHINE.getJoggingIncrementByAxis(axis);
                        }

                        //Z Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.MINUS) || (keyCode.equals(KeyCode.EQUALS))) {
                        //This is and Y Axis Jog action
                        axis = "Z"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.MINUS)) {
                            //Invert this value by multiplying by -1
                            jogDial = -1 * MACHINE.getJoggingIncrementByAxis(axis);
                        } else {
                            jogDial = MACHINE.getJoggingIncrementByAxis(axis);
                        }
                    }

                    if (axis.equals("X") || axis.equals("Y") || axis.equals("Z")) {
                        // valid key pressed
                        commandManager.setIncrementalMovementMode();
                        DRIVER.write("{\"GC\":\"G1F" +
                                MACHINE.getAxisByName(axis).getFeedRateMaximum() *
                                        feedRatePercentage + axis + jogDial + "\"}\n");
//                        DRIVER.write("{\"GC\":\"G0" + axis + jogDial + "\"}\n");
                        isKeyPressed = true;
                    }
                }

            } //end if isSendingFile
            else {
                //We are sending a file... We need to post a messages
                setGcodeText("Jogging disabled... Sending File.");
            }
        });


        /*
         * keyRelease event handler
         */
        cncMachinePane.setOnKeyReleased(keyEvent -> {
            //logger.info("End jogging.");
            if (!isSendingFile().get()) {
                try {
                    setGcodeText("");
                    // We should find out of TinyG's distance mode is set to G90 before just firing this off.
                    if (isKeyPressed) {
                        commandManager.stopJogMovement();
                        if (MACHINE.getGcodeDistanceMode().equals(GcodeDistanceMode.INCREMENTAL)) {
                            //We are in incremental mode we now will enter ABSOLUTE mode
                            commandManager.setAbsoluteMovementMode();
                        }
                        isKeyPressed = false; //reset the press flag
                    }
                } catch (InterruptedException ex) {
                    logger.error(ex);
                }
            }
        });

        cncMachinePane.visibleProperty().bind(cncMachineVisible);

        // This shows the coords when the cncMachinePane is visible.
        coordLocationGridPane.visibleProperty().bind(cncMachinePane.visibleProperty());

        gcodeStatusMessageTxt.textProperty().bind(gcodeStatusMessageValue);
        gcodeStatusMessageTxt.visibleProperty().bind(gcodeStatusMessageVisible);

        yAxisLocationTxt.textProperty().bind(yPosition);
        xAxisLocationTxt.textProperty().bind(xPosition);

        timeElapsedTxt.textProperty().bind(timeElapsed);
        timeLeftTxt.textProperty().bind(timeLeft);

        xLcd.valueProperty().bind(MACHINE.getAxisByName("x").getMachinePositionSimple()
                .subtract(MACHINE.getAxisByName("x").getOffset())
                .divide(MACHINE.gcodeUnitDivision));
        yLcd.valueProperty().bind(MACHINE.getAxisByName("y").getMachinePositionSimple()
                .subtract(MACHINE.getAxisByName("y").getOffset())
                .divide(MACHINE.gcodeUnitDivision));
        zLcd.valueProperty().bind(MACHINE.getAxisByName("z").getMachinePositionSimple()
                .subtract(MACHINE.getAxisByName("z").getOffset())
                .divide(MACHINE.gcodeUnitDivision));
        aLcd.valueProperty().bind(MACHINE.getAxisByName("a").getMachinePositionSimple()
                .subtract(MACHINE.getAxisByName("a").getOffset()));
        velLcd.valueProperty().bind(MACHINE.velocity);

        // TODO: make sure this is actually working at some point
        isSendingFile.bindBidirectional(WRITER.getIsSendingFile());
        Bindings.createStringBinding(() -> {
            SimpleStringProperty str = WRITER.getGcodeComment();
            Main.postConsoleMessage(str.getValue());
            return null;
        });

        // FIXME: java.lang.RuntimeException: HBox.disable : A bound value cannot be set.
        //gcodeTabHbox.disableProperty().bind(DRIVER.getConnectionStatus().not());

        // add support for zmove
//        assert zMoveScale != null : "fx:id=\"zMoveScale\" was not injected: check your FXML file 'Position.fxml'.";
//
//        // Set up ChoiceBox selection handler
//        zMoveScale.getSelectionModel().selectedIndexProperty()
//                .addListener((observableValue, number, result) -> {
//            switch ((int) result) {
//                case 0:
//                    zScale = 10.0f;
//                    break;
//                case 1:
//                    zScale = 1.0f;
//                    break;
//                case 2:
//                    zScale = 0.1f;
//                    break;
//            }
//        });

        timeStartDt = new Date();

        // We default to NOT display the CNC MACHINE pane.
        // Once the serial port is connected we will show this.
        setCNCMachineVisible(false);

        // This adds our CNC Machine (2d preview) to our display window
        // drawTable();

        /*
         * CHANGE LISTENERS
         */

        /* xLcd listener */
        xLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = MACHINE.getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });

        /* yLcd listener */
        yLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = MACHINE.getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });

        /* gcodeUnitMode listener */
        MACHINE.getGcodeUnitMode().addListener((ov, oldValue, newValue) -> {
            String gcodeUnitMode = MACHINE.getGcodeUnitMode().get();
            Main.postConsoleMessage("Gcode Unit Mode Changed to: " + gcodeUnitMode + "\n");

            try {
                DRIVER.getSerialWriter().setThrottled(true);
                DRIVER.priorityWrite(CMD_QUERY_MOTOR_1_SETTINGS);
                DRIVER.priorityWrite(CMD_QUERY_MOTOR_2_SETTINGS);
                DRIVER.priorityWrite(CMD_QUERY_MOTOR_3_SETTINGS);
                DRIVER.priorityWrite(CMD_QUERY_MOTOR_4_SETTINGS);

                DRIVER.priorityWrite(CMD_QUERY_AXIS_X);
                DRIVER.priorityWrite(CMD_QUERY_AXIS_Y);
                DRIVER.priorityWrite(CMD_QUERY_AXIS_Z);
                DRIVER.priorityWrite(CMD_QUERY_AXIS_A);
                DRIVER.priorityWrite(CMD_QUERY_AXIS_B);
                DRIVER.priorityWrite(CMD_QUERY_AXIS_C);
                Thread.sleep(400);
                DRIVER.getSerialWriter().setThrottled(false);
            } catch (InterruptedException ex) {
                logger.error("Error querying tg model state on gcode unit change.  " +
                        "Main.java binding section.");
            }
        });

        cncMachinePane.heightProperty().addListener((o, oldVal, newVal) ->
                logger.info("cncHeightChanged: " + cncMachinePane.getHeight()));
        cncMachinePane.maxWidthProperty().addListener((ov, oldValue, newValue) -> handleMaxWidthChange());
        cncMachinePane.maxHeightProperty().addListener((ov, oldValue, newValue) -> handleMaxHeightChange());

        /*
         * GCODE FILE CODE
         */
        data = FXCollections.observableArrayList();

        gcodeCol.setCellValueFactory(new PropertyValueFactory<>("codeLine"));

        gcodeView.getItems().clear();
        data.add(new GcodeLine("Click open to load..", 0));
        gcodeView.setItems(data);

        gcodeView.addEventHandler(MouseEvent.MOUSE_CLICKED, me -> {
            if (me.getButton().equals(MouseButton.PRIMARY)) {
                if (me.getClickCount() == 2) {
                    GcodeLine gcl = gcodeView.getSelectionModel().getSelectedItem();
                    if (DRIVER.isConnected().get()) {
                        logger.info("Double Clicked gcodeView " + gcl.getCodeLine());
                        DRIVER.write(gcl.getGcodeLineJsonified());
                        Main.postConsoleMessage(gcl.getGcodeLineJsonified());
                    } else {
                        logger.info("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
                        Main.postConsoleMessage("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
                    }

                }
            }
        });

        if(TgFXConstants.DISABLE_UI_CONNECTION_CHECK) {
            setCNCMachineVisible(true);
        }

    }


    /**
     * getLcdByAxisName
     *
     * @param axis axis name
     * @return Lcd Gauge
     */
    private Gauge getLcdByAxisName(String axis) {
        logger.info("getLcdByAxisName");
        switch (axis) {
            case "x":
                return xLcd;
            case "y":
                return yLcd;
            case "z":
                return zLcd;
            case "a":
                return aLcd;
            case "vel":
                return velLcd;
            default:
                return null;
        }
    }


    /**
     * fileSenderTask
     *
     * @return task
     */
    private Task fileSenderTask() {
        logger.info("fileSenderTask");
        return new Task() {
            @Override
            protected Object call()  {
                logger.info("call");
                StringBuilder line = new StringBuilder();
                for (GcodeLine gcodeLine : data) {
                    if (!isTaskActive()) {
                        //Cancel Button was pushed
                        Main.postConsoleMessage("File Sending Task Killed....\n");
                        break;
                    } else {
                        if (gcodeLine.getCodeLine().equals("")) {
                            //Blank Line.. Passing..
                            continue;
                        }
                        if (gcodeLine.getCodeLine().toLowerCase().contains("(")) {
                            DRIVER.write("**COMMENT**" + gcodeLine.getCodeLine());
                            Main.postConsoleMessage("GCODE COMMENT:" + gcodeLine.getCodeLine());
                            continue;
                        }

                        line.setLength(0);
                        line.append("{\"gc\":\"").append(gcodeLine.getCodeLine()).append("\"}\n");
                        DRIVER.write(line.toString());
                    }
                }

                DRIVER.write("**FILEDONE**");
                return true;
            }
        };
    }

    public CNCMachine getCncMachinePane(){
        return cncMachinePane;
    }


    /**
     * isTaskActive
     *
     * @return task status
     */
    private synchronized boolean isTaskActive() {
        // logger.info("isTaskActive: {}", taskActive);
        return taskActive;
    }


    /**
     * setTaskActive
     *
     * @param boolTask task status
     */
    public synchronized void setTaskActive(boolean boolTask) {
        // logger.info("setTaskActive: {}", boolTask);
        taskActive = boolTask;
    }


    /**
     * isSendingFile
     * model for enabling/disabling jogging
     *
     * @return isSendingFile property
     */
    public SimpleBooleanProperty isSendingFile(){
        // logger.info("isSendingFile: {}", isSendingFile);
        return isSendingFile;
    }


    /**
     * setGcodeText
     * updates gcodeText
     *
     * @param text message
     */
    public void setGcodeText(String text) {
        logger.info("setGcodeText: {}", text);
        gcodeStatusMessageValue.setValue(text);
        gcodeStatusMessageVisible.setValue(true);
    }

    /**
     * hideGcodeText
     * hides gcode status message
     *
     */
    public void hideGcodeText() {
        logger.info("hideGcodeText");
        gcodeStatusMessageVisible.setValue(false);
    }

    /**
     * drawCanvasUpdate
     * model to update canvas
     *
     */
    public void drawCanvasUpdate() {
        logger.info("drawCanvasUpdate");
        if (TgfxSettingsController.isDrawPreview()) {
            cncMachinePane.drawLine(MACHINE.getMotionMode().get(), MACHINE.getVelocity());
        }
    }


    /**
     * drawTable
     *
     */
    private void drawTable() {
        logger.info("drawTable");
        //TODO  We need to make this a message to subscribe to.
        if (!gcodePane.getChildren().contains(cncMachinePane)) {
            // Add the cnc MACHINE to the gcode pane
            gcodePane.getChildren().add(cncMachinePane);
        }
    }


    /**
     * normalizeGcodeLine
     *
     * @param gcl gcode line string
     * @return boolean, false if bad bytes
     */
    private boolean normalizeGcodeLine(String gcl) {
        logger.trace("normalizeGcodeLine {}",gcl);
        // These are considered bad bytes in gcode files.
        // These will trigger tinyg to throw interrupts
        // 0x21 = !
        // 0x18 = Ctrl-X
        // 0x7e = ~
        // 0x25 = %
        for (int i = 0; i < BAD_BYTES.length; i++) {
            for (int j = 0; j < gcl.length(); j++) {
                if (gcl.charAt(j) == BAD_BYTES[i]) {
                    //Bad Byte Found
                    logger.error("Bad Byte Char Detected: " + BAD_BYTES[i]);
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * handleMaxHeightChange
     *
     * FIXME: possible NPEs
     */
    private void handleMaxHeightChange() {
        logger.info("handleMaxHeightChange");

        // FIXME: makes the CNCMachine stage too large
//        scaleAmount = getScale();
//        cncMachinePane.autoScaleWorkTravelSpace(scaleAmount);

        // this was already commented out
//        widthSize.textProperty().bind( Bindings.format("%s",
//        cncMachinePane.widthProperty().divide(MACHINE.gcodeUnitDivision)
//                .asString().concat(MACHINE.getGcodeUnitMode())    ));
//
//        heightSize.setText(decimalFormat.format(MACHINE.getAxisByName("y").
//                getTravelMaximum()) + " " + MACHINE.getGcodeUnitMode().getValue());
    }


    /**
     * handleMaxWidthChange
     *
     */
    private void handleMaxWidthChange() {
        logger.info("handleMaxWidthChange");

        // FIXME: makes the CNCMachine stage too large
//        scaleAmount = getScale();
//        cncMachinePane.autoScaleWorkTravelSpace(scaleAmount);

        // this was already commented out
//        widthSize.setText(decimalFormat.format(DRIVER
//        .m.getAxisByName("x").getTravelMaximum()) + " " + DRIVER
//        .m.getGcodeUnitMode().getValue());
    }



    private double getScale(){
        logger.info("handleMaxHeightChange");
        double x = MACHINE.getAxisByName("x").getTravelMaxSimple().get();
        double y = MACHINE.getAxisByName("y").getTravelMaxSimple().get();
        double scale;

        if (gcodePane.getWidth() - x < gcodePane.getHeight() - y) {
            //X is longer use this code
            if (MACHINE.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scale = (gcodePane.heightProperty().get() / y * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scale = (gcodePane.heightProperty().get() / y) * .80;  //%80 of the scale;
            }
        } else {
            //Y is longer use this code
            if (MACHINE.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scale = (gcodePane.heightProperty().get() / y * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scale = (gcodePane.heightProperty().get() / y) * .80;  //%80 of the scale;
            }
        }
        return scale;
    }


    /**
     * setCNCMachineVisible
     *
     * @param visible update cnc MACHINE visibility
     */
    public void setCNCMachineVisible(boolean visible) {
        cncMachineVisible.setValue(visible);
    }


    /**
     * setIsFileSending
     *
     * @param isFileSending set is sending
     */
    public void setIsFileSending(boolean isFileSending) {
        logger.info("setIsFileSending: {}", isFileSending);
        isSendingFile.set(isFileSending);
    }


    /**
     * updateProgress
     * Scroll Gcode table view to specified line, show elapsed and remaining time
     *
     * @param lineNum line number
     */
    public void updateProgress(int lineNum) {
        logger.info("updateProgress: {}", lineNum);
        if (isSendingFile.get() && lineNum > 0) {
//            gcodeView.scrollTo(lineNum);

            // Show elapsed and remaining time
            Date currentTimeDt = new Date();  // Get current time
            long elapsed = currentTimeDt.getTime() - timeStartDt.getTime();

            float rate = elapsed / (float) lineNum;
            long remain = (long) ((totalGcodeLines - lineNum) * rate);  // remaining lines * secs per line

            timeElapsed.setValue(String.format("%02d:%02d", elapsed / 60000, (elapsed / 1000) % 60));
            timeLeft.setValue(String.format("%02d:%02d", remain / 60000, (remain / 1000) % 60));
        }
    }
}
