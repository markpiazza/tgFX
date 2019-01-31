package tgfx.ui.gcode;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import eu.hansolo.medusa.Gauge;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
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

    private static CNCMachine cncMachine = new CNCMachine();
    private static Date timeStartDt;
    private static byte[] BAD_BYTES = {(byte) 0x21, (byte) 0x18, (byte) 0x7e};
    private static int totalGcodeLines = 0;
    private static int test = 1;
    private int buildNumber;
    private String buildDate;
    private String _axis = "";
    private double scaleAmount;
    private double jogDial = 0;
    private double FEED_RATE_PERCENTAGE = .05;  //%5
    private double TRAVERSE_FEED_RATE = 1;  //%100
    private double NUDGE_FEED_RATE = .05;  //%5
    private boolean taskActive = false;
    private boolean isKeyPressed = false;

    // Holds CNCMachine  This needs to be before CNCMachine()
    public static StackPane gcodePane = new StackPane();

    // This tracks to see if we are sending a file to tinyg.
    // This allows us to NOT try to jog while sending files
    public static SimpleBooleanProperty isSendingFile = new SimpleBooleanProperty(false);

    public ObservableList<GcodeLine> data; //List to store the gcode file


    /*  ######################## FXML ELEMENTS ############################*/
    @FXML
    private static Text timeElapsedTxt;
    @FXML
    private static Text timeLeftTxt;
    @FXML
    private Gauge xLcd, yLcd, zLcd, aLcd, velLcd; //DRO Lcds
//    @FXML
//    StackPane machineWorkspace;
//    @FXML
//    private Pane previewPane;
    @FXML
    private TableColumn<GcodeLine, String> gcodeCol;
    @FXML
    private static TableView gcodeView;
    @FXML
    private Text xAxisLocation, yAxisLocation;
    @FXML
    private static Text gcodeStatusMessage;  //Cursor location on the cncMachine Canvas
    @FXML
    private Button pauseResume;
//    private Button Run, Connect, gcodeZero, btnClearScreen, pauseResume, btnTest, btnHandleInhibitAllAxis;
    @FXML
    private GridPane coordLocationGridPane;
