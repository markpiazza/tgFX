package tgfx.tinyg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.MainController;

import static tgfx.tinyg.CommandConstants.*;

/**
 * CommandManager
 * Processes commands
 */
public class CommandManager {
    private static final Logger logger = LogManager.getLogger();

    private TinygDriver driver;

    public CommandManager(TinygDriver driver) {
        this.driver = driver;
    }

    public void stopTinyGMovement() {
        logger.info("Stopping Job Clearing Serial Queue...\n");
        driver.priorityWrite(CMD_APPLY_PAUSE);
        driver.getSerialWriter().clearQueueBuffer();
        driver.priorityWrite(CMD_APPLY_QUEUE_FLUSH);
        MainController.postConsoleMessage("Stopping Job Clearing Serial Queue...\n");
    }

    public void stopJogMovement() throws InterruptedException {
        //Do not mess with this order.
        driver.getSerialWriter().clearQueueBuffer();
        driver.priorityWrite(CMD_APPLY_PAUSE);
        Thread.sleep(40);
        driver.priorityWrite(CMD_APPLY_QUEUE_FLUSH);
    }

    public void setIncrementalMovementMode() {
        driver.write(CMD_APPLY_INCREMENTAL_POSITION_MODE);
    }

    public void setAbsoluteMovementMode() {
        driver.write(CMD_APPLY_ABSOLUTE_POSITION_MODE);
    }

    public void setMachinePosition(double x, double y) {
        driver.write("{\"gc\":\"g28.3" + "X" + x + "Y" + y + "\"}\n");
    }

    /**
     *
     * Query All Motors for their current settings
     */
    public void queryAllMotorSettings() {
        driver.write(CMD_QUERY_MOTOR_1_SETTINGS);
        logger.info("Getting Motor 1 Settings");
        MainController.postConsoleMessage("Getting TinyG Motor 1 Settings...");

        driver.write(CMD_QUERY_MOTOR_2_SETTINGS);
        logger.info("Getting Motor 2 Settings");
        MainController.postConsoleMessage("Getting TinyG Motor 2 Settings...");

        driver.write(CMD_QUERY_MOTOR_3_SETTINGS);
        logger.info("Getting Motor 3 Settings");
        MainController.postConsoleMessage("Getting TinyG Motor 3 Settings...");

        driver.write(CMD_QUERY_MOTOR_4_SETTINGS);
        logger.info("Getting Motor 4 Settings");
        MainController.postConsoleMessage("Getting TinyG Motor 4 Settings...");
    }

    public void inhibitAllAxis() throws InterruptedException {
        driver.write(CMD_APPLY_INHIBIT_A_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_INHIBIT_X_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_INHIBIT_Y_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_INHIBIT_Z_AXIS);
        Thread.sleep(300);

    }

    public void enableAllAxis() throws InterruptedException {
        driver.write(CMD_APPLY_ENABLE_A_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_ENABLE_X_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_ENABLE_Y_AXIS);
        Thread.sleep(300);
        driver.write(CMD_APPLY_ENABLE_Z_AXIS);
        Thread.sleep(300);
    }

    public void queryStatusReport() {
        logger.info("Querying Status Report");
        driver.write(CMD_QUERY_STATUS_REPORT);
        MainController.postConsoleMessage("Getting TinyG Status Report...");
    }

    public void queryMachineSwitchMode() {
        driver.write(CMD_QUERY_SWITCHMODE);
    }

    public void applyMachineSwitchMode(int i) {
        if (i == 0) {
            driver.write(CMD_APPLY_SWITCHMODE_NORMALLY_OPEN);
        } else {
            driver.write(CMD_APPLY_SWITCHMODE_NORMALLY_CLOSED);
        }
    }

    public void applyMachineUnitMode(int i) {
        if (i == 0) {
            driver.write(CMD_APPLY_UNITMODE_INCHES);
        } else {
            driver.write(CMD_APPLY_UNITMODE_MM);
        }
    }

    public void queryAllMachineSettings() {
        logger.info("Getting All Machine Settings");
        driver.write(CMD_QUERY_SYSTEM_SETTINGS);
        MainController.postConsoleMessage("Getting TinyG System Settings...");
    }

    /**
     * writes the commands to query current hardware settings on the tinyg board
     */
    public void queryAllHardwareAxisSettings() {
        driver.write(CMD_QUERY_AXIS_X);
        logger.info("Getting X AXIS Settings");
        MainController.postConsoleMessage("Getting TinyG Axis X Settings...");

        driver.write(CMD_QUERY_AXIS_Y);
        logger.info("Getting Y AXIS Settings");
        MainController.postConsoleMessage("Getting TinyG Axis Y Settings...");

        driver.write(CMD_QUERY_AXIS_Z);
        logger.info("Getting Z AXIS Settings");
        MainController.postConsoleMessage("Getting TinyG Axis Z Settings...");

        logger.info("Getting A AXIS Settings");
        driver.write(CMD_QUERY_AXIS_A);
        MainController.postConsoleMessage("Getting TinyG Axis A Settings...");

        logger.info("Getting B AXIS Settings");
        driver.write(CMD_QUERY_AXIS_B);
        MainController.postConsoleMessage("Getting TinyG Axis B Settings...");

        logger.info("Getting C AXIS Settings");
        driver.write(CMD_QUERY_AXIS_C);
        MainController.postConsoleMessage("Getting TinyG Axis C Settings...");

    }
}