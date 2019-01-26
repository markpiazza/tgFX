/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.system;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tgfx.tinyg.MnemonicManager;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

/**
 *
 * @author ril3y
 */
public class Motor {
    private static final Logger logger = LogManager.getLogger();
    
    private String CURRENT_MOTOR_JSON_OBJECT;
    private int id_number; //On TinyG the motor ports are 1-4
    private int ma;// map_to_axis
    private int mi; //Microsteps
    private float sa; //step angle
    private float tr; //travel revolution
    private boolean po; //polarity
    private boolean pm; //power management

    /**
     *
     * What TinyG Motor Class Looks Like. 2/1/2012 [1ma] m1_map_to_axis 0 [0=X,
     * 1=Y...] [1sa] m1_step_angle 1.800 deg [1tr] m1_travel_per_revolution
     * 5.080 mm [1mi] m1_microsteps 8 [1,2,4,8] [1po] m1_polarity 1 [0,1] [1pm]
     * m1_power_management 1 [0,1]
     *
     */
    public Motor(int id) {
        id_number = id;
    }

    public String getCURRENT_MOTOR_JSON_OBJECT() {
        return CURRENT_MOTOR_JSON_OBJECT;
    }

    public void setCURRENT_MOTOR_JSON_OBJECT(String CURRENT_MOTOR_JSON_OBJECT) {
        this.CURRENT_MOTOR_JSON_OBJECT = CURRENT_MOTOR_JSON_OBJECT;
    }

    //Small wrappers to return int's vs bools
    public int isPolarityInt() {
        return isPolarity() ? 1 : 0;
    }
    //Small wrappers to return int's vs bools

    public int isPowerManagementInt() {
        return isPower_management() ? 1 : 0;
    }

    public int getId_number() {
        return id_number;
    }

    public void setId_number(int id_number) {
        this.id_number = id_number;
    }

    public int getMapToAxis() {
        return ma;
    }

    public void setMapToAxis(int m) {
        ma = m;
    }

    public void setMicrosteps(int ms) {

        mi = ms;
    }

    public int getMicrosteps() {
        //This is really ugly looking but this is how it works with combo boxes or selection models.. ugh
        switch (mi) {
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
        return po;
    }

    public void setPolarity(boolean polarity) {
        this.po = polarity;
    }

    private void setPolarity(int polarity) {
        this.po = polarity != 0;
    }

    private boolean isPower_management() {
        return pm;
    }

    private void setPowerManagement(int power_management) {
        this.pm = power_management != 0;
    }

    public void setPowerManagement(boolean power_management) {
        this.pm = power_management;
    }

    public float getStepAngle() {
        return sa;
    }

    private void setStepAngle(float step_angle) {
        this.sa = step_angle;
    }

    public float getTravelPerRevolution() {
        return tr;
    }

    private void setTravelPerRevolution(float travel_per_revolution) {
        this.tr = travel_per_revolution;
    }

    //This is the main method to parser a JSON Motor object
    public void applyJsonSystemSetting(JSONObject js, String parent) {
        Machine machine = TinygDriver.getInstance().getMachine();

        logger.info("Applying JSON Object to " + parent + " Group");
        Iterator ii = js.keySet().iterator();
        while (ii.hasNext()) {
            String _key = ii.next().toString();
            String _val = js.get(_key).toString();
            ResponseCommand rc = new ResponseCommand(parent, _key, _val);

            switch (_key) {
                case (MnemonicManager.MNEMONIC_MOTOR_MAP_AXIS):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setMapToAxis(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case (MnemonicManager.MNEMONIC_MOTOR_MICROSTEPS):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setMicrosteps(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case (MnemonicManager.MNEMONIC_MOTOR_POLARITY):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setPolarity(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case (MnemonicManager.MNEMONIC_MOTOR_POWER_MANAGEMENT):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setPowerManagement(Integer.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case (MnemonicManager.MNEMONIC_MOTOR_STEP_ANGLE):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setStepAngle(Float.valueOf(rc.getSettingValue()));
                    logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                            rc.getSettingKey() + ":" + rc.getSettingValue());
                    break;

                case (MnemonicManager.MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION):
                    machine.getMotorByNumber(Integer.valueOf(rc.getSettingParent()))
                            .setTravelPerRevolution(Float.valueOf(rc.getSettingValue()));
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
