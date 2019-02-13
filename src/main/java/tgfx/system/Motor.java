package tgfx.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

import static tgfx.tinyg.Mnemonics.*;

/**
 * Motor
 *
 */
public class Motor {
    private static final Logger logger = LogManager.getLogger();
    
    private String currentMotorJsonObject;
    private int idNumber;               // On TinyG the motor ports are 1-4
    private int mapToAxis;              // map_to_axis
    private int microsteps;             // Microsteps
    private float stepAngle;            // step angle
    private float travelPerRevolution;  // travel revolution
    private boolean polarity;           // polarity
    private boolean powerManagement;    // power management

    /*
     * What TinyG Motor Class Looks Like. 2/1/2012
     * [1ma] m1_map_to_axis 0 [0=X, 1=Y...]
     * [1sa] m1_step_angle 1.800 deg
     * [1tr] m1_travel_per_revolution 5.080 mm
     * [1mi] m1_microsteps 8 [1,2,4,8]
     * [1po] m1_polarity 1 [0,1]
     * [1pm] m1_power_management 1 [0,1]
     */

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
            case 1:
                return 0;
            case 2:
                return 1;
            case 4:
                return 2;
            case 8:
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
     * @param power_management
     */
    private void setPowerManagement(int power_management) {
        this.powerManagement = power_management != 0;
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
     * @param travel_per_revolution travel per revolution
     */
    private void setTravelPerRevolution(float travel_per_revolution) {
        this.travelPerRevolution = travel_per_revolution;
    }


    /**
     * This is the main method to parser a JSON Motor object
     * @param js json object
     * @param parent parent
     */
    public void applyJsonSystemSetting(JSONObject js, String parent) {
        logger.info("Applying JSON Object to " + parent + " Group");

        Machine machine = TinygDriver.getInstance().getMachine();
        for (Object o : js.keySet()) {
            String key = o.toString();
            String val = js.get(key).toString();
            ResponseCommand rc = new ResponseCommand(parent, key, val);
            Motor motor = machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()));
            if(motor == null){
                logger.error("Invalid Motor number");
                continue;
            }

            switch (key) {
                case MNEMONIC_MOTOR_MAP_AXIS:
                    motor.setMapToAxis(Integer.valueOf(rc.getSettingValue()));
                    logger.info( "applied map axis: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_MICROSTEPS:
                    motor.setMicrosteps(Integer.valueOf(rc.getSettingValue()));
                    logger.info( "applied microsteps: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_POLARITY:
                    motor.setPolarity(Integer.valueOf(rc.getSettingValue()));
                    logger.info( "applied polarity: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_POWER_MANAGEMENT:
                    motor.setPowerManagement(Integer.valueOf(rc.getSettingValue()));
                    logger.info( "applied power management: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_STEP_ANGLE:
                    motor.setStepAngle(Float.valueOf(rc.getSettingValue()));
                    logger.info( "applied step angle: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION:
                    motor.setTravelPerRevolution(Float.valueOf(rc.getSettingValue()));
                    logger.info( "applied travel per revolution: {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;
                default:
                    logger.error("unknown motor property: : {}, {} : {}",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                    break;
            }
        }
    }
}
