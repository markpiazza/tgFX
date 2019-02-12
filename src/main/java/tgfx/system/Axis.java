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

import static tgfx.tinyg.Mnemonics.*;

/*
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

/**
 * Axis
 *
 */
public final class Axis {
    private static final Logger logger = LogManager.getLogger();

    private static final DecimalFormat decimalFormat =
            new DecimalFormat("#.000");
    private static final DecimalFormat decimalFormatJunctionDeviation =
            new DecimalFormat("0.000000");
    private static final DecimalFormat decimalFormatMaximumJerk =
            new DecimalFormat("#.###");

    private List<Motor> motors = new ArrayList<>();

    private SimpleBooleanProperty homed;
    private SimpleDoubleProperty workPosition;
    private SimpleDoubleProperty machinePosition;
    private SimpleDoubleProperty travelMaximum;
    private SimpleDoubleProperty offset;

    private SwitchMode maxSwitchMode = SwitchMode.DISABLED;
    private SwitchMode minSwitchMode = SwitchMode.DISABLED;

    private AxisType axisType;
    private AxisMode axisMode;
    private String axisName;
    private String currentAxisJsonObject;
    private float latchVelocity;
//    private float seekRateMaximum;
    private double latchBackoff;
    private double zeroBackoff;
    private double machine_position;
    private double radius;
    private double searchVelocity;
    private double feedRateMaximum;
    private double velocityMaximum;
    private double jerkMaximum;
    private double jerkHomingMaximum;
    private double junctionDeviation;
//    private float homing_travel;
//    private float homing_search_velocity;
//    private float homing_latch_velocity;
//    private float homing_zero_offset;
//    private float homing_work_offset;



    public String getCurrentAxisJsonObject() {
        return currentAxisJsonObject;
    }

    public void setCurrentAxisJsonObject(String currentAxisJsonObject) {
        this.currentAxisJsonObject = currentAxisJsonObject;
    }

    public Axis(AxisName axisName, AxisType axisType, AxisMode axisMode) {
        this.axisMode = axisMode;
        this.setAxisName(axisName.name());
        this.setAxisType(axisType);
        this.homed = new SimpleBooleanProperty(false);
        this.workPosition = new SimpleDoubleProperty();
        this.machinePosition = new SimpleDoubleProperty();
        this.travelMaximum = new SimpleDoubleProperty();
        this.offset = new SimpleDoubleProperty();
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
                this.setAxisMode(val);
                logger.info("\tSet Axis: " + this.getAxisName() +
                        " Axis Mode to: " + AxisMode.getAxisMode(val).name());
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

    private void setAxisMode(int axMode) {
        this.axisMode = AxisMode.getAxisMode(axMode);
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
        List<Axis> allAxis = TinygDriver.getInstance().getMachine().getAllLinearAxis();

        double maxTravel = 0;
        for(Axis axis : allAxis){
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


    public void setMotorCommand(String cmd, String value) {
        //Generic command parser when a single axis command has been given.
        //IE: $xsr=1200
        //cmd would be sr and value would be 1200
        switch (cmd) {
            case MNEMONIC_AXIS_AXIS_MODE:
                setAxisMode((int) Double.parseDouble(value));
                break;
            case MNEMONIC_AXIS_VELOCITY_MAXIMUM:
                setVelocityMaximum(Float.valueOf(value));
                break;
            case MNEMONIC_AXIS_FEEDRATE_MAXIMUM:
                setFeedRateMaximum(Float.valueOf(value));
                break;
            case MNEMONIC_AXIS_TRAVEL_MAXIMUM:
                setTravelMaximum(Float.valueOf(value));
                break;
            case MNEMONIC_AXIS_JERK_MAXIMUM:
                setJerkMaximum(Double.valueOf(value));
                break;
            case MNEMONIC_AXIS_JUNCTION_DEVIATION:
                setJunctionDeviation(Float.valueOf(value));
                break;
            case "sn":
                setMaxSwitchMode((int) Double.parseDouble(value));
                break;
            case MNEMONIC_AXIS_SEARCH_VELOCITY:
                setSearchVelocity(Double.parseDouble(value));
                break;
            case MNEMONIC_AXIS_LATCH_VELOCITY:
                setLatchVelocity(Float.parseFloat(value));
                break;
            case MNEMONIC_AXIS_LATCH_BACKOFF:
                setLatchBackoff(Float.parseFloat(value));
                break;
            case MNEMONIC_AXIS_ZERO_BACKOFF:
                setZeroBackoff(Float.parseFloat(value));
                break;
            default:
                break;
        }
    }


    public SimpleDoubleProperty getWorkPosition() {
        return workPosition;
    }

    public void setWorkPosition(double workPosition) {
        this.workPosition.set(workPosition);
    }

    public SimpleDoubleProperty getMachinePositionSimple() {
        return machinePosition;
    }

    public void setMachinePosition(double machinePosition) {
        this.machinePosition.set(machinePosition);
    }

    public double getMachinePosition() {
        return machine_position;
    }

    public void setMachinePosition(float machinePosition) {
        this.machine_position = machinePosition;
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

    public void addMotor(Motor motor) {
        if (!motors.contains(motor)) {
            motors.add(motor);
        }
    }

    public void setMotors(List<Motor> motors) {
        this.motors = motors;
    }


    //TODO: UI code should be moved into the UI
    //Utility Method to cleanly trim doubles for display in the UI
    private double formatDoubleValue(double val) {
        return Double.parseDouble(decimalFormat.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private double formatJunctionDeviation(double val) {
        return Double.parseDouble(decimalFormatJunctionDeviation.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private double formatJerkMaximum(double val) {
        return Double.parseDouble(decimalFormat.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private float formatFloatValue(float val) {
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
            case MNEMONIC_AXIS_AXIS_MODE:
                axis.setAxisMode(Double.valueOf(rc.getSettingValue()).intValue());
                logger.info( "applied axis mode: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_FEEDRATE_MAXIMUM:
                axis.setFeedRateMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied feed rate max: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_JERK_MAXIMUM:
                axis.setJerkMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied jerk max: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_JUNCTION_DEVIATION:
                axis.setJunctionDeviation(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied junction deviation: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_LATCH_BACKOFF:
                axis.setLatchBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied latch backoff: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_LATCH_VELOCITY:
                axis.setLatchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied lat velocity: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_MAX_SWITCH_MODE:
                axis.setMaxSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info( "applied max switch mode: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_MIN_SWITCH_MODE:
                axis.setMinSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logger.info( "applied min switch mode: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_RADIUS:
                axis.setRadius(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied radius: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_SEARCH_VELOCITY:
                axis.setSearchVelocity(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied search velocity: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_TRAVEL_MAXIMUM:
                axis.setTravelMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied travel max: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_VELOCITY_MAXIMUM:
                axis.setVelocityMaximum(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied velocity max: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_ZERO_BACKOFF:
                axis.setZeroBackoff(Float.valueOf(rc.getSettingValue()));
                logger.info( "applied zero backoff: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case MNEMONIC_AXIS_JERK_HOMING:
                axis.setJerkHomingMaximum(Double.valueOf(rc.getSettingValue()));
                logger.info( "applied jerk homing max: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            case "tn":
                logger.info( "property tn, recognised, but unknown: {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                break;
            default:
                logger.info("unknown property {}, {} : {}",
                        rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue() );
        }
    }
}