//    @FXML // fx:id="zMoveScale"
//    private ChoiceBox<?> zMoveScale; // Value injected by FXMLLoader
//    private float zScale = 0.1f;
    @FXML
    private HBox gcodeTabControllerHBox;

    /**
     * Initializes the controller class.
     */
    @SuppressWarnings("ConstantConditions")
    public GcodeTabController() {
        logger.info("Gcode Controller Loaded");
        cncMachine.setOnMouseMoved(me -> {
            yAxisLocation.setText(cncMachine.getNormalizedYasString(me.getY()));
            xAxisLocation.setText(cncMachine.getNormalizedXasString(me.getX()));
        });

        //TODO: JOGGING NEEDS TO BE BROKEN INTO A NEW CLASS

        EventHandler<KeyEvent> keyPress = keyEvent -> {
            if (!isSendingFile.get()) {  //If we are sending a file.. Do NOT jog right now
//                Main.postConsoleMessage("KEY PRESSED: " + keyEvent.getCode().toString());

                // Do the jogging.
                _axis = " "; // Initialize to no valid axis set

                if (!isKeyPressed) { //If this event has already sent a jog in need to pass this over.
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
                        _axis = "Y"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.UP)) {
                            jogDial = TinygDriver.getInstance().getMachine()
                                    .getJoggingIncrementByAxis(_axis);
                        } else if (keyEvent.getCode().equals(KeyCode.DOWN)) {
                            //Invert this value by multiplying by -1
                            jogDial = -1 * TinygDriver.getInstance().getMachine()
                                    .getJoggingIncrementByAxis(_axis);
                        }

                    //X Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.RIGHT) || keyCode.equals(KeyCode.LEFT)) {
                        //This is a X Axis Jog Action
                        _axis = "X"; //Set the axis for this jog movement
                        if (keyEvent.getCode().equals(KeyCode.LEFT)) {
                            jogDial = -1 * TinygDriver.getInstance().getMachine().
                                    getJoggingIncrementByAxis(_axis);
                        } else if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
                            //Invert this value by multiplying by -1
                            jogDial = TinygDriver.getInstance().getMachine()
                                    .getJoggingIncrementByAxis(_axis);
                        }

                    //Z Axis Jogging Movement
                    } else if (keyCode.equals(KeyCode.MINUS) || (keyCode.equals(KeyCode.EQUALS))) {
                        _axis = "Z";
                        if (keyEvent.getCode().equals(KeyCode.MINUS)) {
                            jogDial = -1 * TinygDriver.getInstance().getMachine()
                                    .getJoggingIncrementByAxis(_axis);
                        } else if (keyEvent.getCode().equals(KeyCode.EQUALS)) {
                            //Invert this value by multiplying by -1
                            jogDial = TinygDriver.getInstance().getMachine()
                                    .getJoggingIncrementByAxis(_axis);
                        }
                    }

                    if (_axis.equals("X") || _axis.equals("Y") || _axis.equals("Z")) {
                        // valid key pressed
                        CommandManager.setIncrementalMovementMode();
                        TinygDriver.getInstance().write("{\"GC\":\"G1F" +
                                TinygDriver.getInstance().getMachine().
                                        getAxisByName(_axis).getFeedRateMaximum() *
                                        FEED_RATE_PERCENTAGE + _axis + jogDial + "\"}\n");
//                        TinygDriver.getInstance().write("{\"GC\":\"G0" + _axis + jogDial + "\"}\n");
                        isKeyPressed = true;
                    }
                }

            } //end if isSendingFile
            else {
                //We are sending a file... We need to post a messages
                setGcodeTextTemp("Jogging Disabled... Sending File.");
            }
        };

        EventHandler<KeyEvent> keyRelease = keyEvent -> {
//                Main.postConsoleMessage("Stopping Jog Action: " + keyEvent.getCode().toString());
            if (!isSendingFile.get()) {
                try {
                    setGcodeText("");
                    if (isKeyPressed) {  //We should find out of TinyG's distance mode is set to G90 before just firing this off.
                      CommandManager.stopJogMovement();
                        if (TinygDriver.getInstance().getMachine()
                                .getGcodeDistanceMode()
                                .equals(GcodeDistanceMode.INCREMENTAL)) {
                            //We are in incremental mode we now will enter ABSOLUTE mode
                            CommandManager.setAbsoluteMovementMode();
                        } //re-enable absolute mode
                        isKeyPressed = false; //reset the press flag
                    }
                } catch (InterruptedException ex) {
                   logger.error(ex);
                }
            }
        };

        cncMachine.setOnKeyPressed(keyPress);
        cncMachine.setOnKeyReleased(keyRelease);
    }

    public static void setGcodeTextTemp(String _text) {
        gcodeStatusMessage.setText(_text);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(3000), gcodeStatusMessage);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.play();
