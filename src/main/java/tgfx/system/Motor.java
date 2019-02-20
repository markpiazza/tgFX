package tgfx.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

import java.util.Iterator;

import static tgfx.tinyg.Mnemonics.*;

/**
 * Motor Model
 *
 * @see <a href="https://github.com/synthetos/TinyG/wiki/TinyG-Command-Line">Tinyg Command Line</a>
 *
 * $3    --- Show all motor 3 settings ---
 * [3ma] m3_map_to_axis 0 [0=X, 1=Y...]
 * [3sa] m3_step_angle 1.800 deg
 * [3tr] m3_travel_per_revolution 5.080 mm
 * [3mi] m3_microsteps 8 [1,2,4,8]
 * [3po] m3_polarity 1 [0,1]
 * [3pm] m3_power_management 1 [0,1]
 */
public class Motor {
    private static final Logger logger = LogManager.getLogger();
    
    private String currentMotorJsonObject;

    private int idNumber;               // On TinyG the motor ports are 1-4
    private int mapToAxis;              // map_to_axis
    private float stepAngle;            // step angle
    private float travelPerRevolution;  // travel revolution
    private int microsteps;             // Microsteps
    private boolean polarity;           // polarity
    private boolean powerManagement;    // power management


    /**
     * motor constructor
     * @param id id
     */
    public Motor(int id) {
        idNumber = id;
    }


    /**
     * get current motor json object
     * @return current motor json object
     */
    public String getCurrentMotorJsonObject() {
        return currentMotorJsonObject;
    }


    /**
     * set current motor json object
     * @param currentMotorJsonObject current motor json object
     */
    public void setCurrentMotorJsonObject(String currentMotorJsonObject) {
        this.currentMotorJsonObject = currentMotorJsonObject;
    }


    /**
     * Small wrappers to return int's vs bools
     * @return polarity int
     */
    public int isPolarityInt() {
        return isPolarity() ? 1 : 0;
    }


    /**
     * Small wrappers to return int's vs bools
     * @return power management int
     */
    public int isPowerManagementInt() {
        return isPowerManagement() ? 1 : 0;
    }


    /**
     * get id number
     * @return id number
     */
    public int getIdNumber() {
        return idNumber;
    }


    /**
     * set id number
     * @param idNumber id  number
     */
    public void setIdNumber(int idNumber) {
        this.idNumber = idNumber;
    }


    /**
     * get map to axis
     * @return map to axis
     */
    public int getMapToAxis() {
        return mapToAxis;
    }


    /**
     * set map to axis
     * @param m map to axis
     */
    public void setMapToAxis(int m) {
        mapToAxis = m;
    }


    /**
     * get microsteps
     * @return microsteps
     */
    public int getMicrosteps() {
        // This is really ugly looking but this is how it works
        // with combo boxes or selection models.. ugh
        switch (microsteps) {
            case 1: // full step
                return 0;
            case 2: // half step
                return 1;
            case 4: // quarter step
                return 2;
            case 8: // eighth step
                return 3;
            default:
                return 1;
        }
    }


    /**
     * set microsteps
     * @param microsteps microsteps
     */
    public void setMicrosteps(int microsteps) {
        this.microsteps = microsteps;
    }


    /**
     * is polarity
     * @return polarity
     */
    private boolean isPolarity() {
        return polarity;
    }


    /**
     * set polarity
     * @param polarity polarity
     */
    public void setPolarity(boolean polarity) {
        this.polarity = polarity;
    }


    /**
     * set polarity
     * @param polarity polarity
     */
    private void setPolarity(int polarity) {
        this.polarity = polarity != 0;
    }


    /**
     * is power management
     * @return poer management
     */
    private boolean isPowerManagement() {
        return powerManagement;
    }


    /**
     * set power management by boolean
     * @param powerManagement power management
     */
    public void setPowerManagement(boolean powerManagement) {
        this.powerManagement = powerManagement;
    }


    /**
     * set power management by int
     * @param powerManagement power management
     */
    private void setPowerManagement(int powerManagement) {
        this.powerManagement = powerManagement != 0;
    }


    /**
     * get step angle
     * @return step angle
     */
    public float getStepAngle() {
        return stepAngle;
    }


    /**
     * set step angle
     * @param stepAngle step angle
     */
    private void setStepAngle(float stepAngle) {
        this.stepAngle = stepAngle;
    }


    /**
     * get travel per revolution
     * @return travel per revolution
     */
    public float getTravelPerRevolution() {
        return travelPerRevolution;
    }


    /**
     * set travel per revolution
     * @param travelPerRevolution travel per revolution
     */
    private void setTravelPerRevolution(float travelPerRevolution) {
        this.travelPerRevolution = travelPerRevolution;
    }

    /* json formatters */

    public void applyJsonSystemSetting(JSONObject js, String parent) {
        logger.info("Applying JSON Object to " + parent + " Group");
        Iterator<String> ii = js.keySet().iterator();
        try {
            while (ii.hasNext()) {
                String key = ii.next();
                String val = js.get(key).toString();
                ResponseCommand rc = new ResponseCommand(parent, key, val);
                applyJsonSystemSetting(rc);
            }

        } catch (JSONException | NumberFormatException ex) {
            logger.error("Error in applyJsonSystemSetting in Motor");
        }
    }



    private void applyJsonSystemSetting(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();
        Motor motor = machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()));
        if(motor == null){
            logger.error("Invalid Motor: {}", rc.getSettingParent());
            return;
        }

        switch (rc.getSettingKey()) {
            case MNEMONIC_MOTOR_MAP_AXIS:
                motor.setMapToAxis(Integer.valueOf(rc.getSettingValue()));
                logMotorInfo("map axis", rc);
                break;
            case MNEMONIC_MOTOR_MICROSTEPS:
                motor.setMicrosteps(Integer.valueOf(rc.getSettingValue()));
                logMotorInfo("microsteps", rc);
                break;
            case MNEMONIC_MOTOR_POLARITY:
                motor.setPolarity(Integer.valueOf(rc.getSettingValue()));
                logMotorInfo("polarity", rc);
                break;
            case MNEMONIC_MOTOR_POWER_MANAGEMENT:
                motor.setPowerManagement(Integer.valueOf(rc.getSettingValue()));
                logMotorInfo("power management", rc);
                break;
            case MNEMONIC_MOTOR_STEP_ANGLE:
                motor.setStepAngle(Float.valueOf(rc.getSettingValue()));
                logMotorInfo("step angle", rc);
                break;
            case MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION:
                motor.setTravelPerRevolution(Float.valueOf(rc.getSettingValue()));
                logMotorInfo("travel per revolution", rc);
                break;
            default:
                logMotorInfo("", rc);
                break;
        }
    }

    private void logMotorInfo(String name, ResponseCommand rc){
        String rcName = name != null ? "applied " + name : "unknown property";
        logger.info( "{}: {}, {} : {}", rcName,
                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
    }
}
