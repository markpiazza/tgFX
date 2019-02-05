/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.hardwarePlatforms;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;

/**
 * HardwarePlatformManager
 *
 */
public class HardwarePlatformManager {
    private static final Logger logger = LogManager.getLogger();

    private static HardwarePlatformManager hardwarePlatformManagerInstance;

    private ArrayList<HardwarePlatform> availablePlatforms = new ArrayList<>();

    public HardwarePlatformManager() {
        logger.info("Starting HardwarePlatformManager");
        this.loadPlatformConfigs();
    }

    public static HardwarePlatformManager getInstance() {
        if(hardwarePlatformManagerInstance == null){
            hardwarePlatformManagerInstance = new HardwarePlatformManager();
        }
        return hardwarePlatformManagerInstance;
    }

    //we are not using this until all platforms have the $hp element.
    public void setPlatformByName(String name) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getPlatformName().equals(name)) {
                TinygDriver.getInstance().getMachine().setHardwarePlatform(platform);
                logger.info("Applied {} hardware Profile to System", name);
                return;
            }
        }
    }

    public void setHardwarePlatformByVersionNumber(int verNumber) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getHardwarePlatformVersion() == verNumber) {
                TinygDriver.getInstance().getMachine().setHardwarePlatform(platform);
                logger.info("Applied {} hardware platform id number to System", verNumber);
                return;
            }
        }
    }

    private void loadPlatformConfigs() {
        // FIXME: god damned java file loading
        File folder = null;
        try {
            folder = new File(HardwarePlatformManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()+"/hardwarePlatforms");
        } catch (URISyntaxException e) {
            logger.error(e);
        }
        if(folder==null){
            logger.error("Error loading platform configs, path not found");
            return;
        }
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles==null) {
            // FIXME: this isn't the right error, but it's a low priority fix
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
                        logger.error("Error loading hardware platforms: {}", ex.getMessage());
                    }
                }
            }
        }
        logger.info("Loaded " + availablePlatforms.size() + " platform files");
    }

    private void updatePlatformFiles() {
        // TODO: code in support for updating platform files from remote server
    }
}
