package tgfx.utility;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.TgFXConstants;

/**
 * UtilityFunctions
 *
 */
public class UtilityFunctions {
    private static final Logger logger = LogManager.getLogger();

    // Used to track build date and build number
    private final static ResourceBundle rb = ResourceBundle.getBundle("version");
    
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
        return TgFXConstants.OS.contains("lin");
    }

    private static boolean isWindows() {
        return TgFXConstants.OS.contains("win");
    }

    private static boolean isMac() {
        return TgFXConstants.OS.contains("mac");
    }

    private static boolean isUnix() {
        return TgFXConstants.OS.contains("nux");
    }

    public static String getBuildInfo(String propToken) {
        String msg = "";
        try {
            msg = rb.getString(propToken);
        } catch (MissingResourceException e) {
            logger.error("Error Getting Build Info Token ".concat(propToken)
                    .concat(" not in Propertyfile!"));
        }
        return msg;
    }
}
