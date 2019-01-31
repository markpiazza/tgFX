/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.system;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import tgfx.system.enums.AxisMode;
import tgfx.system.enums.AxisName;
import tgfx.system.enums.AxisType;
import tgfx.system.enums.SwitchMode;
import tgfx.tinyg.MnemonicManager;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

/**
 * [xmp] x_machine_position 0.000 mm [xwp] x_work_position 0.000 mm [xam]
 * x_axis_mode 1 [standard] [xfr] x_feedrate_maximum 2400.000 mm/min [xvm]
 * x_velocity_maximum 2400.000 mm/min [xtm] x_travel_maximum 400.000 mm [xjm]
 * x_jerk_maximum 100000000 mm/min^3 [xjd] x_junction_deviation 0.0500 mm [xsm]
 * x_switch_mode 1 [0,1] [xht] x_homing_travel 400.000 mm [xhs]
 * x_homing_search_velocity 2400.000 mm/min [xhl] x_homing_latch_velocity
 * 100.000 mm/min [xhz] x_homing_zero_offset 5.000 mm [xhw] x_homing_work_offset
 * 200.000 mm
 *
 */
public final class Axis {
    private static final Logger logger = LogManager.getLogger();

    private String currentAxisJsonObject;
    private AxisType axisType;
    private SimpleBooleanProperty homed;
    private float latchVelocity;
//    private float seekRateMaximum;
    private double latchBackoff;
    private double zeroBackoff;
    private double machine_position;
    private SimpleDoubleProperty workPosition;
    private SimpleDoubleProperty machinePosition;
    private AxisMode axisMode;
    private double radius;
    private double searchVelocity;
    private double feedRateMaximum;
    private double velocityMaximum;
    private SimpleDoubleProperty travelMaximum;
    private SimpleDoubleProperty offset;
    private double jerkMaximum;
    private double jerkHomingMaximum;
    private double junctionDeviation;
    private SwitchMode maxSwitchMode = SwitchMode.DISABLED;
    private SwitchMode minSwitchMode = SwitchMode.DISABLED;
    //    private float homing_travel;
//    private float homing_search_velocity;
//    private float homing_latch_velocity;
//    private float homing_zero_offset;
//    private float homing_work_offset;
    private String axisName;
    private List<Motor> motors = new ArrayList<>();

    private DecimalFormat decimalFormat;
    private DecimalFormat decimalFormatJunctionDeviation;
    private DecimalFormat decimalFormatMaximumJerk;

    public String getCurrentAxisJsonObject() {
        return currentAxisJsonObject;
    }

    public void setCurrentAxisJsonObject(String currentAxisJsonObject) {
        this.currentAxisJsonObject = currentAxisJsonObject;
    }

//    public Axis() {
//        axisMode = AxisMode.STANDARD;
////        latchVelocity = 0;
////        latchBackoff = 0;
////        machine_position = 0;
////        feed_rate_maximum = 800;
////        jerk_maximum = 0;
//
//    }


    public Axis(AxisName axisName, AxisType axisType, AxisMode axisMode) {
        this.axisMode = axisMode;
        if (axisName == AxisName.X) {
            this.setAxisName("X");
            this.setAxisType(axisType);

        } else if (axisName == AxisName.Y) {
            this.setAxisName("Y");
            this.setAxisType(axisType);

        } else if (axisName == AxisName.Z) {
            this.setAxisName("Z");
            this.setAxisType(axisType);

        } else if (axisName == AxisName.A) {
            this.setAxisName("A");
            this.setAxisType(axisType);

        } else if (axisName == AxisName.B) {
            this.setAxisName("B");
            this.setAxisType(axisType);

        } else if (axisName == AxisName.C) {
            this.setAxisName("C");
            this.setAxisType(axisType);
        } else {
            logger.info("Invalid Axis Name Specified.\n");
        }

        homed = new SimpleBooleanProperty(false);
        workPosition = new SimpleDoubleProperty();
        machinePosition = new SimpleDoubleProperty();
        travelMaximum = new SimpleDoubleProperty();
        offset = new SimpleDoubleProperty();
        decimalFormat = new DecimalFormat("#.000");
        decimalFormatJunctionDeviation = new DecimalFormat("0.000000");
        decimalFormatMaximumJerk = new DecimalFormat("################################.############################");
    }


