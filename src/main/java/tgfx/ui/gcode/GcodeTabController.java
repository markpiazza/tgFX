package tgfx.ui.gcode;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import eu.hansolo.medusa.Gauge;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.Main;
import tgfx.render.CNCMachine;
import tgfx.render.Draw2d;
import tgfx.system.Machine;
import tgfx.system.enums.GcodeDistanceMode;
import tgfx.tinyg.CommandManager;
import tgfx.tinyg.TinygDriver;
import tgfx.ui.tgfxsettings.TgfxSettingsController;

/**
 * GcodeTabController
 *
 * FXML Controller class
 *
 */
public class GcodeTabController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    private static TinygDriver driver = TinygDriver.getInstance();
    private static Machine machine = driver.getMachine();

    private static Date timeStartDt;
    private static byte[] BAD_BYTES = {(byte) 0x21, (byte) 0x18, (byte) 0x7e};
    private static int totalGcodeLines = 0;
    private static int test = 1;

    private String buildDate;
    private String axis = "";
    private double scaleAmount;
    private double jogDial = 0;
    private double FEED_RATE_PERCENTAGE = .05;  //%5
    private double TRAVERSE_FEED_RATE = 1;  //%100
    private double NUDGE_FEED_RATE = .05;  //%5
    private float zScale = 0.1f;
    private int buildNumber;
    private boolean taskActive = false;
    private boolean isKeyPressed = false;

    /*
     * Data models for FXML elements
     */
    private static SimpleBooleanProperty isSendingFile = new SimpleBooleanProperty(false);
    private static SimpleBooleanProperty cncMachineVisible = new SimpleBooleanProperty(true);
    private static SimpleStringProperty gcodeStatusMessageValue = new SimpleStringProperty("");
    private static SimpleBooleanProperty gcodeStatusMessageVisible = new SimpleBooleanProperty(false);

    //List to store the gcode file
    public ObservableList<GcodeLine> data;


    /*  ######################## FXML ELEMENTS ############################*/
    @FXML
    private Text timeElapsedTxt,
            timeLeftTxt,
            xAxisLocationTxt,
            yAxisLocationTxt,
            gcodeStatusMessageTxt;
    @FXML
    private Gauge xLcd,
            yLcd,
            zLcd,
            aLcd,
            velLcd;
    @FXML
    private StackPane machineWorkspacePane,
            gcodePane;
    @FXML
    private GridPane coordLocationGridPane;
    @FXML
    private Pane previewPane;
    @FXML
    private TableColumn<GcodeLine, String> gcodeCol;
    @FXML
    private TableView gcodeView;
    @FXML
    private Button runBtn,
            connectBtn,
            gcodeZeroBtn,
            btnClearScreenBtn,
            pauseResumeBtn,
            btnTestBtn,
            btnHandleInhibitAllAxisBtn;
    @FXML
    private ChoiceBox<?> zMoveScaleChoiceBox; // commented out
    @FXML
    private HBox gcodeTabControllerHBox; // commented out
    @FXML
    private CNCMachine cncMachinePane = new CNCMachine();


    /**
     * Initializes the controller class.
     */
    @SuppressWarnings("ConstantConditions")
    public GcodeTabController() {
        logger.info("Gcode Controller Loaded");
        cncMachinePane.setOnMouseMoved(me -> {
            yAxisLocationTxt.setText(cncMachinePane.getNormalizedYasString(me.getY()));
            xAxisLocationTxt.setText(cncMachinePane.getNormalizedXasString(me.getX()));
        });

        /*
         * keyPress EventHandler
         * TODO: Jogging needs to be broken into a new class
         */
        EventHandler<KeyEvent> keyPress = keyEvent -> {
            //If we are sending a file.. Do NOT jog right now
            if (!isSendingFile.get()) {
                // Initialize to no valid axis set
                axis = " ";

                //If this event has already sent a jog in need to pass this over.
                if (!isKeyPressed) {
                    KeyCode keyCode = keyEvent.getCode();
                    if (keyCode.equals(KeyCode.SHIFT)) {
                        // This is going to toss out our initial SHIFT press for the z axis key combination.
                        return;
                    }

                    if (keyEvent.isShiftDown()) {
                        // Alt is down so we make this into a Z movement
                        FEED_RATE_PERCENTAGE = TRAVERSE_FEED_RATE;
                    } else {
                        FEED_RATE_PERCENTAGE = NUDGE_FEED_RATE;
                    }

                    //Y Axis Jogging Movement
                    if (keyCode.equals(KeyCode.UP) || keyCode.equals(KeyCode.DOWN)) {
                        //This is and Y Axis Jog action
                        axis = "Y"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.UP)) {
                            jogDial = machine.getJoggingIncrementByAxis(axis);
                        } else if (keyEvent.getCode().equals(KeyCode.DOWN)) {
                            //Invert this value by multiplying by -1
                            jogDial = -1 * machine.getJoggingIncrementByAxis(axis);
                        }

                    //X Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.RIGHT) || keyCode.equals(KeyCode.LEFT)) {
                        //This is a X Axis Jog Action
                        axis = "X"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.LEFT)) {
                            jogDial = -1 * machine.getJoggingIncrementByAxis(axis);
                        } else if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
                            //Invert this value by multiplying by -1
                            jogDial = machine.getJoggingIncrementByAxis(axis);
                        }

                    //Z Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.MINUS) || (keyCode.equals(KeyCode.EQUALS))) {
                        //This is and Y Axis Jog action
                        axis = "Z"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.MINUS)) {
                            jogDial = -1 * machine.getJoggingIncrementByAxis(axis);
                        } else if (keyEvent.getCode().equals(KeyCode.EQUALS)) {
                            //Invert this value by multiplying by -1
                            jogDial = machine.getJoggingIncrementByAxis(axis);
                        }
                    }

                    if (axis.equals("X") || axis.equals("Y") || axis.equals("Z")) {
                        // valid key pressed
                        CommandManager.setIncrementalMovementMode();
                        driver.write("{\"GC\":\"G1F" +
                                machine.getAxisByName(axis).getFeedRateMaximum() *
                                        FEED_RATE_PERCENTAGE + axis + jogDial + "\"}\n");
//                        driver.write("{\"GC\":\"G0" + axis + jogDial + "\"}\n");
                        isKeyPressed = true;
                    }
                }

            } //end if isSendingFile
            else {
                //We are sending a file... We need to post a messages
                setGcodeTextTemp("Jogging Disabled... Sending File.");
            }
        };


        /*
         * keyRelease event handler
         */
        EventHandler<KeyEvent> keyRelease = keyEvent -> {
            if (!isSendingFile().get()) {
                try {
                    setGcodeText("");
                    // We should find out of TinyG's distance mode is set to G90 before just firing this off.
                    if (isKeyPressed) {
                        CommandManager.stopJogMovement();
                        if (machine.getGcodeDistanceMode().equals(GcodeDistanceMode.INCREMENTAL)) {
                            //We are in incremental mode we now will enter ABSOLUTE mode
                            CommandManager.setAbsoluteMovementMode();
                        }
                        isKeyPressed = false; //reset the press flag
                    }
                } catch (InterruptedException ex) {
                   logger.error(ex);
                }
            }
        };

        cncMachinePane.setOnKeyPressed(keyPress);
        cncMachinePane.setOnKeyReleased(keyRelease);
    }


    /**
     * handleHomeXYZ
     *
     * @param evt action event
     */
    @FXML
    private void handleHomeXYZ(ActionEvent evt) {
        logger.info("handleHomeXYZ");
        if (driver.isConnected().get()) {
            driver.write(CommandManager.CMD_APPLY_SYSTEM_HOME_XYZ_AXES);
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

        if (driver.isConnected().get()) {
            switch (axis) {
                case "x":
                    driver.write(CommandManager.CMD_APPLY_HOME_X_AXIS);
                    break;
                case "y":
                    driver.write(CommandManager.CMD_APPLY_HOME_Y_AXIS);
                    break;
                case "z":
                    driver.write(CommandManager.CMD_APPLY_HOME_Z_AXIS);
                    break;
                case "a":
                    driver.write(CommandManager.CMD_APPLY_HOME_A_AXIS);
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

        if (driver.isConnected().get()) {
            //We set this so we do not draw lines for the previous position to the new zero.
            Draw2d.setFirstDraw(true);
            switch (axis) {
                case "x":
                    driver.write(CommandManager.CMD_APPLY_ZERO_X_AXIS);
                    break;
                case "y":
                    driver.write(CommandManager.CMD_APPLY_ZERO_Y_AXIS);
                    break;
                case "z":
                    driver.write(CommandManager.CMD_APPLY_ZERO_Z_AXIS);
                    break;
                case "a":
                    driver.write(CommandManager.CMD_APPLY_ZERO_A_AXIS);
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
     * initialize
     *
     * @param url url
     * @param rb resource bundle
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("initialize");

        cncMachinePane.visibleProperty().bind(cncMachineVisible);
        //This shows the coords when the cncMachinePane is visible.
        coordLocationGridPane.visibleProperty().bind(cncMachinePane.visibleProperty());
        gcodeStatusMessageTxt.textProperty().bind(gcodeStatusMessageValue);
        gcodeStatusMessageTxt.visibleProperty().bind(gcodeStatusMessageVisible);

        xLcd.valueProperty().bind(machine.getAxisByName("x").getMachinePositionSimple()
                .subtract(machine.getAxisByName("x").getOffset()).divide(machine.gcodeUnitDivision));
        yLcd.valueProperty().bind(machine.getAxisByName("y").getMachinePositionSimple()
                .subtract(machine.getAxisByName("y").getOffset()).divide(machine.gcodeUnitDivision));
        zLcd.valueProperty().bind(machine.getAxisByName("z").getMachinePositionSimple()
                .subtract(machine.getAxisByName("z").getOffset()).divide(machine.gcodeUnitDivision));
        aLcd.valueProperty().bind(machine.getAxisByName("a").getMachinePositionSimple()
                .subtract(machine.getAxisByName("a").getOffset()));
        velLcd.valueProperty().bind(machine.velocity);

        // FIXME: java.lang.RuntimeException: HBox.disable : A bound value cannot be set.
        // gcodeTabControllerHBox.disableProperty().bind(driver.getConnectionStatus().not());

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

        // We default to NOT display the CNC machine pane.
        // Once the serial port is connected we will show this.
        setCNCMachineVisible(false);

        // This adds our CNC Machine (2d preview) to our display window
        if (!gcodePane.getChildren().contains(cncMachinePane)) {
            // Add the cnc machine to the gcode pane
            gcodePane.getChildren().add(cncMachinePane);
        }

        /*
         * CHANGE LISTENERS
         */

        /* xLcd listener */
        xLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = machine.getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });

        /* yLcd listener */
        yLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = machine.getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });

        /* gcodeUnitMode listener */
        machine.getGcodeUnitMode().addListener((ov, oldValue, newValue) -> {
            String gcodeUnitMode = machine.getGcodeUnitMode().get();
            Main.postConsoleMessage("Gcode Unit Mode Changed to: " + gcodeUnitMode + "\n");

            try {
                driver.serialWriter.setThrottled(true);
                driver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_1_SETTINGS);
                driver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_2_SETTINGS);
                driver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_3_SETTINGS);
                driver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_4_SETTINGS);

                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_X);
                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_Y);
                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_Z);
                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_A);
                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_B);
                driver.priorityWrite(CommandManager.CMD_QUERY_AXIS_C);
                Thread.sleep(400);
                driver.serialWriter.setThrottled(false);
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
        for(GcodeLine line : data){
            System.err.println("----"+line);
        }

        gcodeCol.setCellValueFactory(new PropertyValueFactory<>("codeLine"));
        GcodeLine n = new GcodeLine("Click open to load..", 0);

