package tgfx.ui.tgfxsettings;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.utility.UtilityFunctions;

/**
 * TgfxSettingsController

 * FXML Controller class
 *
 */
public class TgfxSettingsController implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private static boolean drawPreview = true;

    @FXML
    private Label tgfxBuildNumber, tgfxBuildDate, tgfxVersion;
    
    @FXML
    private ToggleButton settingDrawBtn;
    
    @FXML
    private ToggleButton settingDebugBtn;

    @FXML
    private void handleTogglePreview(ActionEvent event) {
        if(settingDrawBtn.isSelected()){
            settingDrawBtn.setText("Enabled");
            setDrawPreview(true);
            
        }else{
            setDrawPreview(false);
            settingDrawBtn.setText("Disabled");
        }
    }

    public static boolean isDrawPreview() {
        return drawPreview;
    }

    private static void setDrawPreview(boolean drawPreview) {
        TgfxSettingsController.drawPreview = drawPreview;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Initializing TgfxSettingsController");

        String buildNumber = Optional
                .ofNullable(UtilityFunctions.getBuildInfo("BUILD"))
                .orElse("Build Number Not Available");

        String buildDate = Optional
                .ofNullable(UtilityFunctions.getBuildInfo("DATE"))
                .orElse("Build Date Not Available");

        settingDrawBtn.setSelected(true);  //We set drawing preview to default
        settingDrawBtn.setText("Enabled");

        tgfxBuildDate.setId("lblMachine");
        tgfxBuildDate.setText(buildDate);

        tgfxBuildNumber.setId("lblMachine");
        tgfxBuildNumber.setText(buildNumber);

        tgfxVersion.setId("lblMachine");
        tgfxVersion.setText(".95");
    }
}
