package tgfx.system;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleDoubleProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tgfx.system.enums.AxisMode;
import tgfx.system.enums.AxisName;
import tgfx.system.enums.AxisType;
import tgfx.system.enums.SwitchMode;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

import static tgfx.tinyg.Mnemonics.*;

/**
 * Axis Model
 *
 * @see <a href="https://github.com/synthetos/TinyG/wiki/TinyG-Command-Line">Tinyg Command Line</a>
 *
 * $x    --- Show all X axis settings (send g20 for inches) ---
 * [xmp] x_machine_position 0.000 mm
 * [xwp] x_work_position 0.000 mm
 * [xam] x_axis_mode 1 [standard]
 * [xfr] x_feedrate_maximum 2400.000 mm/min
 * [xvm] x_velocity_maximum 2400.000 mm/min
 * [xtm] x_travel_maximum 400.000 mm
 * [xjm] x_jerk_maximum 100000000 mm/min^3
 * [xjd] x_junction_deviation 0.0500 mm
 * [xsm] x_switch_mode 1 [0,1]
 * [xht] x_homing_travel 400.000 mm
 * [xhs] x_homing_search_velocity 2400.000 mm/min
 * [xhl] x_homing_latch_velocity 100.000 mm/min
 * [xhz] x_homing_zero_offset 5.000 mm
 * [xhw] x_homing_work_offset 200.000 mm
 */
public final class Axis {
    private static final Logger logger = LogManager.getLogger();

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.000");
    private static final DecimalFormat decimalFormatJunctionDeviation = new DecimalFormat("0.000000");

    private List<Motor> motors = new ArrayList<>();

    private String currentAxisJsonObject;

    private String axisName;
    private AxisType axisType;
    private AxisMode axisMode;

    private SimpleDoubleProperty machinePosition;
    private SimpleDoubleProperty workPosition;
    private SimpleDoubleProperty travelMaximum;
    private SimpleDoubleProperty offset;
    private SwitchMode maxSwitchMode = SwitchMode.DISABLED;
    private SwitchMode minSwitchMode = SwitchMode.DISABLED;
    private double feedRateMaximum;
    private double velocityMaximum;
    private double jerkMaximum;
    private double junctionDeviation;
    private double jerkHomingMaximum;
    private double radius;
    private double zeroBackoff;
    private double latchBackoff;
    private double searchVelocity;
    private float latchVelocity;


    /**
     * axis constructor
     * @param axisName axis's name
     * @param axisType axis's type
     * @param axisMode axis's mode
     */
    public Axis(AxisName axisName, AxisType axisType, AxisMode axisMode) {
        this.axisName = axisName.name();
        this.axisType = axisType;
        this.axisMode = axisMode;
        this.workPosition = new SimpleDoubleProperty();
        this.machinePosition = new SimpleDoubleProperty();
        this.travelMaximum = new SimpleDoubleProperty();
        this.offset = new SimpleDoubleProperty();
    }


    /**
     * get current axis json object as string
     * @return json string
     */
    public String getCurrentAxisJsonObject() {
        return currentAxisJsonObject;
    }


    /**
     * set current axis json object as string
     * @param currentAxisJsonObject json string
     */
    public void setCurrentAxisJsonObject(String currentAxisJsonObject) {
        this.currentAxisJsonObject = currentAxisJsonObject;
    }


    /**
     * get axis name
     * @return axis name
     */
    public String getAxisName() {
        return axisName;
    }


    /**
     * set axis name
     * @param axisName axis name
     */
    private void setAxisName(String axisName) {
        this.axisName = axisName;
    }


    /**
     * get axis type
     * @return axis type
     */
    public AxisType getAxisType() {
        return axisType;
    }


    /**
     * set axis type
     * @param axisType axis type
     */
    private void setAxisType(AxisType axisType) {
        this.axisType = axisType;
    }


    /**
     * get axis mode
     * @return axis mode
     */
    public AxisMode getAxisMode() {
        return axisMode;
    }


    /**
     * set axis mode by int
     * @param axMode axis mode
     */
    private void setAxisMode(int axMode) {
        this.axisMode = AxisMode.getAxisMode(axMode);
    }


    /**
     * get velocity max
     * @return velocity max
     */
    public double getVelocityMaximum() {
        return formatDoubleValue(velocityMaximum);
    }


    /**
     * set velocity max
     * @param velocityMaximum velocity max
     */
    private void setVelocityMaximum(double velocityMaximum) {
        this.velocityMaximum = velocityMaximum;
    }


    /**
     * get feed rate max
     * @return feed rate max
     */
    public double getFeedRateMaximum() {
        return formatDoubleValue(feedRateMaximum);
    }


