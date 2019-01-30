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
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.tinyg.TinygDriver;

/**
 * @author ril3y
 */
public class HardwarePlatformManager {
    private static final Logger logger = LogManager.getLogger();

    private static HardwarePlatformManager hardwarePlatformManagerInstance;

    private ArrayList<HardwarePlatform> availablePlatforms = new ArrayList<>();

    public HardwarePlatformManager() {
        this.loadPlatformConfigs();
    }

    public static HardwarePlatformManager getInstance() {
        if(hardwarePlatformManagerInstance==null){
            hardwarePlatformManagerInstance = new HardwarePlatformManager();
        }
        return hardwarePlatformManagerInstance;
    }

    //we are not using this until all platforms have the $hp element.
    public void setPlatformByName(String name) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getPlatformName().equals(name)) {
                TinygDriver.getInstance().getMachine().hardwarePlatform = platform;
                logger.info("Applied " + name + " hardware Profile to System");
                return;
            }
        }
    }

    public void setHardwarePlatformByVersionNumber(int verNumber) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getHardwarePlatformVersion() == verNumber) {
                TinygDriver.getInstance().getMachine().hardwarePlatform = platform;
                logger.info("Applied " + verNumber + " hardware platform id number to System");
                return;
            }
        }
    }

    private void loadPlatformConfigs() {
        File file = new File("src/main/resources");
        System.err.println("hardware config: "+file.getPath());
        for(File file1 : file.listFiles() ){
            System.err.println(" - "+file1.getName());
        }

        // FIXME: THis needs to not be a absolute/relative path (should be resource path)
        File folder = new File("src/main/resources/hardwarePlatforms");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles==null) {
            logger.warn("Error loading hardware platforms, '"+folder.getName()+"' not found");
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

    private void updatePlatformFiles() {
        //todo code in support for updating platform files from remote server
    }
}
