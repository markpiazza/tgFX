package tgfx.tinyg;

import static tgfx.tinyg.Mnemonics.*;

@SuppressWarnings({"WeakerAccess","unused"})
public class Commands {

    public static final String CMD_QUERY_COORDINATE_SYSTEM = "{\"coor\":\"\"}\n";
    public static final String CMD_QUERY_HARDWARE_BUILD_NUMBER = "{\"fb\":\"\"}\n";
    public static final String CMD_QUERY_HARDWARE_FIRMWARE_NUMBER = "{\"fv\":\"\"}\n";
    public static final String CMD_QUERY_HARDWARE_PLATFORM = "{\"hp\":null}\n";
    public static final String CMD_QUERY_OK_PROMPT = "{\"gc\":\"?\"}\n";
    //    public static final String CMD_APPLY_STATUS_REPORT_FORMAT = "{\"sr\":{\"line\":t,\"vel\":t,\"mpox\":t,\"mpoy\":t, \"mpoz\":t,\"mpoa\":t,\"coor\":t, \"ofsa\":t,\"ofsx\":t,\"ofsy\":t,\"ofsz\":t,\"unit\":t,\"stat\":t,\"homz\":t,\"homy\":t,\"homx\":t,\"momo\":t}}\n";
    public static final String CMD_APPLY_STATUS_REPORT_FORMAT = "{\"sr\":{\"line\":t,\"vel\":t,\"mpox\":t,\"mpoy\":t, \"mpoz\":t,\"mpoa\":t,\"coor\":t, \"ofsa\":t,\"ofsx\":t,\"ofsy\":t,\"ofsz\":t,\"dist\":t,\"unit\":t,\"stat\":t,\"homz\":t,\"homy\":t,\"homx\":t,\"momo\":t}}\n";
    public static final String CMD_QUERY_STATUS_REPORT = "{\"sr\":\"\"}\n";
    public static final String CMD_QUERY_HARDWARE_ID = "{\"id\":null}\n";
    public static final String CMD_QUERY_HARDWARE_VERSION = "{\"hv\":null}\n";
    public static final String CMD_QUERY_AXIS_X = "{\"x\":null}\n";
    public static final String CMD_QUERY_AXIS_Y = "{\"y\":null}\n";
    public static final String CMD_QUERY_AXIS_Z = "{\"z\":null}\n";
    public static final String CMD_QUERY_AXIS_A = "{\"a\":null}\n";
    public static final String CMD_QUERY_AXIS_B = "{\"b\":null}\n";
    public static final String CMD_QUERY_AXIS_C = "{\"c\":null}\n";
    public static final String CMD_QUERY_MOTOR_1_SETTINGS = "{\"1\":null}\n";
    public static final String CMD_QUERY_MOTOR_2_SETTINGS = "{\"2\":null}\n";
    public static final String CMD_QUERY_MOTOR_3_SETTINGS = "{\"3\":null}\n";
    public static final String CMD_QUERY_MOTOR_4_SETTINGS = "{\"4\":null}\n";
    public static final String CMD_QUERY_SYSTEM_SETTINGS = "{\"sys\":null}\n";
    public static final String CMD_APPLY_SYSTEM_ZERO_ALL_AXES = "{\"gc\":\"g92x0y0z0a0\"}\n";
    public static final String CMD_APPLY_SYSTEM_HOME_XYZ_AXES = "{\"gc\":\"g28.2x0y0z0\"}\n";
    public static final String CMD_APPLY_SYSTEM_GCODE_UNITS_INCHES = "{\"" + MNEMONIC_STATUS_REPORT_UNIT + "\":0}\n"; //0=inches
    public static final String CMD_APPLY_SYSTEM_GCODE_UNITS_MM = "{\"" + MNEMONIC_STATUS_REPORT_UNIT + "\":1}\n"; //1=mm
    public static final String CMD_APPLY_SYSTEM_DISABLE_LOCAL_ECHO = "{\"" + MNEMONIC_SYSTEM_ENABLE_ECHO + "\":0}\n";
    public static final String CMD_APPLY_SYSTEM_ENABLE_LOCAL_ECHO = "{\"" + MNEMONIC_SYSTEM_ENABLE_ECHO + "\":0}\n";
    public static final String CMD_APPLY_SYSTEM_MNEMONIC_SYSTEM_SWITCH_TYPE_NC = "{\"" + MNEMONIC_SYSTEM_SWITCH_TYPE + "\":1}\n";
    public static final String CMD_QUERY_SYSTEM_GCODE_UNIT_MODE = "{\"" + MNEMONIC_STATUS_REPORT_UNIT + "\":null}\n";
    public static final String CMD_QUERY_SYSTEM_GCODE_PLANE = "{\"" + MNEMONIC_SYSTEM_DEFAULT_GCODE_PLANE + "\":null}\n";
    public static final String CMD_APPLY_DISABLE_HASHCODE = "{\"eh\":0\"}\n";
    public static final String CMD_APPLY_DEFAULT_SETTINGS = "{\"defa\":1}\n";
    public static final String CMD_APPLY_STATUS_UPDATE_INTERVAL = "{\"si\":100}\n";
    public static final String CMD_APPLY_JSON_VERBOSITY = "{\"jv\":3}\n";
    public static final String CMD_APPLY_ENABLE_JSON_MODE = "{\"ej\":1}\n";
    public static final String CMD_DEFAULT_ENABLE_JSON = "{\"ej\":1}\n";
    public static final String CMD_APPLY_TEXT_VERBOSITY = "{\"tv\":0}\n";
    public static final String CMD_APPLY_NOOP = "{}\n";
    public static final String CMD_QUERY_SWITCHMODE = "{\"st\":null}\n";
    public static final String CMD_APPLY_SWITCHMODE_NORMALLY_OPEN = "{\"st\":0}\n";
    public static final String CMD_APPLY_SWITCHMODE_NORMALLY_CLOSED = "{\"st\":1}\n";
    public static final String CMD_APPLY_UNITMODE_INCHES = "{\"gc\":\"g20\"}\n";
    public static final String CMD_APPLY_UNITMODE_MM = "{\"gc\":\"g21\"}\n";
    public static final String CMD_APPLY_PAUSE = "!\n";
    public static final String CMD_APPLY_RESUME = "~\n";
    public static final String CMD_APPLY_QUEUE_FLUSH = "%\n";
    //Homeing Commandings
    public static final String CMD_APPLY_HOME_X_AXIS = "{\"gc\":\"g28.2x0\"}\n";
    public static final String CMD_APPLY_HOME_Y_AXIS = "{\"gc\":\"g28.2y0\"}\n";
    public static final String CMD_APPLY_HOME_Z_AXIS = "{\"gc\":\"g28.2z0\"}\n";
    public static final String CMD_APPLY_HOME_A_AXIS = "{\"gc\":\"g28.2a0\"}\n";
    //ZERO Commands
    public static final String CMD_APPLY_ZERO_X_AXIS = "{\"gc\":\"g92x0\"}\n";
    public static final String CMD_APPLY_ZERO_Y_AXIS = "{\"gc\":\"g92y0\"}\n";
    public static final String CMD_APPLY_ZERO_Z_AXIS = "{\"gc\":\"g92z0\"}\n";
    public static final String CMD_APPLY_ZERO_A_AXIS = "{\"gc\":\"g92a0\"}\n";
    //    public static final String CMD_APPLY_INHIBIT_ALL_AXIS = "{\"xam\":2, \"yam\":2, \"zam\":2, \"aam\":2}\n";
    public static final String CMD_APPLY_INHIBIT_X_AXIS = "{\"xam\":2}\n";
    public static final String CMD_APPLY_INHIBIT_Y_AXIS = "{\"yam\":2}\n";
    public static final String CMD_APPLY_INHIBIT_Z_AXIS = "{\"zam\":2}\n";
    public static final String CMD_APPLY_INHIBIT_A_AXIS = "{\"aam\":2}\n";
    public static final String CMD_APPLY_ENABLE_X_AXIS = "{\"xam\":1}\n";
    public static final String CMD_APPLY_ENABLE_Y_AXIS = "{\"yam\":1}\n";
    public static final String CMD_APPLY_ENABLE_Z_AXIS = "{\"zam\":1}\n";
    public static final String CMD_APPLY_ENABLE_A_AXIS = "{\"aam\":1}\n";
    public static final String CMD_APPLY_INCREMENTAL_POSITION_MODE = "{\"gc\":\"g91\"}\n";
    public static final String CMD_APPLY_ABSOLUTE_POSITION_MODE = "{\"gc\":\"g90\"}\n";
    //    public static final String CMD_APPLY_ENABLE_ALL_AXIS = "{\"xam\":1, \"yam\":1, \"zam\":1, \"aam\":1}\n";
    public static final String CMD_QUERY_SYSTEM_SERIAL_BUFFER_LENGTH = "{\"rx\":null}\n";
    public static final Byte CMD_APPLY_RESET = 0x18;
    //    public static final String CMD_APPLY_RESET = "\x18\n";
    public static final String CMD_APPLY_FLOWCONTROL = "{\"ex\":2}\n";
    public static final String CMD_ZERO_ALL_AXIS = "{\"gc\":G920g0x0y0z0}\n";
    public static final String CMD_APPLY_BOOTLOADER_MODE = "{\"boot\":1}\n";

}