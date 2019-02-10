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
     *
     * @param id
     */
    public Motor(int id) {
        idNumber = id;
    }


    /**
     *
     * @return
     */
    public String getCurrentMotorJsonObject() {
        return currentMotorJsonObject;
    }


    /**
     *
     * @param currentMotorJsonObject
     */
    public void setCurrentMotorJsonObject(String currentMotorJsonObject) {
        this.currentMotorJsonObject = currentMotorJsonObject;
    }


    /**
     * Small wrappers to return int's vs bools
     * @return
     */
    public int isPolarityInt() {
        return isPolarity() ? 1 : 0;
    }


    /**
     * Small wrappers to return int's vs bools
     * @return
     */
    public int isPowerManagementInt() {
        return isPowerManagement() ? 1 : 0;
    }


    /**
     *
     * @return
     */
    public int getIdNumber() {
        return idNumber;
    }


    /**
     *
     * @param idNumber
     */
    public void setIdNumber(int idNumber) {
        this.idNumber = idNumber;
    }


    /**
     *
     * @return
     */
    public int getMapToAxis() {
        return mapToAxis;
    }


    /**
     *
     * @param m
     */
    public void setMapToAxis(int m) {
        mapToAxis = m;
    }


    /**
     *
     * @param microsteps
     */
    public void setMicrosteps(int microsteps) {
        this.microsteps = microsteps;
    }


    /**
     *
     * @return
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
     *
     * @return
     */
    private boolean isPolarity() {
        return polarity;
    }


    /**
     *
     * @param polarity
     */
    public void setPolarity(boolean polarity) {
        this.polarity = polarity;
    }


    /**
     *
     * @param polarity
     */
    private void setPolarity(int polarity) {
        this.polarity = polarity != 0;
    }


    /**
     *
     * @return
     */
    private boolean isPowerManagement() {
        return powerManagement;
    }


    /**
     *
     * @param power_management
     */
    public void setPowerManagement(boolean power_management) {
        this.powerManagement = power_management;
    }


    /**
     *
     * @param power_management
     */
    private void setPowerManagement(int power_management) {
        this.powerManagement = power_management != 0;
    }


    /**
     *
     * @return
     */
    public float getStepAngle() {
        return stepAngle;
    }


    /**
     *
     * @param stepAngle
     */
    private void setStepAngle(float stepAngle) {
        this.stepAngle = stepAngle;
    }


    /**
     *
     * @return
     */
    public float getTravelPerRevolution() {
        return travelPerRevolution;
    }


    /**
     *
     * @param travel_per_revolution
     */
    private void setTravelPerRevolution(float travel_per_revolution) {
        this.travelPerRevolution = travel_per_revolution;
    }


    /**
     * This is the main method to parser a JSON Motor object
     * @param js
     * @param parent
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
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_MICROSTEPS:
                    motor.setMicrosteps(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_POLARITY:
                    motor.setPolarity(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_POWER_MANAGEMENT:
                    motor.setPowerManagement(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_STEP_ANGLE:
                    motor.setStepAngle(Float.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION:
                    motor.setTravelPerRevolution(Float.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;
                default:
                    logger.info("Default Switch");
                    break;
            }
        }
    }
}