//        gcodeStatusMessage.setText(""); //clear it out
    }

    public static void setGcodeText(String _text) {
        gcodeStatusMessage.setText(_text);
        gcodeStatusMessage.setVisible(true);
//        FadeTransition fadeTransition  = new FadeTransition(Duration.millis(1000), gcodeStatusMessage);
//                fadeTransition.setFromValue(0.0);
//                fadeTransition.setToValue(1.0);
//                fadeTransition.play();
    }

    public static void hideGcodeText() {
//        gcodeStatusMessage.setVisible(false);
//        FadeTransition fadeTransition  = new FadeTransition(Duration.millis(500), gcodeStatusMessage);
//                fadeTransition.setFromValue(1.0);
//                fadeTransition.setToValue(0.0);
//                fadeTransition.play();
    }

    public static void drawCanvasUpdate() {
        if (TgfxSettingsController.isDrawPreview()) {
            cncMachine.drawLine(TinygDriver.getInstance().getMachine()
                    .getMotionMode().get(), TinygDriver.getInstance().getMachine().getVelocity());
        }
    }

    private void drawTable() {
        //TODO  We need to make this a message to subscribe to.
        if (!gcodePane.getChildren().contains(cncMachine)) {
            // Add the cnc machine to the gcode pane
            gcodePane.getChildren().add(cncMachine);
        }
    }

    @FXML
    private void handleHomeXYZ(ActionEvent evt) {
        if (TinygDriver.getInstance().isConnected().get()) {
            TinygDriver.getInstance()
                    .write(CommandManager.CMD_APPLY_SYSTEM_HOME_XYZ_AXES);
        }
    }

    @FXML
    private void handleHomeAxisClick(ActionEvent evt) {
        MenuItem m = (MenuItem) evt.getSource();
        String _axis = String.valueOf(m.getId().charAt(0));
        if (TinygDriver.getInstance().isConnected().get()) {
            switch (_axis) {
                case "x":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_HOME_X_AXIS);
                    break;
                case "y":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_HOME_Y_AXIS);
                    break;
                case "z":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_HOME_Z_AXIS);
                    break;
                case "a":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_HOME_A_AXIS);
                    break;
            }
        }
        Main.postConsoleMessage("Homing " + _axis.toUpperCase() + " Axis...\n");
    }

    @FXML
    private void handleZeroAxisClick(ActionEvent evt) {
        MenuItem m = (MenuItem) evt.getSource();
        String _axis = String.valueOf(m.getId().charAt(0));
        if (TinygDriver.getInstance().isConnected().get()) {
            //We set this so we do not draw lines for the previous position to the new zero.
            Draw2d.setFirstDraw(true);
            switch (_axis) {
                case "x":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_ZERO_X_AXIS);
                    break;
                case "y":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_ZERO_Y_AXIS);
                    break;
                case "z":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_ZERO_Z_AXIS);
                    break;
                case "a":
                    TinygDriver.getInstance().write(CommandManager.CMD_APPLY_ZERO_A_AXIS);
                    break;
            }
        }
        Main.postConsoleMessage("Zeroed " + _axis.toUpperCase() + " Axis...\n");
    }

    @FXML
    private void handleDroMouseClick(MouseEvent me) {
        if (me.isSecondaryButtonDown()) { //Check to see if its a Right Click
            Gauge l = (Gauge) me.getSource();
            String t = String.valueOf(l.idProperty().get().charAt(0));
        }
    }

    public static void setCNCMachineVisible(boolean t) {
        cncMachine.setVisible(t);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        /*
         * add support for zmove
         */
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

        //We default to NOT display the CNC machine pane.
        // Once the serial port is connected we will show this.
        setCNCMachineVisible(false);

        //This adds our CNC Machine (2d preview) to our display window
        if (!gcodePane.getChildren().contains(cncMachine)) {
            gcodePane.getChildren().add(cncMachine); // Add the cnc machine to the gcode pane
        }

        coordLocationGridPane.visibleProperty().bind(cncMachine.visibleProperty());  //This shows the coords when the cncMachine is visible.

        xLcd.valueProperty().bind(TinygDriver.getInstance().getMachine().getAxisByName("x")
                .getMachinePositionSimple().subtract(TinygDriver.getInstance().getMachine().getAxisByName("x").
                        getOffset()).divide(TinygDriver.getInstance().getMachine().gcodeUnitDivision));
        yLcd.valueProperty().bind(TinygDriver.getInstance().getMachine().getAxisByName("y")
                .getMachinePositionSimple().subtract(TinygDriver.getInstance().getMachine().getAxisByName("y")
                        .getOffset()).divide(TinygDriver.getInstance().getMachine().gcodeUnitDivision));
        zLcd.valueProperty().bind(TinygDriver.getInstance().getMachine().getAxisByName("z")
                .getMachinePositionSimple().subtract(TinygDriver.getInstance().getMachine().getAxisByName("z")
                        .getOffset()).divide(TinygDriver.getInstance().getMachine().gcodeUnitDivision));
        aLcd.valueProperty().bind(TinygDriver.getInstance().getMachine().getAxisByName("a")
                .getMachinePositionSimple().subtract(TinygDriver.getInstance().getMachine().getAxisByName("a")
                        .getOffset()));
        velLcd.valueProperty().bind(TinygDriver.getInstance().getMachine().velocity);

        /*
         * BINDINGS CODE
         */
        // FIXME: java.lang.RuntimeException: HBox.disable : A bound value cannot be set.
        //gcodeTabControllerHBox.disableProperty().bind(TinygDriver.getInstance().getConnectionStatus().not());


        /*
         * CHANGE LISTENERS
         */
        xLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = TinygDriver.getInstance().getMachine()
                    .getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });


        yLcd.valueProperty().addListener((ov, oldValue, newValue) -> {
            double tmp = TinygDriver.getInstance().getMachine()
                    .getAxisByName("y").getWorkPosition().doubleValue() + 5;
        });

        TinygDriver.getInstance().getMachine().getGcodeUnitMode()
                .addListener((ov, oldValue, newValue) -> {
            String tmp = TinygDriver.getInstance().getMachine().getGcodeUnitMode().get();

            Main.postConsoleMessage("Gcode Unit Mode Changed to: " + tmp + "\n");

            try {
                TinygDriver.getInstance().serialWriter.setThrottled(true);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_MOTOR_1_SETTINGS);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_MOTOR_2_SETTINGS);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_MOTOR_3_SETTINGS);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_MOTOR_4_SETTINGS);

                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_X);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_Y);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_Z);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_A);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_B);
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_QUERY_AXIS_C);
                Thread.sleep(400);
                TinygDriver.getInstance().serialWriter.setThrottled(false);
            } catch (InterruptedException ex) {
                logger.error("Error querying tg model state on gcode unit change.  " +
                        "Main.java binding section.");
            }
        });

        cncMachine.heightProperty().addListener((o, oldVal, newVal) ->
                logger.info("cncHeightChanged: " + cncMachine.getHeight()));
        cncMachine.maxWidthProperty().addListener((ov, oldValue, newValue) ->
                handleMaxWithChange());
        cncMachine.maxHeightProperty().addListener((ov, oldValue, newValue) ->
                handleMaxHeightChange());

        /*
         * GCODE FILE CODE
         */
        data = FXCollections.observableArrayList();
        for(GcodeLine line : data){
            System.err.println("----"+line);
        }

        gcodeCol.setCellValueFactory(new PropertyValueFactory<>("codeLine"));
        GcodeLine n = new GcodeLine("Click open to load..", 0);


        // FIXME: gcodeView == null here for some reason
