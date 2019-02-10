package tgfx;

/**
 * TgFXCONSTANTS
 *
 */
@SuppressWarnings("WeakerAccess") // these accessors _can_ be package-private, but I don't want them to be
public class TgFXConstants {
    public static final int STAGE_MIN_WIDTH = 1152;
    public static final int STAGE_MIN_HEIGHT = 648;

    // This is the amount of time in milliseconds that will
    // go until we say the connection has timed out.
    public static final int CONNECTION_TIMEOUT = 10000;

    public static final String CONNECTION_TIMEOUT_STRING = "{\"tgfx\": \"TinyG Connection Timeout\"}";
    public static final String FIRMWARE_UPDATE_URL = "https://github.com/synthetos/TinyG/wiki/TinyG-Boot-Loaderwiki-updating";

    public static final String OS = System.getProperty("os.name").toLowerCase();

    public static final String PROMPT = "tinyg>";

    public static final String STAGE_FXML_MAIN = "/tgfx/Main.fxml";
    public static final String STAGE_FXML_GCODETAB = "/tgfx/ui/gcode/GcodeTab.fxml";

    public static final String ROUTING_STATUS_REPORT = "STATUS_REPORT";
    public static final String ROUTING_CMD_GET_AXIS_SETTINGS = "CMD_GET_AXIS_SETTINGS"; 
    public static final String ROUTING_CMD_GET_MACHINE_SETTINGS = "CMD_GET_MACHINE_SETTINGS"; 
    public static final String ROUTING_CMD_GET_MOTOR_SETTINGS = "CMD_GET_MOTOR_SETTINGS"; 
    public static final String ROUTING_NETWORK_MESSAGE = "NETWORK_MESSAGE"; 
    public static final String ROUTING_MACHINE_UPDATE = "MACHINE_UPDATE"; 
    public static final String ROUTING_TEXTMODE_REPORT = "TEXTMODE_REPORT"; 
    public static final String ROUTING_BUFFER_UPDATE = "BUFFER_UPDATE"; 
    public static final String ROUTING_UPDATE_LINE_NUMBER = "UPDATE_LINE_NUMBER"; 
    public static final String ROUTING_BUILD_OK = "BUILD_OK"; 
    public static final String ROUTING_TINYG_USER_MESSAGE = "TINYG_USER_MESSAGE"; 
    public static final String ROUTING_TINYG_CONNECTION_TIMEOUT = "TINYG_CONNECTION_TIMEOUT";
    public static final String ROUTING_BUILD_ERROR = "BUILD_ERROR"; 
    public static final String ROUTING_DISCONNECT = "DISCONNECT"; 
    public static final String ROUTING_RECONNECT = "RECONNECT";

    public static final int SERIAL_DATA_RATE = 115200;

    // This disables the serial connection check so we can debug things
    // when there's no machine available.
    // TODO: need to write mock serial interface
    public static final boolean DISABLE_UI_CONNECTION_CHECK = true;

    public static final String PATH = "/Users/Mark/Desktop/Workspace/tgfx/src/main/resources";
}