    /**
     * set feed rate max
     * @param feedRateMaximum feed rate max
     */
    private void setFeedRateMaximum(float feedRateMaximum) {
        this.feedRateMaximum = feedRateMaximum;
    }


    /**
     * get travel max property
     * @return travel max property
     */
    public SimpleDoubleProperty travelMaximumProperty() {
        return travelMaximum;
    }


    /**
     * get travel max
     * @return travel max
     */
    public double getTravelMaximum() {
        return formatDoubleValue(travelMaximum.getValue());
    }


    /**
     * set travel max
     * @param travelMaximum travel max
     */
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


    /**
     * get jerk max
     * @return jerk max
     */
    public double getJerkMaximum() {
        return jerkMaximum;
    }


    /**
     * set jerk max
     * @param jerkMaximum jerk max
     */
    private void setJerkMaximum(double jerkMaximum) {
        this.jerkMaximum = jerkMaximum;
    }


    /**
     * get jerk homing max
     * @return jerk homing max
     */
    public double getJerkHomingMaximum() {
        return jerkHomingMaximum;
    }


    /**
     * set jerk homing max
     * @param jerkHomingMaximum jerk homing max
     */
    public void setJerkHomingMaximum(double jerkHomingMaximum) {
        this.jerkHomingMaximum = jerkHomingMaximum;
    }


    /**
     * get junction deviation
     * @return junction deviation
     */
    public double getJunctionDeviation() {
        return formatJunctionDeviation(junctionDeviation);
    }


    /**
     * set junction deviation
     * @param junctionDeviation junction deviation
     */
    private void setJunctionDeviation(float junctionDeviation) {
        this.junctionDeviation = junctionDeviation;
    }


    /**
     * get max switch mode
     * @return max switch mode
     */
    public SwitchMode getMaxSwitchMode() {
        return maxSwitchMode;
    }


    /**
     * set max switch mode by number
     * @param maxSwitchMode max switch mode
     */
    private void setMaxSwitchMode(int maxSwitchMode) {
        this.maxSwitchMode = getSwitchModeByInt(maxSwitchMode);
    }


    /**
     * get min switch mode
     * @return min switch mode
     */
    public SwitchMode getMinSwitchMode() {
        return minSwitchMode;
    }


    /**
     * set min switch mode
     * @param minSwitchMode min switch mode
     */
    private void setMinSwitchMode(int minSwitchMode) {
        this.minSwitchMode = getSwitchModeByInt(minSwitchMode);
    }


    /**
     * get switch mode by int
     * @param mode mode int
     * @return switch mode
     */
    private SwitchMode getSwitchModeByInt(int mode){
        switch (mode) {
            case 0:
                return SwitchMode.DISABLED;
            case 1:
                return SwitchMode.HOMING_ONLY;
            case 2:
                return SwitchMode.LIMIT_ONLY;
            case 3:
                return SwitchMode.HOMING_AND_LIMIT;
            default:
                return null;
        }
    }


    /**
     * get search velocity
     * @return search velocity
     */
    public double getSearchVelocity() {
        return formatDoubleValue(searchVelocity);
    }


    /**
     * set search velocity
     * @param searchVelocity search velocity
     */
    private void setSearchVelocity(double searchVelocity) {
        this.searchVelocity = searchVelocity;
    }


    /**
     * get latch velocity
     * @return latch velocity
     */
    public float getLatchVelocity() {
        return formatFloatValue(latchVelocity);
    }


    /**
     * set latch velocity
     * @param latchVelocity latch velocity
     */
    private void setLatchVelocity(float latchVelocity) {
        this.latchVelocity = latchVelocity;
    }


    /**
     * get latch backoff
     * @return latch backoff
     */
    public double getLatchBackoff() {
        return formatDoubleValue(latchBackoff);
    }


    /**
     * set latch backoff
     * @param latchBackoff latch backoff
     */
    private void setLatchBackoff(float latchBackoff) {
        this.latchBackoff = latchBackoff;
    }


    /**
     * get zero backoff
     * @return zero backoff
     */
    public double getZeroBackoff() {
        return formatDoubleValue(zeroBackoff);
    }


    /**
     * set zero backoff
     * @param zeroBackoff zero backoff
     */
    private void setZeroBackoff(float zeroBackoff) {
        this.zeroBackoff = zeroBackoff;
    }


    /**
     * get radius
     * @return radius
     */
    public double getRadius() {
        return formatDoubleValue(radius);
    }


    /**
     * set radius
     * @param radius radius
     */
    private void setRadius(double radius) {
        this.radius = radius;
    }


