package tgfx.ui.machinesettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import org.json.JSONObject;
import tgfx.MainController;
import tgfx.TgFXConstants;
import tgfx.tinyg.TinygDriver;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import org.json.JSONException;

import static tgfx.tinyg.Commands.CMD_APPLY_DEFAULT_SETTINGS;

/**
 * MachineSettingsController
 *
 * FXML Controller class
 *
 */
public class MachineSettingsController implements Initializable {
    private static final Logger logger = LogManager.getLogger();

    private final DecimalFormat decimalFormat = new DecimalFormat("#.###");

    @FXML
    private ListView<String> configsListView;
    @FXML
    private ChoiceBox machineSwitchType, machineUnitMode;
    @FXML
    private Button loadButton;
    @FXML
    private ProgressBar configProgress;

    public void updateGuiMachineSettings() {
        machineUnitMode.getSelectionModel().select(
                TinygDriver.getInstance().getMachine().getGcodeUnitModeAsInt());
        machineSwitchType.getSelectionModel().select(
                TinygDriver.getInstance().getMachine().getSwitchType());
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Initializing MachineSettingsController");
        populateConfigFiles();
    }

    private void populateConfigFiles() {
        // FIXME: god damned java file loading
        File folder = new File(TgFXConstants.PATH+ "/configs");
        if(!folder.exists()){
            logger.error("Error loading platform configs, path not found");
            return;
        }
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles==null) {
            // FIXME: this isn't the right error, but it's a low priority fix
            logger.error("Error loading platform configs, '" + folder.getName() + "' not found");
            return;
        }
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                String files = listOfFile.getName();
                if (files.endsWith(".config") || files.endsWith(".json")) {
                    configsListView.getItems().add(files);
                }
            }
        }
        logger.info("Loaded " + configsListView.getItems().size() + " platform files");
    }

    @FXML
    private void handleSaveCurrentSettings(ActionEvent event) {
        MainController.postConsoleMessage("Saving current of Config Files is unsupported at this time.");
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
                
//                FileChooser fc = new FileChooser();
//                fc.setInitialDirectory(new File(System.getProperty("user.dir") +
//                      System.getProperty("file.separator") + "configs" +
//                      System.getProperty("file.separator")));
//                fc.setTitle("Save Current TinyG Configuration");
//                File f = fc.showSaveDialog(null);
//                if (f.canWrite()) {
//                }
//            }
//        });
    }

    @FXML
    private void handleImportConfig(ActionEvent event) {
        MainController.postConsoleMessage("Importing of Config Files is unsupported at this time.");
    }


    private void writeConfigValue(JSONObject j) throws InterruptedException {
        String topLevelParent;
        topLevelParent = (String) j.names().get(0);
        Iterator it = j.getJSONObject(topLevelParent).keys();

        while (it.hasNext()) {
            String k = (String) it.next();
            Double value = j.getJSONObject(topLevelParent).getDouble(k);
            System.out.println("This is the value " + k + " " + decimalFormat.format(value));
            //value = Double.valueOf(decimalFormatjunctionDeviation.format(value));
            String singleJsonSetting = "{\"" + topLevelParent + k + "\":" + value + "}\n";
            TinygDriver.getInstance().write(singleJsonSetting);
            Thread.sleep(400);
        }
    }

    private int getElementCount(JSONObject j) throws JSONException {
        //We are getting a count of all the values we need to send from the config file.
        if (j.has("name")) { //We do not want the name of the config to count as stuff to write.
            return 0;
        } else {
            String topLevelParent = (String) j.names().get(0);
            return j.getJSONObject(topLevelParent).length();
        }
    }

    @FXML
    private void handleLoadConfig(ActionEvent event) throws FileNotFoundException {
        //This function gets the config file selected and applys the settings onto tinyg.
        InputStream fis, fis2;
        final BufferedReader br, br2;
        if(configsListView.getSelectionModel().isEmpty()){
            MainController.postConsoleMessage("Please select a valid config file");
            return;
        }
        // Why are we reading the file 2x?  It is to get the count of elements
        // we need to write.. then writing each line... so we just do it 2x.
        // FIXME: god damned java file loading
        File folder = new File(TgFXConstants.PATH+ "/configs");
        File selected_config = new File(folder.getPath()  + "/" +
                configsListView.getSelectionModel().getSelectedItem());

        fis = new FileInputStream(selected_config);
        fis2 = new FileInputStream(selected_config);

        br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
        br2 = new BufferedReader(new InputStreamReader(fis2, Charset.forName("UTF-8")));

        Task task;
        task = new Task<Void>() {
            @Override
            public Void call() throws IOException, InterruptedException {
            String line;
            int maxElements = 0;
            int currentElement = 0;
            String filename = "";

            while ((line = br2.readLine()) != null) {
                JSONObject j = new JSONObject(line);
                maxElements = maxElements + getElementCount(j);
            }

            while ((line = br.readLine()) != null) {
                if (TinygDriver.getInstance().isConnected().get()) {
                    if (line.startsWith("{\"name")) {
                        //This is the name of the CONFIG lets not write this to TinyG
                        filename = line.split(":")[1];
                        MainController.postConsoleMessage("Loading " + filename +
                                " config into TinyG... Please Wait...");
                    } else {

                        JSONObject j = new JSONObject(line);

                        String topLevelParent;
                        topLevelParent = (String) j.names().get(0);
                        Iterator it = j.getJSONObject(topLevelParent).keys();

                        while (it.hasNext()) {
                            String k = (String) it.next();
                            Double value = j.getJSONObject(topLevelParent).getDouble(k);
                            System.out.println("This is the value " + k + " " + decimalFormat.format(value));
                            MainController.postConsoleMessage("Applied: " + k + ":" + decimalFormat.format(value));
                            //value = Double.valueOf(decimalFormatjunctionDeviation.format(value));

                            String singleJsonSetting = "{\"" + topLevelParent + k + "\":" + value + "}\n";
                            TinygDriver.getInstance().write(singleJsonSetting);
                            updateProgress(currentElement, maxElements);
                            Thread.sleep(400); //Writing Values to eeprom can take a bit of time..
                            currentElement++;
                        }
                    }
                }
            }
            updateProgress(0, 0); //reset the progress bar
            MainController.postConsoleMessage("Finished Loading " + filename + ".");
            loadButton.setDisable(false);
            return null;
            }
        };
        
        if (TinygDriver.getInstance().isConnected().get()) {
            configProgress.progressProperty().bind(task.progressProperty());
            loadButton.setDisable(true);
            new Thread(task).start();
        }
    }

    @FXML
    private void handleApplyMachineSettings() {
        TinygDriver.getInstance().getCommandManager().
                applyMachineSwitchMode(machineSwitchType.getSelectionModel().getSelectedIndex());
        TinygDriver.getInstance().getCommandManager().
                applyMachineUnitMode(machineUnitMode.getSelectionModel().getSelectedIndex());
    }

    @FXML
    private void handleQueryMachineSettings() {
        TinygDriver.getInstance().getCommandManager().queryMachineSwitchMode();
        TinygDriver.getInstance().getCommandManager().queryAllMachineSettings();
}

    @FXML
    void handleApplyDefaultSettings(ActionEvent evt) {
        if (checkConectedMessage().equals("true")) {
            TinygDriver.getInstance().write(CMD_APPLY_DEFAULT_SETTINGS);
        } else {
            logger.error(checkConectedMessage());
            MainController.postConsoleMessage(checkConectedMessage());
        }
    }

    private String checkConectedMessage() {
        if (TinygDriver.getInstance().isConnected().get()) {
            return ("true");
        } else {
            return ("TinyG is Not Connected");
        }
    }
}
