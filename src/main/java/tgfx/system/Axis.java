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
    public void setHomed(boolean choice) {
        homed.set(choice);
    }

    public void setAxisCommand(String cmd, String value) {
        //This is a blind commmand mode...  meaning that
        //if 2 strings (key, val) are passed to the axis class
        //Then we parse the command and set the value.
        switch (cmd) {
            case "am": {
                int val = Double.valueOf(value).intValue();
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
                logger.info("\t[+]Set Axis: " + this.getAxisName() + " Axis Mode to: " + _axisMode);
                return;
            }
            case "vm": {
                int val = (int) Double.parseDouble(value);
                this.setVelocityMaximum(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Velocity Max to: " + this.getVelocityMaximum());
                return;
            }
            case "fr": {
                int val = (int) Double.parseDouble(value);
                this.setFeed_rate_maximum(val);
                logger.info("\t[+]Set Axis: " + this.getFeedRateMaximum() +
                        " Feed Rate Max to: " + this.getFeedRateMaximum());
                return;
            }
            case "tm": {
                int val = (int) Double.parseDouble(value);
                this.setTravelMaximum(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Travel Max to: " + this.getTravelMaximum());
                return;
            }
            case "jm": {
                int val = (int) Double.parseDouble(value);
                this.setJerkMaximum(val);
                logger.info("\t[+]Set Axis: " + this.getJerkMaximum() +
                        " Jerk Max to: " + this.getJerkMaximum());
                return;
            }
            case "jd": {
                int val = (int) Double.parseDouble(value);
                this.setJunctionDeviation(val);
                logger.info("\t[+]Set Axis: " + this.getJunctionDeviation() +
                        " Junction Deviation Max to: " + this.getJunctionDeviation());
                return;
            }
            case "sx": {
                int val = Double.valueOf(value).intValue();
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
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Axis Mode to: " + _switchMode);
                return;
            }
            case "sn": {
                int val = Double.valueOf(value).intValue();
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
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Axis Mode to: " + _switchMode);
                return;
            }
            case "sv": {
                int val = (int) Double.parseDouble(value);
                this.setSearchVelocity(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Search Velocity to: " + this.getSearchVelocity());
                return;
            }
            case "lv": {
                int val = (int) Double.parseDouble(value);
                this.setLatchVelocity(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Latch Velocity to: " + this.getLatchVelocity());
                return;
            }
            case "lb": {
                int val = (int) Double.parseDouble(value);
                this.setLatchBackoff(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Latch Back Off to: " + this.getLatchBackoff());
                return;
            }
            case "zb": {
                int val = (int) Double.parseDouble(value);
                this.setZeroBackoff(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Zero Back Off to: " + this.getZeroBackoff());
                return;
            }
            case "ra": {
                int val = (int) Double.parseDouble(value);
                this.setRadius(val);
                logger.info("\t[+]Set Axis: " + this.getAxisName() +
                        " Radius to: " + this.getRadius());
                return;
            }
            default: {
                logger.info("[!]Error... No such setting: " + value + " in Axis Settings...");
            }
        }
    }

    public double getLatchBackoff() {
        return formatDoubleValue(latchBackoff);
    }

    public double getRadius() {
        return formatDoubleValue(radius);
    }

//    public void setRadius(int radius) {
//        this.radius = radius;
//    }

    private void setLatchBackoff(float latchBackoff) {
        this.latchBackoff = latchBackoff;
    }

    public float getLatchVelocity() {
        return formatFloatValue(latchVelocity);
    }

//    public float getSeekRateMaximum() {
//        return seekRateMaximum;
//    }

//    public void setSeekRateMaximum(float seekRateMaximum) {
//        this.seekRateMaximum = seekRateMaximum;
//    }

    public double getSearchVelocity() {
        return formatDoubleValue(searchVelocity);
    }

    private void setSearchVelocity(double searchVelocity) {
        this.searchVelocity = searchVelocity;
    }

    private void setLatchVelocity(float latchVelocity) {
        this.latchVelocity = latchVelocity;
    }

    public double getZeroBackoff() {
        return formatDoubleValue(zeroBackoff);
    }

    private void setZeroBackoff(float zeroBackoff) {
        this.zeroBackoff = zeroBackoff;
    }

    private void setRadius(double radius) {
        this.radius = radius;
    }

    private void setAxisType(AxisType axisType) {
        this.axisType = axisType;
    }

    public AxisType getAxisType() {
        return this.axisType;
    }

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
            logger.info("[!]Invalid Axis Name Specified.\n");
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

    public AxisMode getAxisMode() {
        return axisMode;
    }

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
//                return (this.setFeed_rate_maximum(Float.valueOf(value)));
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
//        if (axMode == 0) {
//            this.axisMode =AxisMode.DISABLE;
//        }else if( axMode == 1){
//            this.axisMode = AxisMode.
//        }
    }

    public String getAxisName() {
        return axisName;
    }

    private void setAxisName(String axisName) {
        this.axisName = axisName;
    }

    public double getFeedRateMaximum() {
        return formatDoubleValue(feedRateMaximum);
    }

    private void setFeed_rate_maximum(float feedRateMaximum) {
        this.feedRateMaximum = feedRateMaximum;
    }

    public double getJerkHomingMaximum() {
        return jerkHomingMaximum;
    }

    public void setJerkHomingMaximum(double jerkHomingMaximum) {
        this.jerkHomingMaximum = jerkHomingMaximum;
    }

    public double getJerkMaximum() {
        return jerkMaximum;
    }

    private void setJerkMaximum(double jerkMaximum) {
        this.jerkMaximum = jerkMaximum;
    }

    public double getJunctionDeviation() {
        return formatJunctionDeviation(junctionDeviation);
    }

    private void setJunctionDeviation(float junctionDevation) {
        this.junctionDeviation = junctionDevation;
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

    public SwitchMode getMaxSwitchMode() {
        return maxSwitchMode;
    }

    public SwitchMode getMinSwitchMode() {
        return minSwitchMode;
    }

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

    public SimpleDoubleProperty getTravelMaxSimple() {
        return travelMaximum;
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
            TinygDriver.getInstance().getMachine().longestTravelAxisValue.set(maxTravel);
        }
        this.travelMaximum.set(travelMaximum);
    }

    public double getVelocityMaximum() {
        return formatDoubleValue(velocityMaximum);
    }

    private void setVelocityMaximum(double velocityMaximum) {
        this.velocityMaximum = velocityMaximum;
    }

    public SimpleDoubleProperty getWorkPosition() {
        return workPosition;
    }

    public void setWorkPosition(double workPosition) {
        this.workPosition.set(workPosition);
    }

    void setMachinePosition(double machinePosition) {
        this.machinePosition.set(machinePosition);
    }

    public SimpleDoubleProperty getMachinePositionSimple() {
        return machinePosition;
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

        switch (rc.getSettingKey()) {
            case (MnemonicManager.MNEMONIC_AXIS_AXIS_MODE):
                machine.getAxisByName(rc.getSettingParent())
                        .setAxisMode(Double.valueOf(rc.getSettingValue()).intValue());
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_FEEDRATE_MAXIMUM):
                machine.getAxisByName(rc.getSettingParent())
                        .setFeed_rate_maximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " "
                        + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_JERK_MAXIMUM):
                machine.getAxisByName(rc.getSettingParent())
                        .setJerkMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_JUNCTION_DEVIATION):
                machine.getAxisByName(rc.getSettingParent())
                        .setJunctionDeviation(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_LATCH_BACKOFF):
                machine.getAxisByName(rc.getSettingParent())
                        .setLatchBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_LATCH_VELOCITY):
                machine.getAxisByName(rc.getSettingParent())
                        .setLatchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_MAX_SWITCH_MODE):
                machine.getAxisByName(rc.getSettingParent())
                        .setMaxSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_MIN_SWITCH_MODE):
                machine.getAxisByName(rc.getSettingParent())
                        .setMinSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_RADIUS):
                machine.getAxisByName(rc.getSettingParent())
                        .setRadius(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_SEARCH_VELOCITY):
                machine.getAxisByName(rc.getSettingParent()).
                        setSearchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_TRAVEL_MAXIMUM):
                machine.getAxisByName(rc.getSettingParent())
                        .setTravelMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_VELOCITY_MAXIMUM):
                machine.getAxisByName(rc.getSettingParent())
                        .setVelocityMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_AXIS_ZERO_BACKOFF):
                machine.getAxisByName(rc.getSettingParent())
                        .setZeroBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                        rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            default:
                logger.info("Default Switch");
        }
    }
}