/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;


/**
 *
 * @author ril3y
 */
public class TgFX extends Application {

    private static final Logger logger = Logger.getLogger(TgFX.class);

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(TgFXConstants.STAGE_FXML_MAIN));
        Scene scene = new Scene(root);
        scene.setRoot(root);

        FXMLLoader fxmlLoader = new FXMLLoader();
        TgFX TgFXController = fxmlLoader.getController();

        stage.setMinHeight(TgFXConstants.STAGE_MIN_HEIGHT);
        stage.setMinWidth(TgFXConstants.STAGE_MIN_WIDTH);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        logger.info("Starting up TgFX");
        Application.launch(TgFX.class, args);
    }
}