    /* work position */

    public SimpleDoubleProperty getWorkPosition() {
        return workPosition;
    }

    public void setWorkPosition(double workPosition) {
        this.workPosition.set(workPosition);
    }

    /* machine position */

    public SimpleDoubleProperty machinePositionProperty() {
        return machinePosition;
    }

    public double getMachinePosition() {
        return machinePosition.doubleValue();
    }

    public void setMachinePosition(double machinePosition) {
        this.machinePosition.set(machinePosition);
    }

    /* offset */

    public SimpleDoubleProperty offsetProperty() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset.set(offset);
    }

    /* motor */

    public List<Motor> getMotors() {
        return motors;
    }

    public void addMotor(Motor motor) {
        if (!motors.contains(motor)) {
            motors.add(motor);
        }
    }

    /* formatters */

    //TODO: UI code should be moved into the UI
    //Utility Method to cleanly trim doubles for display in the UI
    private double formatDoubleValue(double val) {
        return Double.parseDouble(decimalFormat.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private float formatFloatValue(float val) {
        return Float.parseFloat(decimalFormat.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private double formatJunctionDeviation(double val) {
        return Double.parseDouble(decimalFormatJunctionDeviation.format(val));
    }

    //Utility Method to cleanly trim doubles for display in the UI
    private double formatJerkMaximum(double val) {
        return Double.parseDouble(decimalFormat.format(val));
    }


    /* json formatters */


    public void applyJsonSystemSetting(JSONObject js, String parent) {
        logger.info("Applying JSON Object to " + parent + " Group");
        for (String key : js.keySet()) {
            String val = js.get(key).toString();
            ResponseCommand rc = new ResponseCommand(parent, key, val);
            applyJsonSystemSetting(rc);
        }
    }


    private void applyJsonSystemSetting(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();
        Axis axis = machine.getAxisByName(rc.getSettingParent());
        if(axis == null){
            logger.error("Invalid Axis: {}", rc.getSettingParent());
            return;
        }

        switch (rc.getSettingKey()) {
            case MNEMONIC_AXIS_AXIS_MODE:
                axis.setAxisMode(Double.valueOf(rc.getSettingValue()).intValue());
                logAxisInfo("axis mode", rc);
                break;
            case MNEMONIC_AXIS_FEEDRATE_MAXIMUM:
                axis.setFeedRateMaximum(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("feed rate max", rc);
                break;
            case MNEMONIC_AXIS_JERK_MAXIMUM:
                axis.setJerkMaximum(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("jerk max", rc);
                break;
            case MNEMONIC_AXIS_JUNCTION_DEVIATION:
                axis.setJunctionDeviation(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("junction deviation", rc);
                break;
            case MNEMONIC_AXIS_LATCH_BACKOFF:
                axis.setLatchBackoff(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("latch backoff", rc);
                break;
            case MNEMONIC_AXIS_LATCH_VELOCITY:
                axis.setLatchVelocity(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("latch velocity", rc);
                break;
            case MNEMONIC_AXIS_MAX_SWITCH_MODE:
                axis.setMaxSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logAxisInfo("max switch mode", rc);
                break;
            case MNEMONIC_AXIS_MIN_SWITCH_MODE:
                axis.setMinSwitchMode(Integer.valueOf(rc.getSettingValue()));
                logAxisInfo("min switch mode", rc);
                break;
            case MNEMONIC_AXIS_RADIUS:
                axis.setRadius(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("radius", rc);
                break;
            case MNEMONIC_AXIS_SEARCH_VELOCITY:
                axis.setSearchVelocity(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("search velocity", rc);
                break;
            case MNEMONIC_AXIS_TRAVEL_MAXIMUM:
                axis.setTravelMaximum(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("travel max", rc);
                break;
            case MNEMONIC_AXIS_VELOCITY_MAXIMUM:
                axis.setVelocityMaximum(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("velocity max", rc);
                break;
            case MNEMONIC_AXIS_ZERO_BACKOFF:
                axis.setZeroBackoff(Float.valueOf(rc.getSettingValue()));
                logAxisInfo("zero backoff", rc);
                break;
            case MNEMONIC_AXIS_JERK_HOMING:
                axis.setJerkHomingMaximum(Double.valueOf(rc.getSettingValue()));
                logAxisInfo("jerk homing max", rc);
                break;
            default:
                logAxisInfo("", rc);
        }
    }

    private void logAxisInfo(String name, ResponseCommand rc){
        String rcName = name != null ? "applied " + name : "unknown property";
        logger.info( "{}: {}, {} : {}", rcName,
                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
    }
}