    // FIXME: All of the logging in here can be moved to their respective functions
    public void setAxisCommand(String cmd, String value) {
        // https://github.com/synthetos/TinyG/wiki/TinyG-Command-Line

        // This is a blind commmand mode...  meaning that
        // if 2 strings (key, val) are passed to the axis class
        // Then we parse the command and set the value.
        switch (cmd) {
            case "am": { // access mode
                int val = Double.valueOf(value).intValue();
                // FIXME: this switch statement is unnecessary and is only to provide logger info
                String _axisMode;
                switch (val) {
                    case 0:
                        _axisMode = "DISABLED";
                        break;
                    case 1:
                        _axisMode = "STANDARD";
                        break;
                    case 2:
                        _axisMode = "INHIBITED";
                        break;
                    case 3:
                        _axisMode = "RADIUS";
                        break;
                    case 4:
                        _axisMode = "SLAVE_X";
                        break;
                    case 5:
                        _axisMode = "SLAVE_Y";
                        break;
                    case 6:
                        _axisMode = "SLAVE_Z";
                        break;
                    case 7:
                        _axisMode = "SLAVE_XY";
                        break;
                    case 8:
                        _axisMode = "SLAVE_XZ";
                        break;
                    case 9:
                        _axisMode = "SLAVE_YZ";
                        break;
                    case 10:
                        _axisMode = "SLAVE_XYZ";
                        break;
                    default:
                        _axisMode = "UKNOWN";
                        break;
                }
                this.setAxisMode(val);
                logger.info("\tSet Axis: " + this.getAxisName() + " Axis Mode to: " + _axisMode);
                break;
            }
            case "vm": { // maximum velocity
                int val = (int) Double.parseDouble(value);
                this.setVelocityMaximum(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Velocity Max to: " + this.getVelocityMaximum());
                break;
            }
            case "fr": { // maximum feed rate
                int val = (int) Double.parseDouble(value);
                this.setFeedRateMaximum(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Feed Rate Max to: " + this.getFeedRateMaximum());
                break;
            }
            case "tm": { // travel maximum
                int val = (int) Double.parseDouble(value);
                this.setTravelMaximum(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Travel Max to: " + this.getTravelMaximum());
                break;
            }
            case "jm": { // jerk maximum
                int val = (int) Double.parseDouble(value);
                this.setJerkMaximum(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Jerk Max to: " + this.getJerkMaximum());
                break;
            }
            case "jh": { // jerk homing
                int val = (int) Double.parseDouble(value);
                this.setJerkHomingMaximum(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Jerk Homing to: " + this.getJerkHomingMaximum());
                break;
            }
            case "jd": { // junction deviation
                int val = (int) Double.parseDouble(value);
                this.setJunctionDeviation(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Junction Deviation Max to: " + this.getJunctionDeviation());
                break;
            }
            case "sn": { // switch min
                int val = Double.valueOf(value).intValue();
                // FIXME: this switch statement is unnecessary and is only to provide logger info
                String _switchMode = "UNKNOWN";
                switch (val) {
                    case 0:
                        _switchMode = "DISABLED";
                        break;
                    case 1:
                        _switchMode = "HOMING ONLY";
                        break;
                    case 2:
                        _switchMode = "HOMING AND LIMIT";
                        break;
                }
                this.setMinSwitchMode(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Axis Mode to: " + _switchMode);
                break;
            }
            case "sx": { // switch max
                int val = Double.valueOf(value).intValue();
                // FIXME: this switch statement is unnecessary and is only to provide logger info
                String _switchMode = "UNKNOWN";
                switch (val) {
                    case 0:
                        _switchMode = "DISABLED";
                        break;
                    case 1:
                        _switchMode = "HOMING ONLY";
                        break;
                    case 2:
                        _switchMode = "HOMING AND LIMIT";
                        break;
                }
                this.setMaxSwitchMode(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Axis Mode to: " + _switchMode);
                break;
            }
            case "sv": { // search velocity
                int val = (int) Double.parseDouble(value);
                this.setSearchVelocity(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Search Velocity to: " + this.getSearchVelocity());
                break;
            }
            case "lv": { // latch velocity
                int val = (int) Double.parseDouble(value);
                this.setLatchVelocity(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Latch Velocity to: " + this.getLatchVelocity());
                break;
            }
            case "lb": { // latch backoff
                int val = (int) Double.parseDouble(value);
                this.setLatchBackoff(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Latch Back Off to: " + this.getLatchBackoff());
                break;
            }
            case "zb": { // zero backoff
                int val = (int) Double.parseDouble(value);
                this.setZeroBackoff(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Zero Back Off to: " + this.getZeroBackoff());
                break;
            }
            case "ra": {
                int val = (int) Double.parseDouble(value);
                this.setRadius(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Radius to: " + this.getRadius());
                break;
            }
            default: {
                logger.info("Error... No such setting: " + value + " in Axis Settings...");
            }
        }
    }

    public String getAxisName() {
        return axisName;
    }

    private void setAxisName(String axisName) {
        this.axisName = axisName;
    }


    private void setAxisType(AxisType axisType) {
        this.axisType = axisType;
    }

    public AxisType getAxisType() {
        return this.axisType;
    }

    public AxisMode getAxisMode() {
        return axisMode;
    }

    //TODO: this could use the enum's 'functions'
    private void setAxisMode(int axMode) {
        switch (axMode) {
            case 0:
                this.axisMode = AxisMode.DISABLE;
                return;
            case 1:
                this.axisMode = AxisMode.STANDARD;
                return;
            case 2:
                this.axisMode = AxisMode.INHIBITED;
                return;
            case 3:
                this.axisMode = AxisMode.RADIUS;
                return;
            case 4:
                this.axisMode = AxisMode.SLAVE_X;
                return;
            case 5:
                this.axisMode = AxisMode.SLAVE_Y;
                return;
            case 6:
                this.axisMode = AxisMode.SLAVE_Z;
                return;
            case 7:
                this.axisMode = AxisMode.SLAVE_XY;
                return;
            case 8:
                this.axisMode = AxisMode.SLAVE_XZ;
                return;
            case 9:
                this.axisMode = AxisMode.SLAVE_YZ;
                return;
            case 10:
                this.axisMode = AxisMode.SLAVE_XYZ;
                return;
            default:
        }
    }

    public double getVelocityMaximum() {
        return formatDoubleValue(velocityMaximum);
    }

    private void setVelocityMaximum(double velocityMaximum) {
        this.velocityMaximum = velocityMaximum;
    }

    public double getFeedRateMaximum() {
        return formatDoubleValue(feedRateMaximum);
    }

    private void setFeedRateMaximum(float feedRateMaximum) {
        this.feedRateMaximum = feedRateMaximum;
    }

    public SimpleDoubleProperty getTravelMaxSimple() {
        return travelMaximum;
    }

    public double getTravelMaximum() {
        return formatDoubleValue(travelMaximum.getValue());
    }

    private void setTravelMaximum(float travelMaximum) {
        //Stub to always track the largest travel axis
        List allAxis = TinygDriver.getInstance().getMachine().getAllLinearAxis();
        Iterator iterator = allAxis.iterator();
        double maxTravel = 0;
        Axis axis;

        while (iterator.hasNext()) {
            axis = (Axis) iterator.next();
            if (axis.getTravelMaximum() > maxTravel) {
                //This is the largest travel max so far.. lets set it.
                maxTravel = axis.getTravelMaximum();
            }
            // We set this binding now to the largest value
            TinygDriver.getInstance().getMachine().setLongestTravelAxisValue(maxTravel);
        }
        this.travelMaximum.set(travelMaximum);
    }


    public double getJerkMaximum() {
        return jerkMaximum;
    }

    private void setJerkMaximum(double jerkMaximum) {
        this.jerkMaximum = jerkMaximum;
    }

    public double getJerkHomingMaximum() {
        return jerkHomingMaximum;
    }

    public void setJerkHomingMaximum(double jerkHomingMaximum) {
        this.jerkHomingMaximum = jerkHomingMaximum;
    }

    public double getJunctionDeviation() {
        return formatJunctionDeviation(junctionDeviation);
    }

    private void setJunctionDeviation(float junctionDevation) {
        this.junctionDeviation = junctionDevation;
    }

    public SwitchMode getMaxSwitchMode() {
        return maxSwitchMode;
    }

    //TODO: this could use the enum's 'functions'
    private void setMaxSwitchMode(int maxSwitchMode) {
        switch (maxSwitchMode) {
            case 0:
                this.maxSwitchMode = SwitchMode.DISABLED;
                return;
            case 1:
                this.maxSwitchMode = SwitchMode.HOMING_ONLY;
                return;
            case 2:
                this.maxSwitchMode = SwitchMode.LIMIT_ONLY;
                return;
            case 3:
                this.maxSwitchMode = SwitchMode.HOMING_AND_LIMIT;
                return;
            default:
        }
    }

    public SwitchMode getMinSwitchMode() {
        return minSwitchMode;
    }

    //TODO: this could use the enum's 'functions'
    private void setMinSwitchMode(int minSwitchMode) {
        switch (minSwitchMode) {
            case 0:
                this.minSwitchMode = SwitchMode.DISABLED;
                return;
            case 1:
                this.minSwitchMode = SwitchMode.HOMING_ONLY;
                return;
            case 2:
                this.minSwitchMode = SwitchMode.LIMIT_ONLY;
                return;
            case 3:
                this.minSwitchMode = SwitchMode.HOMING_AND_LIMIT;
                return;
            default:
        }
    }

    public double getSearchVelocity() {
        return formatDoubleValue(searchVelocity);
    }

    private void setSearchVelocity(double searchVelocity) {
        this.searchVelocity = searchVelocity;
    }

    private void setLatchVelocity(float latchVelocity) {
        this.latchVelocity = latchVelocity;
    }

    public float getLatchVelocity() {
        return formatFloatValue(latchVelocity);
    }

    public double getLatchBackoff() {
        return formatDoubleValue(latchBackoff);
    }

    private void setLatchBackoff(float latchBackoff) {
        this.latchBackoff = latchBackoff;
    }

    public double getZeroBackoff() {
        return formatDoubleValue(zeroBackoff);
    }

    private void setZeroBackoff(float zeroBackoff) {
        this.zeroBackoff = zeroBackoff;
    }

    public double getRadius() {
        return formatDoubleValue(radius);
    }

    private void setRadius(double radius) {
        this.radius = radius;
    }


//    public float getSeekRateMaximum() {
//        return seekRateMaximum;
//    }

//    public void setSeekRateMaximum(float seekRateMaximum) {
//        this.seekRateMaximum = seekRateMaximum;
//    }

//    public void setHomed(boolean choice) {
//        homed.set(choice);
//    }





//    public boolean setMotorCommand(String cmd, String value) {
//        //Generic command parser when a single axis command has been given.
//        //IE: $xsr=1200
//        //cmd would be sr and value would be 1200
//        switch (cmd) {
//            case MnemonicManager.MNEMONIC_AXIS_AXIS_MODE: {
//                int val = (int) Double.parseDouble(value);
//                return (this.setAxisMode(val));
//            }
//            case MnemonicManager.MNEMONIC_AXIS_VELOCITY_MAXIMUM:
//                return (this.setVelocityMaximum(Float.valueOf(value)));
//            case MnemonicManager.MNEMONIC_AXIS_FEEDRATE_MAXIMUM:
//                return (this.setFeedRateMaximum(Float.valueOf(value)));
//            case MnemonicManager.MNEMONIC_AXIS_TRAVEL_MAXIMUM:
//                return (this.setTravelMaximum(Float.valueOf(value)));
//            case MnemonicManager.MNEMONIC_AXIS_JERK_MAXIMUM:
//                return (this.setJerkMaximum(Double.valueOf(value)));
//            case MnemonicManager.MNEMONIC_AXIS_JUNCTION_DEVIATION:
//                return (this.setJunctionDeviation(Float.valueOf(value)));
//            case "sn": {
//                int val = (int) Double.parseDouble(value);
//                return (this.setMaxSwitchMode(val));
//            }
//            case MnemonicManager.MNEMONIC_AXIS_SEARCH_VELOCITY:
//                return (this.setSearchVelocity(Double.parseDouble(value)));
//            case MnemonicManager.MNEMONIC_AXIS_LATCH_VELOCITY:
//                return (this.setLatchVelocity(Float.parseFloat(value)));
//            case MnemonicManager.MNEMONIC_AXIS_LATCH_BACKOFF:
//                return (this.setLatchBackoff(Float.parseFloat(value)));
//            case MnemonicManager.MNEMONIC_AXIS_ZERO_BACKOFF:
//                return (this.setZeroBackoff(Float.parseFloat(value)));
//            default:
//                return false;
//        }
//    }


    public SimpleDoubleProperty getWorkPosition() {
        return workPosition;
    }

    public void setWorkPosition(double workPosition) {
        this.workPosition.set(workPosition);
    }

    public SimpleDoubleProperty getMachinePositionSimple() {
        return machinePosition;
    }

    void setMachinePosition(double machinePosition) {
        this.machinePosition.set(machinePosition);
    }

    public double getMachinePosition() {
        return machine_position;
    }

    public boolean setMachinePosition(float machinePosition) {
        this.machine_position = machinePosition;
        return true;
    }

    public SimpleDoubleProperty getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset.set(offset);
    }

    public List<Motor> getMotors() {
        return motors;
    }

    public boolean addMotor(Motor motor) {
        if (!motors.contains(motor)) {
            motors.add(motor);
            return true;
        }
        return false;
    }

    public void setMotors(List<Motor> motors) {
        this.motors = motors;
    }




    private double formatDoubleValue(double val) {
        //Utility Method to cleanly trim doubles for display in the UI
        return Double.parseDouble(decimalFormat.format(val));
    }

    private double formatJunctionDeviation(double val) {
        //Utility Method to cleanly trim doubles for display in the UI
        return Double.parseDouble(decimalFormatJunctionDeviation.format(val));
    }

    private double formatJerkMaximum(double val) {
        //Utility Method to cleanly trim doubles for display in the UI
        return Double.parseDouble(decimalFormat.format(val));
    }

    private float formatFloatValue(float val) {
        //Utility Method to cleanly trim doubles for display in the UI
        return Float.parseFloat(decimalFormat.format(val));
    }




    public void applyJsonSystemSetting(ResponseCommand rc) {
        _applyJsonSystemSetting(rc);
    }

    //This is the main method to parser a JSON Axis object
    public void applyJsonSystemSetting(JSONObject js, String parent) {
        logger.info("Applying JSON Object to " + parent + " Group");
        Iterator<String> ii = js.keySet().iterator();
        try {
            while (ii.hasNext()) {
                String key = ii.next();
                String val = js.get(key).toString();
                ResponseCommand rc = new ResponseCommand(parent, key, val);
                _applyJsonSystemSetting(rc);
            }

        } catch (JSONException | NumberFormatException ex) {
            logger.error("Error in applyJsonSystemSetting in Axis");
        }

    }

    private void _applyJsonSystemSetting(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();

        Axis axis = machine.getAxisByName(rc.getSettingParent());
        if(axis == null){
            logger.error("Invalid Axis");
            return;
        }

        switch (rc.getSettingKey()) {
            case (MnemonicManager.MNEMONIC_AXIS_AXIS_MODE):

                axis.setAxisMode(Double.valueOf(rc.getSettingValue()).intValue());
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_FEEDRATE_MAXIMUM):
                axis.setFeedRateMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " "
                        + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_JERK_MAXIMUM):
                axis.setJerkMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_JUNCTION_DEVIATION):
                axis.setJunctionDeviation(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_LATCH_BACKOFF):
                axis.setLatchBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_LATCH_VELOCITY):
                axis.setLatchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_MAX_SWITCH_MODE):
                axis.setMaxSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_MIN_SWITCH_MODE):
                axis.setMinSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_RADIUS):
                axis.setRadius(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_SEARCH_VELOCITY):
                axis.setSearchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_TRAVEL_MAXIMUM):
                axis.setTravelMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_VELOCITY_MAXIMUM):
                axis.setVelocityMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_ZERO_BACKOFF):
                axis.setZeroBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            default:
                logger.info("Default Switch");
        }
    }
}