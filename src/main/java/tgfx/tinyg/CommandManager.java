/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.tinyg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.Main;

import static tgfx.tinyg.Commands.*;

/**
 * CommandManager
 *
 */
public class CommandManager {
    private static final Logger logger = LogManager.getLogger();

    private static TinygDriver DRIVER = TinygDriver.getInstance();

    public CommandManager() {
    }

    public void stopTinyGMovement() {
        logger.info("Stopping Job Clearing Serial Queue...\n");
        DRIVER.priorityWrite(CMD_APPLY_PAUSE);
        DRIVER.getSerialWriter().clearQueueBuffer();
        DRIVER.priorityWrite(CMD_APPLY_QUEUE_FLUSH);
        Main.postConsoleMessage("Stopping Job Clearing Serial Queue...\n");
    }

    public void stopJogMovement() throws InterruptedException {
        //Do not mess with this order.
        DRIVER.getSerialWriter().clearQueueBuffer();
        DRIVER.priorityWrite(CMD_APPLY_PAUSE);
        Thread.sleep(40);
        DRIVER.priorityWrite(CMD_APPLY_QUEUE_FLUSH);
//        Main.postConsoleMessage("Stopping Job Clearing Serial Queue...\n");
    }

    public void setIncrementalMovementMode() {
        DRIVER.write(CMD_APPLY_INCREMENTAL_POSITION_MODE);
    }

    public void setAbsoluteMovementMode() {
        DRIVER.write(CMD_APPLY_ABSOLUTE_POSITION_MODE);
    }

    public void setMachinePosition(double x, double y) {
        DRIVER.write("{\"gc\":\"g28.3" + "X" + x + "Y" + y + "\"}\n");
    }

    /**
     *
     * Query All Motors for their current settings
     */
    public void queryAllMotorSettings() {
        try {
            DRIVER.write(CMD_QUERY_MOTOR_1_SETTINGS);
            logger.info("Getting Motor 1 Settings");
            Main.postConsoleMessage("Getting TinyG Motor 1 Settings...");

            DRIVER.write(CMD_QUERY_MOTOR_2_SETTINGS);
            logger.info("Getting Motor 2 Settings");
            Main.postConsoleMessage("Getting TinyG Motor 2 Settings...");

            DRIVER.write(CMD_QUERY_MOTOR_3_SETTINGS);
            logger.info("Getting Motor 3 Settings");
            Main.postConsoleMessage("Getting TinyG Motor 3 Settings...");

            DRIVER.write(CMD_QUERY_MOTOR_4_SETTINGS);
            logger.info("Getting Motor 4 Settings");
            Main.postConsoleMessage("Getting TinyG Motor 4 Settings...");

        } catch (Exception ex) {
            logger.error("Exception in queryAllMotorSettings()...");
            logger.error(ex.getMessage());
        }
    }

    public void inhibitAllAxis() throws InterruptedException {
        DRIVER.write(CMD_APPLY_INHIBIT_A_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_INHIBIT_X_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_INHIBIT_Y_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_INHIBIT_Z_AXIS);
        Thread.sleep(300);

    }

    public void enableAllAxis() throws InterruptedException {
        DRIVER.write(CMD_APPLY_ENABLE_A_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_ENABLE_X_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_ENABLE_Y_AXIS);
        Thread.sleep(300);
        DRIVER.write(CMD_APPLY_ENABLE_Z_AXIS);
        Thread.sleep(300);
    }

    public void queryStatusReport() {
        logger.info("Querying Status Report");
        DRIVER.write(CMD_QUERY_STATUS_REPORT);
        Main.postConsoleMessage("Getting TinyG Status Report...");
    }

    public void queryMachineSwitchMode() {
        DRIVER.write(CMD_QUERY_SWITCHMODE);
    }

    public void applyMachineSwitchMode(int i) {
        if (i == 0) {
            DRIVER.write(CMD_APPLY_SWITCHMODE_NORMALLY_OPEN);
        } else {
            DRIVER.write(CMD_APPLY_SWITCHMODE_NORMALLY_CLOSED);
        }
    }

    public void applyMachineUnitMode(int i) {
        if (i == 0) {
            DRIVER.write(CMD_APPLY_UNITMODE_INCHES);
        } else {
            DRIVER.write(CMD_APPLY_UNITMODE_MM);
        }
    }

    public void queryAllMachineSettings() {
        logger.info("Getting All Machine Settings");
        DRIVER.write(CMD_QUERY_SYSTEM_SETTINGS);
        Main.postConsoleMessage("Getting TinyG System Settings...");
    }

    /**
     * writes the commands to query current hardware settings on the tinyg board
     */
    public void queryAllHardwareAxisSettings() {
        logger.info("Getting A AXIS Settings");
        DRIVER.write(CMD_QUERY_AXIS_A);
        Main.postConsoleMessage("Getting TinyG Axis A Settings...");

        logger.info("Getting B AXIS Settings");
        DRIVER.write(CMD_QUERY_AXIS_B);
        Main.postConsoleMessage("Getting TinyG Axis B Settings...");

        logger.info("Getting C AXIS Settings");
        DRIVER.write(CMD_QUERY_AXIS_C);
        Main.postConsoleMessage("Getting TinyG Axis C Settings...");

        DRIVER.write(CMD_QUERY_AXIS_X);
        logger.info("Getting X AXIS Settings");
        Main.postConsoleMessage("Getting TinyG Axis X Settings...");

        DRIVER.write(CMD_QUERY_AXIS_Y);
        logger.info("Getting Y AXIS Settings");
        Main.postConsoleMessage("Getting TinyG Axis Y Settings...");

        DRIVER.write(CMD_QUERY_AXIS_Z);
        logger.info("Getting Z AXIS Settings");
        Main.postConsoleMessage("Getting TinyG Axis Z Settings...");
    }
}