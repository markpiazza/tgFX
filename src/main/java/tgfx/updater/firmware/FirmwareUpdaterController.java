/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.updater.firmware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import jfxtras.labs.dialogs.MonologFX;
import jfxtras.labs.dialogs.MonologFXBuilder;
import jfxtras.labs.dialogs.MonologFXButton;
import jfxtras.labs.dialogs.MonologFXButtonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.Main;
import tgfx.tinyg.*;
import tgfx.utility.UtilityFunctions;

/**
 * FXML Controller class
 *
 * @author ril3y
 */
public class FirmwareUpdaterController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    @FXML
    private static Label firmwareVersion;

    @FXML
    private Label hwVersion, buildNumb, hardwareId, latestFirmwareBuild;

    @FXML
    private Label currentFirmwareVersionLabel;

    @FXML
    private static Button handleUpdateFirmware;

    private static String avrdudePath = "";
    private static String avrconfigPath = "";

    private SimpleDoubleProperty _currentVersionString = new SimpleDoubleProperty();

    static HashMap<String, String> platformSetup = new HashMap<>();

    private static Task updateFirmware() {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                File avc = new File("tools" + File.separator + "config" +
                        File.separator + "avrdude.conf");
                avrconfigPath = avc.getAbsolutePath();
                if (UtilityFunctions.getOperatingSystem().equals("mac")) {
                    File avd = new File("tools" + File.separator + "avrdude");
                    avrdudePath = avd.getAbsolutePath();
                } else {
                    File avd = new File("tools" + File.separator + "avrdude.exe");
                    avrdudePath = avd.getAbsolutePath();
                }

                logger.info("Trying to enter bootloader mode");
                Main.postConsoleMessage("Entering Bootloader mode.  " +
                        " tgFX will be un-responsive for then next 30 seconds.\n" +
                        "Your TinyG will start blinking rapidly while being programmed");

                enterBootloaderMode();

                //Download TinyG.hex
                URL url;
                try {
                    url = new URL(TinygDriver.getInstance().getMachine().getHardwarePlatform().getFirmwareUrl());
                    URLConnection urlConnection = url.openConnection();
                    logger.info("Opened Connection to Github");
                    Main.postConsoleMessage("Downloading tinyg.hex file from github.com");
                    InputStream input;
                    input = urlConnection.getInputStream();

                    try (OutputStream output = new FileOutputStream(new File("tinyg.hex"))) {
                        byte[] buffer = new byte[4096];
                        int n = -1;
                        while ((n = input.read(buffer)) != -1) {
                            if (n > 0) {
                                output.write(buffer, 0, n);
                            }
                        }
                        output.close();
                        Main.postConsoleMessage("Finished Downloading tinyg.hex");
                        logger.info("Finished Downloading tinyg.hex");
                    }
                } catch (MalformedURLException ex) {
                    logger.error(ex);
                    Main.postConsoleMessage("Error downloading the TinyG update from: " +
                            TinygDriver.getInstance().getMachine().getHardwarePlatform().getFirmwareUrl());
                    Main.postConsoleMessage("Check your internetion connection and try again. " +
                            "Firmware update aborted...");
                } catch (IOException ex) {
                    logger.error(ex);
                    Main.postConsoleMessage("Error updating your TinyG.  IOERROR");
                    return null;
                }

                Runtime rt = Runtime.getRuntime();

                try {
                    Main.postConsoleMessage("Updating TinyG Now... Please Wait");
                    Process process = rt.exec(avrdudePath +
                            " -p x192a3 -C " + avrconfigPath +
                            " -c avr109 -b 115200 -P " +
                            TinygDriver.getInstance().getPortName() +
                            " -U flash:w:tinyg.hex");
                    InputStream is = process.getInputStream();
                    Main.postConsoleMessage("Attempting to update TinyG's firmware.");
                    process.waitFor();
                    Thread.sleep(2000);//sleep a bit and let the firmware init
                    TinygDriver.getInstance().sendReconnectRequest();

                    Main.postConsoleMessage("Firmware update complete.");
                    toggleUpdateFirmwareButton(true);

                } catch (MalformedURLException ex) {
                    Main.postConsoleMessage("TinyG update URL: " +
                            TinygDriver.getInstance().getMachine().getHardwarePlatform().getFirmwareUrl() +
                            " is invalid, check the platform config "
                            + "file you are using in the configs directory.");
                    Main.postConsoleMessage("Firmware update aborted...");
                    return null;
                } catch (IOException | InterruptedException ex) {
                    logger.error(ex);
                }
                return null;
            }
        };
        return task;
    }

    private static void toggleUpdateFirmwareButton(boolean choice) {
        final boolean bChoice = choice;
        Platform.runLater(() -> {
            // when we are updating we dont want to hit it 2x
            handleUpdateFirmware.disableProperty().set(bChoice);
        });
    }

    /**
     * Initializes the controller class.
     */
    @FXML
    public static void handleUpdateFirmware(ActionEvent event) {

        if (TinygDriver.getInstance().getMachine().getHardwarePlatform().getHardwarePlatformVersion() == -1) {
            //This code checks to see if a hardware platform has been applied.
            //if the hpv is -1 then it has not.  So we guess that the board is a v8 TinyG.
            TinygDriver.getInstance().getHardwarePlatformManager().setPlatformByName("TinyG");
        }

        if (TinygDriver.getInstance().isTimedout() || TinygDriver.getInstance().getMachine().
                getHardwarePlatform().isIsUpgradeable()) {
            //This platform can be upgraded  
            
            toggleUpdateFirmwareButton(false);
            Task task = updateFirmware();
            new Thread(task).start();
            toggleUpdateFirmwareButton(true);
            
        } else {
            Main.postConsoleMessage("Sorry your TinyG platform cannot be auto upgraded at this time.  " +
                    "Please see the TinyG wiki for manual upgrade instructions.");
        }
    }

    @FXML
    private void checkFirmwareUpdate(ActionEvent event) {
        logger.info("Checking current Firmware Version");
        Platform.runLater(() -> {
            try {
                URL url = new URL(TinygDriver.getInstance().getMachine()
                        .getHardwarePlatform().getLatestVersionUrl());
                URLConnection urlConnection = url.openConnection();

                InputStream input;
                input = urlConnection.getInputStream();
                byte[] buffer = new byte[4096];
                logger.info("Checking end");
                input.read(buffer);
                String _currentVersionString = new String(buffer);
                latestFirmwareBuild.setText(_currentVersionString);
                Double currentVal;
                if (TinygDriver.getInstance().getMachine().getFirmwareBuildVersion() <
                        Double.parseDouble(_currentVersionString)) {
                    //We need to update your firmware
                    Platform.runLater(() -> {
                        Main.postConsoleMessage("TinyG Firmware Update Available.");

                            MonologFXButton btnYes = new MonologFXButton();
                            btnYes.setDefaultButton(true);
                            btnYes.setIcon("/testmonologfx/dialog_apply.png");
                            btnYes.setType(MonologFXButton.Type.YES);

                            MonologFXButton btnNo = new MonologFXButton();
                            btnNo.setCancelButton(true);
                            btnNo.setIcon("/testmonologfx/dialog_cancel.png");
                            btnNo.setType(MonologFXButton.Type.CANCEL);

                            MonologFX mono = new MonologFX();
                            mono.setTitleText("Firmware Update Available");
                            mono.setMessage("There is a firmware update available for your TinyG Hardware. \n"
                                            + "\n Click Yes to start your firmware update.");
                            mono.addButton(btnYes);
                            mono.addButton(btnNo);
                            mono.setType(MonologFX.Type.ERROR);

                            MonologFXButton.Type retval = mono.show();
                            switch (retval) {
                                case YES:
                                    Main.postConsoleMessage("This is going to take about 30 seconds.... " +
                                            "Please Wait... Watch the flashies....");
                                    handleUpdateFirmware(new ActionEvent());
                                    break;
                                case CANCEL:
                                    Main.postConsoleMessage("TinyG firmware update cancelled.");
                                    break;
                            }
                    });

                } else {
                    Main.postConsoleMessage("Your " + TinygDriver.getInstance()
                            .getMachine().getHardwarePlatform().getPlatformName() +
                            "'s firmware is up to date...\n");
                }

            } catch (IOException ex) {
                logger.error(ex);
            }
        });

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        NumberExpression ne = new SimpleDoubleProperty(_currentVersionString.doubleValue())
                .subtract(TinygDriver.getInstance().getMachine().getFirmwareBuild());

        //Bind the tinyg hardware id to the tg driver value
        hardwareId.textProperty().bind(TinygDriver.getInstance().getMachine().getHardwareId());

        //Bind the tinyg version  to the tg driver value
        // hwVersion.textProperty().bind(TinygDriver.getInstance().machine.hardwareVersion);

        //Bind the tinyg version  to the tg driver value
        hwVersion.textProperty().bind(TinygDriver.getInstance().getMachine().getHardwareVersion());

        // firmwareVersion.textProperty().bind(TinygDriver.getInstance().machine.firmwareVersion);
        buildNumb.textProperty().bind(TinygDriver.getInstance().getMachine().firmwareBuild.asString());

    }

    private static void enterBootloaderMode() {
        if (TinygDriver.getInstance().isConnected().get()) {
            //We need to disconnect from tinyg after issuing out boot command.
            try {
                //Set our board into bootloader mode.
                TinygDriver.getInstance().priorityWrite(CommandManager.CMD_APPLY_BOOTLOADER_MODE);
                Thread.sleep(1000);
            } catch (Exception ex) {
                logger.error(ex);
            }
            try {
                TinygDriver.getInstance().sendDisconnectRequest();
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }
}
