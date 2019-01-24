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
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tgfx.tinyg.TinygDriver;

/**
 *
 * @author ril3y
 */
public class HardwarePlatformManager {
    private static final Logger logger = Logger.getLogger(HardwarePlatformManager.class);
    private static HardwarePlatformManager hardwarePlatformManagerInstance;

    private ArrayList<HardwarePlatform> availablePlatforms = new ArrayList<>();
    private File folder;

    public HardwarePlatformManager() {
        URL path = getClass().getResource("/tgfx/hardwarePlatforms/");
        logger.info(path);
        folder = new File(path.getFile());
        this.loadPlatforConfigs();
    }

    //we are not using this until all platforms have the $hp element.
    public boolean setPlatformByName(String name) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getPlatformName().equals(name)) {
                TinygDriver.getInstance().machine.hardwarePlatform = platform;
                logger.info("Applied " + name + " hardware Profile to System");
                return true;
            }
        }
        return false;
    }

    public boolean setHardwarePlatformByVersionNumber(int verNumber) {
        for(HardwarePlatform platform : availablePlatforms){
            if (platform.getHardwarePlatformVersion() == verNumber) {
                TinygDriver.getInstance().machine.hardwarePlatform = platform;
                logger.info("Applied " + verNumber + " hardware platform id number to System");
                return true;

            }
        }
        return false;
    }

    private int loadPlatforConfigs() {
        return 0;
//        File[] listOfFiles = folder.listFiles();
//
//        for (int i = 0; i < listOfFiles.length; i++) {
//            if (listOfFiles[i].isFile()) {
//                File file = listOfFiles[i].getAbsoluteFile();
//                if (file.getName().endsWith(".json")) {
//                    try {
//                        Gson gson = new Gson();
//                        BufferedReader br = new BufferedReader(new FileReader(file));
//                        HardwarePlatform hp = gson.fromJson(br, HardwarePlatform.class);
//                        availablePlatforms.add(hp);
//                    } catch (FileNotFoundException | JsonIOException ex) {
//                        logger.error("Error loading hardware platforms: " + ex.getMessage());
//                    }catch (JsonSyntaxException ex){
//                        logger.error(ex.getMessage());
//                    }
//                }
//            }
//        }
//        logger.info("Loaded " + availablePlatforms.size() + " platform files");
//        return availablePlatforms.size();
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
