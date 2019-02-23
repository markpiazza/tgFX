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
    public static ToggleButton settingDebugBtn;

    @FXML
    private void handleTogglePreview(ActionEvent event) {
        if(settingDrawBtn.isSelected()){
            settingDrawBtn.setText("Enabled");
            setDrawPreview(true);
            
        }else{
            setDrawPreview(false);
            settingDrawBtn.setText("Disabled");
//        }
//        if (settingDrawBtn.getText().equals("ON")) {
//            settingDrawBtn.setText("OFF");
//            setDrawPreview(true);
//        } else {
//            settingDrawBtn.setText("ON");
//            setDrawPreview(false);
        }
    }

    public static boolean isDrawPreview() {
        return drawPreview;
    }

    private void setDrawPreview(boolean drawPreview) {
        TgfxSettingsController.drawPreview = drawPreview;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Initializing TgfxSettingsController");
        settingDrawBtn.setSelected(true);  //We set drawing preview to default
        settingDrawBtn.setText("Enabled");
        String buildNumber = Optional
                .ofNullable(UtilityFunctions.getBuildInfo("BUILD"))
                .orElse("Build Number Not Available");
        tgfxBuildNumber.setText(buildNumber);
        tgfxVersion.setText(".95");

        tgfxBuildDate.setId("lblMachine");
        tgfxBuildNumber.setId("lblMachine");
        tgfxVersion.setId("lblMachine");
    }
}
