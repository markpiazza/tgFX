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
    private int idNumber; //On TinyG the motor ports are 1-4
    private int mapToAxis;// map_to_axis
    private int microsteps; //Microsteps
    private float stepAngle; //step angle
    private float travelPerRevolution; //travel revolution
    private boolean polarity; //polarity
    private boolean powerManagement; //power management

    /**
     *
     * What TinyG Motor Class Looks Like. 2/1/2012 [1ma] m1_map_to_axis 0 [0=X,
     * 1=Y...] [1sa] m1_step_angle 1.800 deg [1tr] m1_travel_per_revolution
     * 5.080 mm [1mi] m1_microsteps 8 [1,2,4,8] [1po] m1_polarity 1 [0,1] [1pm]
     * m1_power_management 1 [0,1]
     *
     */
    public Motor(int id) {
        idNumber = id;
    }

    public String getCurrentMotorJsonObject() {
        return currentMotorJsonObject;
    }

    public void setCurrentMotorJsonObject(String currentMotorJsonObject) {
        this.currentMotorJsonObject = currentMotorJsonObject;
    }

    //Small wrappers to return int's vs bools
    public int isPolarityInt() {
        return isPolarity() ? 1 : 0;
    }

    //Small wrappers to return int's vs bools
    public int isPowerManagementInt() {
        return isPowerManagement() ? 1 : 0;
    }

    public int getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(int idNumber) {
        this.idNumber = idNumber;
    }

    public int getMapToAxis() {
        return mapToAxis;
    }

    public void setMapToAxis(int m) {
        mapToAxis = m;
    }

    public void setMicrosteps(int microsteps) {
        this.microsteps = microsteps;
    }

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

    private boolean isPolarity() {
        return polarity;
    }

    public void setPolarity(boolean polarity) {
        this.polarity = polarity;
    }

    private void setPolarity(int polarity) {
        this.polarity = polarity != 0;
    }

    private boolean isPowerManagement() {
        return powerManagement;
    }

    public void setPowerManagement(boolean power_management) {
        this.powerManagement = power_management;
    }

    private void setPowerManagement(int power_management) {
        this.powerManagement = power_management != 0;
    }

    public float getStepAngle() {
        return stepAngle;
    }

    private void setStepAngle(float stepAngle) {
        this.stepAngle = stepAngle;
    }

    public float getTravelPerRevolution() {
        return travelPerRevolution;
    }

    private void setTravelPerRevolution(float travel_per_revolution) {
        this.travelPerRevolution = travel_per_revolution;
    }

    //This is the main method to parser a JSON Motor object
    public void applyJsonSystemSetting(JSONObject js, String parent) {
        Machine machine = TinygDriver.getInstance().getMachine();

        logger.info("Applying JSON Object to " + parent + " Group");
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
