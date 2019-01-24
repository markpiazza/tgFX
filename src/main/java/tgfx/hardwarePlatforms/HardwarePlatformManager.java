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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tgfx.Main;
import tgfx.tinyg.TinygDriver;

/**
 *
 * @author ril3y
 */
public class HardwarePlatformManager {
    private static final Logger logger = Logger.getLogger(HardwarePlatformManager.class);
    private static HardwarePlatformManager hardwarePlatformManagerInstance;

    private ArrayList<HardwarePlatform> availablePlatforms = new ArrayList<>();

    public HardwarePlatformManager() {
        this.loadPlatformConfigs();
    }

    //we are not using this until all platforms have the $hp element.
    public void setPlatformByName(String name) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getPlatformName().equals(name)) {
                TinygDriver.getInstance().machine.hardwarePlatform = platform;
                logger.info("Applied " + name + " hardware Profile to System");
                return;
            }
        }
    }

    public void setHardwarePlatformByVersionNumber(int verNumber) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getHardwarePlatformVersion() == verNumber) {
                TinygDriver.getInstance().machine.hardwarePlatform = platform;
                logger.info("Applied " + verNumber + " hardware platform id number to System");
                return;
            }
        }
    }

    private void loadPlatformConfigs() {
        File folder = new File("resources/hardwarePlatforms/");
        File[] listOfFiles = folder.listFiles();
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                if (listOfFile.getName().endsWith(".json")) {
                    try {
                        Gson gson = new Gson();
                        BufferedReader br = new BufferedReader(new FileReader(listOfFile));
                        HardwarePlatform hp = gson.fromJson(br, HardwarePlatform.class);
                        availablePlatforms.add(hp);
                    } catch (FileNotFoundException | JsonIOException ex) {
                        logger.error("Error loading hardware platforms: " + ex.getMessage());
                    }catch (JsonSyntaxException ex){
                        logger.error(ex.getMessage());
                    }
                }
            }
        }
        logger.info("Loaded " + availablePlatforms.size() + " platform files");
        availablePlatforms.size();
    }

    public static HardwarePlatformManager getInstance() {
        if(hardwarePlatformManagerInstance==null){
            hardwarePlatformManagerInstance = new HardwarePlatformManager();
        }
        return hardwarePlatformManagerInstance;
    }

    private void updatePlatformFiles() {
        //todo code in support for updating platform files from remote server
        throw new NotImplementedException();
    }
}