//        gcodeView.getItems().setAll(data);
//        data.add(n);

//        gcodeView.setItems(data);

//        gcodeView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent me) {
//                if (me.getButton().equals(me.getButton().PRIMARY)) {
//                    if (me.getClickCount() == 2) {
//                        GcodeLine gcl = (GcodeLine) gcodeView.getSelectionModel().getSelectedItem();
//                        if (driver.isConnected().get()) {
//                            logger.info("Double Clicked gcodeView " + gcl.getCodeLine());
//                            driver.write(gcl.getGcodeLineJsonified());
//                            Main.postConsoleMessage(gcl.getGcodeLineJsonified());
//                        } else {
//                            logger.info("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
//                            Main.postConsoleMessage("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
//                        }
//
//                    }
//                }
//            }
//        });
    }


    /**
     * handleZeroSystem
     *
     * @param evt action event
     */
    @FXML
    private void handleZeroSystem(ActionEvent evt) {
        cncMachinePane.zeroSystem();
    }


    /**
     * handlePauseResumeAct
     *
     * @param evt action event
     */
    @FXML
    private void handlePauseResumeAct(ActionEvent evt) {
        if ("Pause".equals(pauseResumeBtn.getText())) {
            pauseResumeBtn.setText("Resume");
            driver.priorityWrite(CommandManager.CMD_APPLY_PAUSE);
        } else {
            pauseResumeBtn.setText("Pause");
            driver.priorityWrite(CommandManager.CMD_APPLY_RESUME);
        }
    }


    /**
     * handleClearScreen
     *
     * @param evt action event
     */
    @FXML
    private void handleClearScreen(ActionEvent evt) {
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
        Platform.runLater(() -> {
            try {
                driver.serialWriter.clearQueueBuffer();
                //This sends the 0x18 byte
                driver.priorityWrite(CommandManager.CMD_APPLY_RESET);

                //We disable everything while waiting for the board to reset
//                 topAnchorPane.setDisable(true);
//                 topTabPane.setDisable(true);

//                Thread.sleep(8000);
//                onConnectActions();
                Main.postConsoleMessage("Resetting TinyG....\n.");
                driver.serialWriter.notifyAck();
                driver.serialWriter.clearQueueBuffer();
                cncMachinePane.clearScreen();
                // We set this to false to allow us to jog again
                isSendingFile.set(false);
            } catch (SerialPortException ex) {
                logger.error("handleReset " + ex.getMessage());
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
        Platform.runLater(() -> {
            logger.info("Stopping Job Clearing Serial Queue...\n");
            CommandManager.stopTinyGMovement();
            isSendingFile.set(false); //We set this to false to allow us to jog again
        });
    }


    /**
     * handleTestButton
     *
     * @param evt action event
     */
    @FXML
    static void handleTestButton(ActionEvent evt) {
        logger.info("Test Button....");
        updateProgress(test);
        test += 5;

        //Main.postConsoleMessage("Test!");
        //timeElapsedTxt.setText("hello");

//        Iterator ii = null;
//        Line l;
//        cncMachinePane.getChildren().iterator();
//        while(ii.hasNext()){
//            l = (Line) ii.next();
//            
//        }
    }


    /**
     * handleOpenFile
     *
     * @param event action event
     */
    @FXML
    private void handleOpenFile(ActionEvent event) {
        Platform.runLater(() -> {
            try {
                Main.postConsoleMessage("Loading a gcode file.....\n");
                FileChooser fc = new FileChooser();
                fc.setTitle("Open GCode File");

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
                int _linenumber = 0;
                while ((strLine = br.readLine()) != null) {
                    if (!strLine.equals("")) {
                        //Do not add empty lines to the list
                        //gcodesList.appendText(strLine + "\n");
                        if (!strLine.toUpperCase().startsWith("N")) {
                            strLine = "N" + String.valueOf(_linenumber) + " " + strLine;
                        }
                        if (normalizeGcodeLine(strLine)) {
                            data.add(new GcodeLine(strLine, _linenumber));
                            _linenumber++;
                        } else {
                            Main.postConsoleMessage("ERROR: Your gcode file contains an invalid character.. " +
                                    "Either !,% or ~. Remove this character and try again.");
                            Main.postConsoleMessage("  Line " + _linenumber);
                            data.clear(); //Remove all other previous entered lines
                            break;
                        }

                    }
                }
                totalGcodeLines = _linenumber;
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
     * getLcdByAxisName
     *
     * @param axis axis name
     * @return Lcd Gauge
     */
    private Gauge getLcdByAxisName(String axis) {
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
        return new Task() {
            @Override
            protected Object call()  {
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
                            driver.write("**COMMENT**" + gcodeLine.getCodeLine());
    //                            Main.postConsoleMessage("GCODE COMMENT:" + gcodeLine.getCodeLine());
                            continue;
                        }

                        line.setLength(0);
                        line.append("{\"gc\":\"").append(gcodeLine.getCodeLine()).append("\"}\n");
                        driver.write(line.toString());
                    }
                }

                driver.write("**FILEDONE**");
                return true;
            }
        };
    }


    /**
     * isTaskActive
     *
     * @return task status
     */
    private synchronized boolean isTaskActive() {
        return taskActive;
    }


    /**
     * setTaskActive
     *
     * @param boolTask task status
     */
    public synchronized void setTaskActive(boolean boolTask) {
        taskActive = boolTask;
    }


    /**
     * isSendingFile
     * model for enabling/disabling jogging
     *
     * @return isSendingFile property
     */
    public static SimpleBooleanProperty isSendingFile(){
        return isSendingFile;
    }


    /**
     * setGcodeTextTemp
     * updates the gcodeTextTemp model
     *
     * @param text message
     */
    public static void setGcodeTextTemp(String text) {
        logger.info("setGcodeTextTemp: {}", text);
        gcodeStatusMessageValue.setValue(text);
    }


    /**
     * setGcodeText
     * updates gcodeText
     *
     * @param text message
     */
    public static void setGcodeText(String text) {
        logger.info("setGcodeText: {}",text);
        gcodeStatusMessageVisible.setValue(true);
    }

    /**
     * hideGcodeText
     * hides gcode status message
     *
     */
    public static void hideGcodeText() {
        logger.info("hideGcodeText");
        gcodeStatusMessageVisible.setValue(false);
    }

    /**
     * drawCanvasUpdate
     * model to update canvas
     *
     */
    public static void drawCanvasUpdate() {
        logger.info("drawCanvasUpdate");
        if (TgfxSettingsController.isDrawPreview()) {
//            cncMachinePane.drawLine(machine.getMotionMode().get(), machine.getVelocity());
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
            // Add the cnc machine to the gcode pane
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
        if (gcodePane.getWidth() - machine.getAxisByName("x")
                .getTravelMaxSimple().get() < gcodePane.getHeight() -
                machine.getAxisByName("y").getTravelMaxSimple().get()) {
            //X is longer use this code
            if (machine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = (gcodePane.heightProperty().get() /
                        machine.getAxisByName("y")
                                .getTravelMaxSimple().get() * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = (gcodePane.heightProperty().get() /
                        machine.getAxisByName("y")
                                .getTravelMaxSimple().get()) * .80;  //%80 of the scale;
            }
        } else {
            //Y is longer use this code
            if (machine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = (gcodePane.heightProperty().get() /
                        machine.getAxisByName("y")
                                .getTravelMaxSimple().get() * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = (gcodePane.heightProperty().get() /
                        machine.getAxisByName("y")
                                .getTravelMaxSimple().get()) * .80;  //%80 of the scale;
            }
        }
        cncMachinePane.autoScaleWorkTravelSpace(scaleAmount);
//        widthSize.textProperty().bind( Bindings.format("%s",
//        cncMachinePane.widthProperty().divide(driver
//        .m.gcodeUnitDivision).asString().concat(driver
//        .m.getGcodeUnitMode())    ));  //.asString().concat(driver.m.getGcodeUnitMode().get()));

//        heightSize.setText(decimalFormat.format(driver
//        .m.getAxisByName("y").getTravelMaximum()) + " " + driver
//        .m.getGcodeUnitMode().getValue());
    }

    /**
     * handleMaxWidthChange
     *
     */
    private void handleMaxWidthChange() {
        //This is for the change listener to call for Max Width Change on the CNC Machine
        if (gcodePane.getWidth() - machine.getAxisByName("x")
                .getTravelMaxSimple().get() < gcodePane.getHeight() -
                machine.getAxisByName("y").getTravelMaxSimple().get()) {
            //X is longer use this code
            if (machine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = (gcodePane.heightProperty().get() /
                                machine.getAxisByName("y")
                                    .getTravelMaxSimple().get() * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = (gcodePane.heightProperty().get() /
                                machine.getAxisByName("y")
                                    .getTravelMaxSimple().get()) * .80;  //%80 of the scale;
            }
        } else {
            //Y is longer use this code
            if (machine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = (gcodePane.heightProperty().get() /
                                machine.getAxisByName("y")
                                    .getTravelMaxSimple().get() * 25.4) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = (gcodePane.heightProperty().get() /
                                machine.getAxisByName("y")
                                    .getTravelMaxSimple().get()) * .80;  //%80 of the scale;
            }
        }
        cncMachinePane.autoScaleWorkTravelSpace(scaleAmount);
//        widthSize.setText(decimalFormat.format(driver
//        .m.getAxisByName("x").getTravelMaximum()) + " " + driver
//        .m.getGcodeUnitMode().getValue());
    }


    /**
     * setCNCMachineVisible
     *
     * @param t update cnc machine visibility
     */
    public static void setCNCMachineVisible(boolean t) {
        cncMachineVisible.setValue(t);
    }


    /**
     * setIsFileSending
     *
     * @param flag set is sending
     */
    public static void setIsFileSending(boolean flag) {
        isSendingFile.set(flag);
    }


    /**
     * updateProgress
     * Scroll Gcode table view to specified line, show elapsed and remaining time
     *
     * @param lineNum line number
     */
    public static void updateProgress(int lineNum) {
        if (isSendingFile.get() && lineNum > 0) {
//            gcodeView.scrollTo(lineNum);

            // Show elapsed and remaining time
            Date currentTimeDt = new Date();  // Get current time
            long elapsed = currentTimeDt.getTime() - timeStartDt.getTime();
            // FIXME: integer division
            float rate = elapsed / lineNum;
            long remain = (long) ((totalGcodeLines - lineNum) * rate);  // remaining lines * secs per line

            //timeElapsedTxt.setText(String.format("%02d:%02d", elapsed / 60000, (elapsed / 1000) % 60));
            //timeLeftTxt.setText(String.format("%02d:%02d", remain / 60000, (remain / 1000) % 60));
        }
    }
}
