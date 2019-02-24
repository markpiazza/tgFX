package tgfx.system;

import static tgfx.TgFXConstants.*;

/**
 * StatusCode
 *
 */
public enum StatusCode {
    TG_UNRECOGNIZED_COMMAND(40, "Parser did not recognize the command", ERR_COMMAND),
    TG_EXPECTED_COMMAND_LETTER(41, "Parser received malformed line", ERR_COMMAND),
    TG_BAD_NUMBER_FORMAT(42, "Number format error", ERR_INPUT),
    TG_INPUT_EXCEEDS_MAX_LENGTH(43, "Input string is too long", ERR_INPUT),
    TG_INPUT_VALUE_TOO_SMALL(44, "Input value is too small", ERR_INPUT),
    TG_INPUT_VALUE_TOO_LARGE(45, "Input value is over maximum", ERR_INPUT),
    TG_INPUT_VALUE_RANGE_ERROR(46, "Input value is out-of-range", ERR_INPUT),
    TG_INPUT_VALUE_UNSUPPORTED(47, "Input value is not supported", ERR_INPUT),
    TG_JSON_SYNTAX_ERROR(48, "JSON string is not well formed", ERR_JSON),
    TG_JSON_TOO_MANY_PAIRS(49, "JSON string or has too many JSON pairs", ERR_JSON),
    TG_ZERO_LENGTH_MOVE(60, "Move is zero length", ERR_GCODE),
    TG_GCODE_BLOCK_SKIPPED(61, "Block is too short - was skipped", ERR_GCODE),
    TG_GCODE_INPUT_ERROR(62, "General error for gcode input",ERR_GCODE),
    TG_GCODE_FEEDRATE_ERROR(63, "Move has no feedrate", ERR_GCODE),
    TG_GCODE_AXIS_WORD_MISSING(64, "Command requires at least one axis present", ERR_GCODE),
    TG_MODAL_GROUP_VIOLATION(65, "Gcode modal group error", ERR_GCODE),
    TG_HOMING_CYCLE_FAILED(66, "Homing cycle did not complete", ERR_MOVEMENT),
    TG_MAX_TRAVEL_EXCEEDED(67, "Maximum travel speed exceeded", ERR_MOVEMENT),
    TG_MAX_SPINDLE_SPEED_EXCEEDED(68, "Max spindle speed exceeded", ERR_MOVEMENT),
    TG_ARC_SPECIFICATION_ERROR(69, "Arc specification error", ERR_MOVEMENT);

    private int statusNumber;
    private String message;
    private String statusType;

    /**
     *
     * @param statusNumber number
     * @param message message
     * @param type type
     */
    StatusCode(int statusNumber, String message, String type) {
        this.statusNumber = statusNumber;
        this.message = message;
        this.statusType = type;
    }

    public String getStatusType() {
        return statusType;
    }

    public int getStatusNumber() {
        return statusNumber;
    }

    public String getMessage() {
        return message;
    }
}
