/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.utility;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.TgFXConstants;

/**
 *
 * @author ril3y
 */
public class UtilityFunctions {
    private static final Logger logger = LogManager.getLogger();

    final static ResourceBundle rb = ResourceBundle.getBundle("version");   //Used to track build date and build number
    
    public static String getOperatingSystem() {
        if (isWindows()) {
            return ("win");
        } else if (isMac()) {
            return ("mac");
        } else if (isUnix()) {
            return ("unix");
        } else if (isLinux()) {
            return ("linux");  //not tested yet 380.08
        } else {
            return ("win");
        }
    }

    private static boolean isLinux() {
        return TgFXConstants.OS.indexOf("lin") >= 0;
    }

    private static boolean isWindows() {
        return TgFXConstants.OS.indexOf("win") >= 0;
    }

    private static boolean isMac() {
        return TgFXConstants.OS.indexOf("mac") >= 0;
    }

    private static boolean isUnix() {
        return TgFXConstants.OS.indexOf("nux") >= 0;
    }

    public void testMessage(String message) {
        logger.info("Message Hit");
    }

    public static String getBuildInfo(String propToken) {
        String msg = "";
        try {
            msg = rb.getString(propToken);
        } catch (MissingResourceException e) {
            logger.error("Error Getting Build Info Token ".concat(propToken).concat(" not in Propertyfile!"));
        }
        return msg;
    }
}
