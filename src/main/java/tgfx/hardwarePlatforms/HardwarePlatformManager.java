package tgfx.hardwarePlatforms;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.TgFXConstants;
import tgfx.tinyg.TinygDriver;

/**
 * HardwarePlatformManager
 * manages the hardware platforms that are loaded from the file system
 */
public class HardwarePlatformManager {
    private static final Logger logger = LogManager.getLogger();

    private static HardwarePlatformManager hardwarePlatformManagerInstance;

    private ArrayList<HardwarePlatform> availablePlatforms = new ArrayList<>();

    /**
     * constructor, loads the platforms
     */
    private HardwarePlatformManager() {
        logger.info("Starting HardwarePlatformManager");
        this.loadPlatformConfigs();
    }


    /**
     * get hardware platform singleton
     * @return get a hardware platform manager singleton
     */
    public static HardwarePlatformManager getInstance() {
        if(hardwarePlatformManagerInstance == null){
            hardwarePlatformManagerInstance = new HardwarePlatformManager();
        }
        return hardwarePlatformManagerInstance;
    }


    /**
     * set a platform by name
     * @param name platform name
     */
    public void setPlatformByName(String name) {
        //we are not using this until all platforms have the $hp element.
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getPlatformName().equals(name)) {
                TinygDriver.getInstance().getMachine().setHardwarePlatform(platform);
                logger.info("Applied {} hardware Profile to System", name);
                return;
            }
        }
    }


    /**
     * set a platform by version
     * @param verNumber platform version
     */
    public void setHardwarePlatformByVersionNumber(int verNumber) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getHardwarePlatformVersion() == verNumber) {
                TinygDriver.getInstance().getMachine().setHardwarePlatform(platform);
                logger.info("Applied {} hardware platform id number to System", verNumber);
                return;
            }
        }
    }

    /**
     * load hardware platform configs
     */
    private void loadPlatformConfigs() {
        // FIXME: god damned java file loading
        String configPath = TgFXConstants.PATH+ "/hardwarePlatforms";
        File folder = new File(configPath);
        if(!folder.exists()){
            logger.error("Error loading hardware platforms, '{}' not found", configPath);
            return;
        }
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles==null) {
            logger.error("Error loading hardware platforms, '{}' not found", folder.getName());
            return;
        }
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                if (listOfFile.getName().endsWith(".json")) {
                    try {
                        Gson gson = new Gson();
                        BufferedReader br = new BufferedReader(new FileReader(listOfFile));
                        HardwarePlatform hp = gson.fromJson(br, HardwarePlatform.class);
                        availablePlatforms.add(hp);
                    } catch (FileNotFoundException | JsonSyntaxException | JsonIOException ex) {
                        logger.error("Error loading hardware platforms: {}", ex);
                    }
                }
            }
        }
        logger.info("Loaded " + availablePlatforms.size() + " platform files");
    }

    /**
     * update platform files
     */
    private void updatePlatformFiles() {
        // TODO: code in support for updating platform files from remote server
    }
}
