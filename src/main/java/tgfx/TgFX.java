package tgfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * TgFX
 *
 */
public class TgFX extends Application {
    private static final Logger logger = LogManager.getLogger();


    /**
     *
     * @param stage
     * @throws IOException
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(TgFXConstants.STAGE_FXML_MAIN));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.setRoot(root);

        stage.setMinHeight(TgFXConstants.STAGE_MIN_HEIGHT);
        stage.setMinWidth(TgFXConstants.STAGE_MIN_WIDTH);
        stage.setScene(scene);
        stage.show();
    }


    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        logger.info("Starting up TgFX");
        Application.launch(TgFX.class, args);
    }
}
