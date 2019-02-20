package tgfx.ui.tinygconfig;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.MainController;
import tgfx.system.Axis;
import tgfx.system.Machine;
import tgfx.system.Motor;
import tgfx.tinyg.TinygDriver;

/**
 * TinyGConfigController
 *
 * FXML Controller class
 *
 */
public class TinyGConfigController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    private static TinygDriver DRIVER = TinygDriver.getInstance();

    private static DecimalFormat decimalFormat = new DecimalFormat("#0.000");

    @FXML
    private TabPane axisTabPane, motorTabPane;


    // axises
    @FXML
    private TextField
            motor1ConfigTravelPerRev, motor2ConfigTravelPerRev,
            motor3ConfigTravelPerRev, motor4ConfigTravelPerRev,
            motor1ConfigStepAngle, motor2ConfigStepAngle,
            motor3ConfigStepAngle, motor4ConfigStepAngle;

    @FXML
    private ChoiceBox
            axisAmode, axisBmode, axisCmode,
            axisXmode, axisYmode, axisZmode,
            axisAswitchModeMin, axisAswitchModeMax,
            axisBswitchModeMin, axisBswitchModeMax,
            axisCswitchModeMin, axisCswitchModeMax,
            axisXswitchModeMin, axisXswitchModeMax,
            axisYswitchModeMin, axisYswitchModeMax,
            axisZswitchModeMin, axisZswitchModeMax;


    // motors
    @FXML
    private TextField
            axisAmaxFeedRate, axisBmaxFeedRate, axisCmaxFeedRate,
            axisXmaxFeedRate, axisYmaxFeedRate, axisZmaxFeedRate,
            axisAmaxTravel, axisBmaxTravel, axisCmaxTravel,
            axisXmaxTravel, axisYmaxTravel, axisZmaxTravel,
            axisAjunctionDeviation, axisBjunctionDeviation, axisCjunctionDeviation,
            axisXjunctionDeviation, axisYjunctionDeviation, axisZjunctionDeviation,
            axisAsearchVelocity, axisBsearchVelocity, axisCsearchVelocity,
            axisXsearchVelocity, axisYsearchVelocity, axisZsearchVelocity,
            axisAzeroBackoff, axisBzeroBackoff, axisCzeroBackoff,
            axisXzeroBackoff, axisYzeroBackoff, axisZzeroBackoff,
            axisAmaxVelocity, axisBmaxVelocity, axisCmaxVelocity,
            axisXmaxVelocity, axisYmaxVelocity, axisZmaxVelocity,
            axisAmaxJerk, axisBmaxJerk, axisCmaxJerk,
            axisXmaxJerk, axisYmaxJerk, axisZmaxJerk,
            axisAradius, axisBradius, axisCradius,
            axisAlatchVelocity, axisBlatchVelocity, axisClatchVelocity,
            axisXlatchVelocity, axisYlatchVelocity, axisZlatchVelocity,
            axisXlatchBackoff, axisYlatchBackoff, axisZlatchBackoff,
            axisAlatchBackoff, axisBlatchBackoff, axisClatchBackoff;

    @FXML
    private ChoiceBox
            motor1ConfigMapAxis, motor2ConfigMapAxis,
            motor3ConfigMapAxis, motor4ConfigMapAxis,
            motor1ConfigMicroSteps, motor2ConfigMicroSteps,
            motor3ConfigMicroSteps, motor4ConfigMicroSteps,
            motor1ConfigPolarity, motor2ConfigPolarity,
            motor3ConfigPolarity, motor4ConfigPolarity,
            motor1ConfigPowerMode, motor2ConfigPowerMode,
            motor3ConfigPowerMode, motor4ConfigPowerMode;



    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Initializing TinyGConfigController");
    }


    /* axis enable/disable */


    @FXML
    private void handleEnableAllAxis(ActionEvent evt) throws Exception {
        if (DRIVER.isConnected().get()) {
            MainController.postConsoleMessage("Enabling All Axis.... Motors Live!.\n");
            logger.info("Enabling All Axis");
            DRIVER.getCommandManager().enableAllAxis();

        } else {
            MainController.postConsoleMessage("TinyG is Not Connected...\n");
        }

    }


    @FXML
    private void handleInhibitAllAxis(ActionEvent evt) throws Exception {
        if (DRIVER.isConnected().get()) {
            MainController.postConsoleMessage("Inhibiting All Axis.... " +
                    "Motors Inhibited... However always verify!\n");
            logger.info("Inhibiting All Axis");
            DRIVER.getCommandManager().inhibitAllAxis();
        } else {
            MainController.postConsoleMessage("TinyG is Not Connected...\n");
        }
    }


    /* axis handlers */


    @FXML
    private void handleAxisEnter(final InputEvent event) {
        //private void handleEnter(ActionEvent event) throws Exception {
        final KeyEvent keyEvent = (KeyEvent) event;
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            Axis _axis = Machine.getInstance().getAxisByName(axisTabPane
                    .getSelectionModel().getSelectedItem().getText()
                    .toLowerCase().substring(0, 1));
            if (event.getSource().toString().startsWith("TextField")) {
                //Object Returned is a TextField Object
                TextField tf = (TextField) event.getSource();
                try {
                    MainController.postConsoleMessage("Applying Axis.......\n");
                    DRIVER.applyHardwareAxisSettings(_axis, tf);
                } catch (NumberFormatException ex) {
                    MainController.postConsoleMessage("Invalid Setting Entered.. Ignoring.");
                    logger.error(ex);
                    // This will reset the input that was bad to the current settings
                    // FIXME: Possible NPE
                    DRIVER.queryHardwareSingleAxisSettings(_axis.getAxisName());
                }
            }
        }
    }


    @FXML
    private void handleAxisQuerySettings(ActionEvent evt) {
        String _axisSelected = axisTabPane.getSelectionModel().getSelectedItem().getText().toLowerCase();
        MainController.postConsoleMessage("Querying Axis: " + _axisSelected + "\n");
        DRIVER.queryHardwareSingleAxisSettings(_axisSelected.charAt(0));
    }


    @FXML
    private void handleAxisApplySettings(ActionEvent evt) {
        MainController.postConsoleMessage("Applying Axis...\n");
        try {
            DRIVER.applyHardwareAxisSettings(
                    axisTabPane.getSelectionModel().getSelectedItem());
            //TODO: Breakout Individual response messages vs having to call queryAllHardwareAxisSettings
            //something like if {"1po":1} then parse and update only the polarity setting
//            Thread.sleep(TinygDriver.CONFIG_DELAY);
//            DRIVER.queryAllHardwareAxisSettings();
        } catch (Exception ex) {
            logger.error(ex);
        }
    }


    /**
     * called from main controller
     * @param axname
     */
    public void updateGuiAxisSettings(String axname) {
        //Update the GUI for Axis Config Settings
        final String AXIS_NAME = axname;
        Platform.runLater(() -> {
            //We are now back in the EventThread and can update the GUI for the CMD SETTINGS
            //Right now this is how I am doing this.  However I think there can be a more optimized way
            //Perhaps by passing a routing message as to which motor was updated then not all have to be updated
            //every time one is.
            if (AXIS_NAME == null) {
                //Axis was not provied as a sting argument.. so we update all of them
                for (Axis ax : DRIVER.getMachine().getAllAxis()) {
                    _updateGuiAxisSettings(ax);
                }
            } else {
                //We were provided with a specific axis to update.  Update it.
                _updateGuiAxisSettings(AXIS_NAME);
            }
        });
    }

    private void _updateGuiAxisSettings(String axname) {
        Axis ax = DRIVER.getMachine().getAxisByName(axname);
        _updateGuiAxisSettings(ax);
    }


    private void _updateGuiAxisSettings(Axis ax) {
        switch (ax.getAxisName().toLowerCase()) {
            case "a":
                axisAmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisAmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisAmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisAjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisAmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisAmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisAradius.setText(String.valueOf(ax.getRadius()));
                axisAsearchVelocity.setText(String.valueOf(ax.getSearchVelocity()));
                axisAzeroBackoff.setText(String.valueOf(ax.getZeroBackoff()));
                axisAswitchModeMax.getSelectionModel().select(ax.getMaxSwitchMode().ordinal());
                axisAswitchModeMin.getSelectionModel().select(ax.getMinSwitchMode().ordinal());
                axisAlatchBackoff.setText(String.valueOf(ax.getLatchBackoff()));
                axisAlatchVelocity.setText(String.valueOf(ax.getLatchVelocity()));
                break;
            case "b":
                axisBmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisBmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisBmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisBjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisBmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisBmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisBradius.setText(String.valueOf(ax.getRadius()));
                //Rotational Do not have these.
                axisBsearchVelocity.setDisable(true);
                axisBlatchVelocity.setDisable(true);
                axisBlatchBackoff.setDisable(true);
                axisBswitchModeMax.setDisable(true);
                axisBswitchModeMin.setDisable(true);
                axisBzeroBackoff.setDisable(true);
                break;
            case "c":
                axisCmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisCmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisCmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisCjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisCmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisCmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisCradius.setText(String.valueOf(ax.getRadius()));
                //Rotational Do not have these.
                axisCsearchVelocity.setDisable(true);
                axisClatchVelocity.setDisable(true);
                axisClatchBackoff.setDisable(true);
                axisCswitchModeMax.setDisable(true);
                axisCswitchModeMin.setDisable(true);
                axisCzeroBackoff.setDisable(true);
                break;
            case "x":
                axisXmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisXmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisXmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisXjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisXsearchVelocity.setText(String.valueOf(ax.getSearchVelocity()));
                axisXzeroBackoff.setText(String.valueOf(ax.getZeroBackoff()));
                axisXswitchModeMax.getSelectionModel().select(ax.getMaxSwitchMode().ordinal());
                axisXswitchModeMin.getSelectionModel().select(ax.getMinSwitchMode().ordinal());
                axisXmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisXmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisXlatchBackoff.setText(String.valueOf(ax.getLatchBackoff()));
                axisXlatchVelocity.setText(String.valueOf(ax.getLatchVelocity()));
                break;
            case "y":
                axisYmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisYmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisYmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisYjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisYsearchVelocity.setText(String.valueOf(ax.getSearchVelocity()));
                axisYzeroBackoff.setText(String.valueOf(ax.getZeroBackoff()));
                axisYswitchModeMax.getSelectionModel().select(ax.getMaxSwitchMode().ordinal());
                axisYswitchModeMin.getSelectionModel().select(ax.getMinSwitchMode().ordinal());
                axisYmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisYmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisYlatchVelocity.setText(String.valueOf(ax.getLatchVelocity()));
                axisYlatchBackoff.setText(String.valueOf(ax.getLatchBackoff()));
                break;
            case "z":
                axisZmode.getSelectionModel().select(ax.getAxisMode().ordinal());
                axisZmaxFeedRate.setText(String.valueOf(ax.getFeedRateMaximum()));
                axisZmaxTravel.setText(String.valueOf(ax.getTravelMaximum()));
                axisZjunctionDeviation.setText(String.valueOf(ax.getJunctionDeviation()));
                axisZsearchVelocity.setText(String.valueOf(ax.getSearchVelocity()));
                axisZzeroBackoff.setText(String.valueOf(ax.getZeroBackoff()));
                axisZswitchModeMin.getSelectionModel().select(ax.getMinSwitchMode().ordinal());
                axisZswitchModeMax.getSelectionModel().select(ax.getMaxSwitchMode().ordinal());
                axisZmaxVelocity.setText(String.valueOf(ax.getVelocityMaximum()));
                axisZmaxJerk.setText(decimalFormat.format(ax.getJerkMaximum()));
                axisZlatchVelocity.setText(String.valueOf(ax.getLatchVelocity()));
                axisZlatchBackoff.setText(String.valueOf(ax.getLatchBackoff()));
                break;
        }
    }


    /* motor handlers */


    @FXML
    private void handleMotorEnter(final InputEvent event) {
        //private void handleEnter(ActionEvent event) throws Exception {
        final KeyEvent keyEvent = (KeyEvent) event;
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            TextField tf = (TextField) event.getSource();
            Motor _motor = DRIVER.getMachine()
                    .getMotorByNumber((motorTabPane.getSelectionModel().getSelectedItem().getText()
                            .toLowerCase().split(" "))[1]);

            //TODO: move the applyHardwareMotorSettings to this controller vs TinyGDriver.
            try {
                MainController.postConsoleMessage("Applying "+ tf.getId()+ ":" +
                        tf.getText()+"... \n");
                DRIVER.applyHardwareMotorSettings(_motor, tf);
            } catch (NumberFormatException ex) {
                MainController.postConsoleMessage(tf.getText() +
                        " is an invalid Setting Entered.. Ignoring.");
                logger.error(ex);
                // This will reset the input that was bad to the current settings
                DRIVER.queryHardwareSingleMotorSettings(_motor.getIdNumber());
            }
        }
    }


    @FXML
    private void handleMotorQuerySettings(ActionEvent evt) {
        logger.info("Querying Motor Config...");
        // Detect what motor tab is "active"...
//        updateGuiAxisSettings();
        switch (motorTabPane.getSelectionModel().getSelectedItem().getText()) {
            case "Motor 1":
                DRIVER.queryHardwareSingleMotorSettings(1);
                break;
            case "Motor 2":
                DRIVER.queryHardwareSingleMotorSettings(2);
                break;
            case "Motor 3":
                DRIVER.queryHardwareSingleMotorSettings(3);
                break;
            case "Motor 4":
                DRIVER.queryHardwareSingleMotorSettings(4);
                break;
        }
    }


    @FXML
    private void handleMotorApplySettings(ActionEvent evt) {
        MainController.postConsoleMessage("Applying Motor.......\n");
        DRIVER.applyHardwareMotorSettings(
                motorTabPane.getSelectionModel().getSelectedItem());
    }


    /**
     * called from main controller
     * @param arg
     */
    public void updateGuiMotorSettings(final String arg) {
        //Update the GUI for config settings
        Platform.runLater(new Runnable() {
            String MOTOR_ARGUMENT = arg;

            @Override
            public void run() {
                if (MOTOR_ARGUMENT == null) {
                    //Update ALL motor's gui settings
                    for (Motor m : DRIVER.getMachine().getMotors()) {
                        _updateGuiMotorSettings(String.valueOf(m.getIdNumber()));
                    }
                } else {
                    //Update only ONE motor's gui settings
                    _updateGuiMotorSettings(MOTOR_ARGUMENT);
                }
            }
        });
    }


    private void _updateGuiMotorSettings(String motorNumber) {
        Machine machine = DRIVER.getMachine();
        Motor motor = machine.getMotorByNumber(Integer.valueOf(motorNumber));

        if (motor == null) {
            logger.error("Invalid Motor number");
            return;
        }

        switch (motor.getIdNumber()) {
            case 1:
                motor1ConfigMapAxis.getSelectionModel().select(motor.getMapToAxis());
                motor1ConfigMicroSteps.getSelectionModel().select(motor.getMicrosteps());
                motor1ConfigPolarity.getSelectionModel().select(motor.isPolarityInt());
                motor1ConfigPowerMode.getSelectionModel().select(motor.isPowerManagementInt());
                motor1ConfigStepAngle.setText(String.valueOf(motor.getStepAngle()));
                motor1ConfigTravelPerRev.setText(String.valueOf(motor.getTravelPerRevolution()));
                break;
            case 2:
                motor2ConfigMapAxis.getSelectionModel().select(motor.getMapToAxis());
                motor2ConfigMicroSteps.getSelectionModel().select(motor.getMicrosteps());
                motor2ConfigPolarity.getSelectionModel().select(motor.isPolarityInt());
                motor2ConfigPowerMode.getSelectionModel().select(motor.isPowerManagementInt());
                motor2ConfigStepAngle.setText(String.valueOf(motor.getStepAngle()));
                motor2ConfigTravelPerRev.setText(String.valueOf(motor.getTravelPerRevolution()));
                break;
            case 3:
                motor3ConfigMapAxis.getSelectionModel().select(motor.getMapToAxis());
                motor3ConfigMicroSteps.getSelectionModel().select(motor.getMicrosteps());
                motor3ConfigPolarity.getSelectionModel().select(motor.isPolarityInt());
                motor3ConfigPowerMode.getSelectionModel().select(motor.isPowerManagementInt());
                motor3ConfigStepAngle.setText(String.valueOf(motor.getStepAngle()));
                motor3ConfigTravelPerRev.setText(String.valueOf(motor.getTravelPerRevolution()));
                break;
            case 4:
                motor4ConfigMapAxis.getSelectionModel().select(motor.getMapToAxis());
                motor4ConfigMicroSteps.getSelectionModel().select(motor.getMicrosteps());
                motor4ConfigPolarity.getSelectionModel().select(motor.isPolarityInt());
                motor4ConfigPowerMode.getSelectionModel().select(motor.isPowerManagementInt());
                motor4ConfigStepAngle.setText(String.valueOf(motor.getStepAngle()));
                motor4ConfigTravelPerRev.setText(String.valueOf(motor.getTravelPerRevolution()));
                break;
        }
    }

}
