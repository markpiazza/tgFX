package tgfx.ui.firmware;

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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import jfxtras.labs.dialogs.MonologFX;
import jfxtras.labs.dialogs.MonologFXButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.system.Machine;
import tgfx.tinyg.*;
import tgfx.utility.UtilityFunctions;

import static tgfx.MainController.*;
import static tgfx.tinyg.CommandConstants.CMD_APPLY_BOOTLOADER_MODE;

/**
 * FirmwareUpdaterController
 *
 * FXML Controller class

 */
public class FirmwareUpdaterController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    private TinygDriver driver;
    private Machine machine;

    @FXML
    private Label hwVersion, buildNumb, hardwareId, latestFirmwareBuild;

    @FXML
    private Label firmwareVersion;

    @FXML
    private Label currentFirmwareVersionLabel;

    @FXML
    private Button handleUpdateFirmware;

    private String avrdudePath = "";
    private String avrconfigPath = "";

    private SimpleDoubleProperty currentVersionString;

    private HashMap<String, String> platformSetup;

    public FirmwareUpdaterController(){
        driver = TinygDriver.getInstance();
        machine = driver.getMachine();
        currentVersionString = new SimpleDoubleProperty();
        platformSetup = new HashMap<>();
    }

    private Task updateFirmware() {
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
                postConsoleMessage("Entering Bootloader mode.  " +
                        " tgFX will be un-responsive for then next 30 seconds.\n" +
                        "Your TinyG will start blinking rapidly while being programmed");

                enterBootloaderMode();

                //Download TinyG.hex
                URL url;
                try {
                    url = new URL(machine.getHardwarePlatform().getFirmwareUrl());
                    URLConnection urlConnection = url.openConnection();
                    logger.info("Opened Connection to Github");
                    postConsoleMessage("Downloading tinyg.hex file from github.com");
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
                        postConsoleMessage("Finished Downloading tinyg.hex");
                        logger.info("Finished Downloading tinyg.hex");
                    }
                } catch (MalformedURLException ex) {
                    logger.error(ex);
                    postConsoleMessage("Error downloading the TinyG update from: " +
                            machine.getHardwarePlatform().getFirmwareUrl());
                    postConsoleMessage("Check your internetion connection and try again. " +
                            "Firmware update aborted...");
                } catch (IOException ex) {
                    logger.error(ex);
                    postConsoleMessage("Error updating your TinyG.  IOERROR");
                    return null;
                }

                Runtime rt = Runtime.getRuntime();

                try {
                    postConsoleMessage("Updating TinyG Now... Please Wait");
                    Process process = rt.exec(avrdudePath +
                            " -p x192a3 -C " + avrconfigPath +
                            " -c avr109 -b 115200 -P " +
                            driver.getPortName() +
                            " -U flash:w:tinyg.hex");
                    InputStream is = process.getInputStream();
                    postConsoleMessage("Attempting to update TinyG's firmware.");
                    process.waitFor();
                    Thread.sleep(2000);//sleep a bit and let the firmware init
                    driver.sendReconnectRequest();

                    postConsoleMessage("Firmware update complete.");
                    toggleUpdateFirmwareButton(true);

                } catch (MalformedURLException ex) {
                    postConsoleMessage("TinyG update URL: " +
                            machine.getHardwarePlatform().getFirmwareUrl() +
                            " is invalid, check the platform config "
                            + "file you are using in the configs directory.");
                    postConsoleMessage("Firmware update aborted...");
                    return null;
                } catch (IOException | InterruptedException ex) {
                    logger.error(ex);
                }
                return null;
            }
        };
        return task;
    }

    private void toggleUpdateFirmwareButton(boolean choice) {
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
    public void handleUpdateFirmware(ActionEvent event) {

        if (machine.getHardwarePlatform().getHardwarePlatformVersion() == -1) {
            //This code checks to see if a hardware platform has been applied.
            //if the hpv is -1 then it has not.  So we guess that the board is a v8 TinyG.
            driver.getHardwarePlatformManager().setPlatformByName("TinyG");
        }

        if (driver.isTimedout() || machine.getHardwarePlatform().isIsUpgradeable()) {
            //This platform can be upgraded  
            
            toggleUpdateFirmwareButton(false);
            Task task = updateFirmware();
            new Thread(task).start();
            toggleUpdateFirmwareButton(true);
            
        } else {
            postConsoleMessage("Sorry your TinyG platform cannot be auto upgraded at this time.  " +
                    "Please see the TinyG wiki for manual upgrade instructions.");
        }
    }

    @FXML
    private void checkFirmwareUpdate(ActionEvent event) {
        logger.info("Checking current Firmware Version");
        Platform.runLater(() -> {
            try {
                URL url = new URL(machine.getHardwarePlatform().getLatestVersionUrl());
                URLConnection urlConnection = url.openConnection();

                InputStream input;
                input = urlConnection.getInputStream();
                byte[] buffer = new byte[4096];
                logger.info("Checking end");
                input.read(buffer);
                String _currentVersionString = new String(buffer);
                latestFirmwareBuild.setText(_currentVersionString);
                Double currentVal;
                if (machine.getFirmwareBuild() <
                        Double.parseDouble(_currentVersionString)) {
                    //We need to update your firmware
                    Platform.runLater(() -> {
                        postConsoleMessage("TinyG Firmware Update Available.");

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
                                postConsoleMessage("This is going to take about 30 seconds.... " +
                                        "Please Wait... Watch the flashies....");
                                handleUpdateFirmware(new ActionEvent());
                                break;
                            case CANCEL:
                                postConsoleMessage("TinyG firmware update cancelled.");
                                break;
                        }
                    });

                } else {
                    postConsoleMessage("Your " + machine.getHardwarePlatform().getPlatformName() +
                            "'s firmware is up to date...\n");
                }

            } catch (IOException ex) {
                logger.error(ex);
            }
        });

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hardwareId.textProperty().bind(machine.hardwareIdProperty());
        hwVersion.textProperty().bind(machine.hardwareVersionProperty());
        firmwareVersion.textProperty().bind(machine.firmwareVersionProperty());
        buildNumb.textProperty().bind(machine.firmwareBuildProperty().asString());

    }

    private void enterBootloaderMode() {
        if (driver.isConnected().get()) {
            //We need to disconnect from tinyg after issuing out boot command.
            try {
                //Set our board into bootloader mode.
                driver.priorityWrite(CMD_APPLY_BOOTLOADER_MODE);
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
            try {
                driver.sendDisconnectRequest();
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }
}