//        gcodeView.getItems().setAll(data);
//        data.add(n);

//        gcodeView.setItems(data);

//        gcodeView.addEventHandler(MouseEvent.MOUSE_CLICKED,
//                new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent me) {
//                if (me.getButton().equals(me.getButton().PRIMARY)) {
//                    if (me.getClickCount() == 2) {
//                        GcodeLine gcl = (GcodeLine) gcodeView.getSelectionModel().getSelectedItem();
//                        if (TinygDriver.getInstance().isConnected().get()) {
//                            logger.info("Double Clicked gcodeView " + gcl.getCodeLine());
//                            TinygDriver.getInstance().write(gcl.getGcodeLineJsonified());
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

    private Gauge getLcdByAxisName(String _axis) {
        switch (_axis) {
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

    @FXML
    private void handleZeroSystem(ActionEvent evt) {
        cncMachine.zeroSystem();
    }

    @FXML
    private void handlePauseResumeAct(ActionEvent evt) {
        if ("Pause".equals(pauseResume.getText())) {
            pauseResume.setText("Resume");
            TinygDriver.getInstance().priorityWrite(CommandManager.CMD_APPLY_PAUSE);
        } else {
            pauseResume.setText("Pause");
            TinygDriver.getInstance().priorityWrite(CommandManager.CMD_APPLY_RESUME);
        }
    }

    @FXML
    private void handleClearScreen(ActionEvent evt) {
        Main.postConsoleMessage("Clearing Screen...\n");
        cncMachine.clearScreen();
        //clear this so our first line added draws correctly
        Draw2d.setFirstDraw(true);
    }

    @FXML
    private void handleReset(ActionEvent evt) {
        Platform.runLater(() -> {
            try {
                TinygDriver.getInstance().serialWriter.clearQueueBuffer();
                //This sends the 0x18 byte
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_APPLY_RESET);

                //We disable everything while waiting for the board to reset
//                 topAnchorPane.setDisable(true);
//                 topTabPane.setDisable(true);

//                Thread.sleep(8000);
//                onConnectActions();
                Main.postConsoleMessage("Resetting TinyG....\n.");
                TinygDriver.getInstance().serialWriter.notifyAck();
                TinygDriver.getInstance().serialWriter.clearQueueBuffer();
                cncMachine.clearScreen();
                // We set this to false to allow us to jog again
                isSendingFile.set(false);
            } catch (SerialPortException ex) {
                logger.error("handleReset " + ex.getMessage());
            }
        });
    }

    @FXML
    private void handleStop(ActionEvent evt) {
        Platform.runLater(() -> {
            logger.info("Stopping Job Clearing Serial Queue...\n");
            CommandManager.stopTinyGMovement();
            isSendingFile.set(false); //We set this to false to allow us to jog again
        });
    }

    @FXML
    static void handleTestButton(ActionEvent evt) {
        //logger.info("Test Button....");
        updateProgress(test);
        test += 5;

        //Main.postConsoleMessage("Test!");
        //timeElapsedTxt.setText("hello");

//        Iterator ii = null;
//        Line l;
//        cncMachine.getChildren().iterator();
//        while(ii.hasNext()){
//            l = (Line) ii.next();
//            
//        }
    }

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
                        TinygDriver.getInstance().write("**COMMENT**" + gcodeLine.getCodeLine());
//                            Main.postConsoleMessage("GCODE COMMENT:" + gcodeLine.getCodeLine());
                        continue;
                    }

                    line.setLength(0);
                    line.append("{\"gc\":\"").append(gcodeLine.getCodeLine()).append("\"}\n");
                    TinygDriver.getInstance().write(line.toString());
                }
            }

            TinygDriver.getInstance().write("**FILEDONE**");
            return true;
            }
        };
    }

    public static void setIsFileSending(boolean flag) {
        isSendingFile.set(flag);
    }

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

    private synchronized boolean isTaskActive() {
        return taskActive;
    }

    public synchronized void setTaskActive(boolean boolTask) {
        taskActive = boolTask;
    }

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
//                    logger.info("File Loading Complete");
            } catch (FileNotFoundException ex) {
                logger.error("File Not Found.");
            } catch (IOException ex) {
                logger.error(ex);
            }
        });
    }

    private boolean normalizeGcodeLine(String gcl) {
        byte[] tmpLine = gcl.getBytes();
        //0x21 = !
        //0x18 = Ctrl-X
        //0x7e = ~
        //0x25 = %
        //These are considered bad bytes in gcode files.  These will trigger tinyg to throw interrupts

        for (int i = 0; i < tmpLine.length; i++) {
        }

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

    /*
     * EVENT LISTENERS CODE
     */
    //FIXME: possible NPEs
    private void handleMaxHeightChange() {
        Machine machine = TinygDriver.getInstance().getMachine();
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
//            scaleAmount = (gcodePane.heightProperty().get() /
//                            machine.getAxisByName("y")
//                                .getTravelMaxSimple().get()) * .80;  //%80 of the scale;
        }
        cncMachine.autoScaleWorkTravelSpace(scaleAmount);
//        widthSize.textProperty().bind( Bindings.format("%s",
//        cncMachine.widthProperty().divide(TinygDriver.getInstance()
//        .m.gcodeUnitDivision).asString().concat(TinygDriver.getInstance()
//        .m.getGcodeUnitMode())    ));  //.asString().concat(TinygDriver.getInstance().m.getGcodeUnitMode().get()));

//        heightSize.setText(decimalFormat.format(TinygDriver.getInstance()
//        .m.getAxisByName("y").getTravelMaximum()) + " " + TinygDriver.getInstance()
//        .m.getGcodeUnitMode().getValue());
    }

    //FIXME: possible NPEs
    private void handleMaxWithChange() {
        Machine machine = TinygDriver.getInstance().getMachine();
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
        cncMachine.autoScaleWorkTravelSpace(scaleAmount);
//        widthSize.setText(decimalFormat.format(TinygDriver.getInstance()
//        .m.getAxisByName("x").getTravelMaximum()) + " " + TinygDriver.getInstance()
//        .m.getGcodeUnitMode().getValue());
    }

    // Scroll Gcode table view to specified line, show elapsed and remaining time
    public static void updateProgress(int lineNum) {
        if (isSendingFile.get() && lineNum > 0) {
//            gcodeView.scrollTo(lineNum);

            // Show elapsed and remaining time
            Date currentTimeDt = new Date();  // Get current time
            long elapsed = currentTimeDt.getTime() - timeStartDt.getTime();
            // FIXME: integer division
            float rate = elapsed / lineNum;
            long remain = (long) ((totalGcodeLines - lineNum) * rate);  // remaining lines * secs per line

            timeElapsedTxt.setText(String.format("%02d:%02d", elapsed / 60000, (elapsed / 1000) % 60));
            timeLeftTxt.setText(String.format("%02d:%02d", remain / 60000, (remain / 1000) % 60));
        }
    }
}
