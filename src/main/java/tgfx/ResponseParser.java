package tgfx;

import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Observable;
import javafx.application.Platform;
import jfxtras.labs.dialogs.MonologFX;
import jfxtras.labs.dialogs.MonologFXButton;

import static tgfx.tinyg.Mnemonics.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import tgfx.system.Machine;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.ResponseCommand;

/**
 * ResponseParser
 */
public class ResponseParser extends Observable implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    // These values are for mapping what element in the json
    // footer array maps to which values.
    private static final int FOOTER_ELEMENT_PROTOCOL_VERSION = 0;
    private static final int FOOTER_ELEMENT_STATUS_CODE = 1;
    private static final int FOOTER_ELEMENT_RX_RECVD = 2;
    private static final int FOOTER_ELEMENT_CHECKSUM = 3;

    private static final TinygDriver DRIVER = TinygDriver.getInstance();

    //our holder for ResponseFooter Data
    private ResponseFooter responseFooter = new ResponseFooter();
    private String[] message = new String[2];
    private String line;
    private boolean textMode = false;
    private boolean RUN = true;

    /**
     * constructor
     */
    public ResponseParser() {
    }

    /**
     * is text mode
     * are we in text mode or json mode
     * @return is text mode
     */
    private boolean isTextMode() {
        return textMode;
    }


    /**
     * set text mode
     * are we in text mode or json mode
     * @param textMode is text mode
     */
    private void setTextMode(boolean textMode) {
        this.textMode = textMode;
    }


    /**
     * run response parser thread
     */
    @Override
    public void run() {
        logger.info("Response Parser Thread Running...");
        while (RUN) {
            try {
                line = TinygDriver.getJsonQueue().take();
                if (line.equals("")) {
                    continue;
                }
                if (line.startsWith("{")) {
                    if (isTextMode()) {
                        setTextMode(false);
                        // This checks to see if we WERE in textmode. If we were we notify
                        // the user that we are not longer and update the system state.
                        setChanged();
                        message[0] = "TEXTMODE_REPORT";
                        message[1] = "JSON Response Detected... Leaving Text mode..  " +
                                        "Querying System State....\n";
                        notifyObservers(message);
                        DRIVER.getCommandManager().queryAllMachineSettings();
                        DRIVER.getCommandManager().queryAllHardwareAxisSettings();
                        DRIVER.getCommandManager().queryAllMotorSettings();

                    }
                    parseJSON(line);  //Take a line from the response queue when its ready and parse it.

                } else {
                    //Text Mode Response
                    if (!isTextMode()) {
                        //We are just entering text mode and need to alert the user. 
                        //This will fire the every time user is entering text mode.
                        setTextMode(true);
                        setChanged();
                        message[0] = "TEXTMODE_REPORT";
                        message[1] = "User has entered text mode.  " +
                                        "To exit type \"{\" and hit enter.\n";
                        notifyObservers(message);
                    }
                    setChanged();
                    message[0] = "TEXTMODE_REPORT";
                    message[1] = line + "\n";
                    notifyObservers(message);
                }
            } catch (InterruptedException | JSONException ex) {
                logger.error("Error in responseParser run()", ex);
            }
        }
    }


    /**
     * is json object
     * @param js json object
     * @param strVal string to check
     * @return is json object
     */
    private boolean isJsonObject(JSONObject js, String strVal) {
        return js.get(strVal).getClass().toString().contains("JSONObject");
    }


    /**
     * apply setting master group
     * @param js json object
     * @param pg parent group
     */
    private void applySettingMasterGroup(JSONObject js, String pg) {
        if (pg.equals(MNEMONIC_GROUP_STATUS_REPORT)) {
            /*
             * This is a status report master object that came in through a response object.
             * meaning that the response was asked for like this {"sr":""} and returned like this.
             *  {"r":{"sr":{"line":0,"posx":0.000,"posy":0.000,"posz":0.000,"posa":0.000,
             *  "vel":0.000,"unit":1,"momo":0,"stat":3},"f":[1,0,10,885]}}
             * Right now its parsed down to JUST the json object for the SR like so.
             *  {"sr":{"line":0,"posx":0.000,"posy":0.000,"posz":0.000,"posa":0.000,
             *  "vel":0.000,"unit":1,"momo":0,"stat":3},"f":[1,0,10,885]}
             * so we can now just pass it to the applySettingStatusReport method.
             */
            applySettingStatusReport(js);
        } else {
            if (js.keySet().size() > 1) {
                //This is a special multi single value response object
                for (Object o : js.keySet()) {
                    String key = o.toString();
                    if (key.equals("f")) {
                        // This is very important.  We break out our response footer..
                        // error codes.. bytes available in hardware buffer etc.
                        parseFooter(js.getJSONArray("f"));
                    } else {
                        ResponseCommand rc = DRIVER.getMnemonicManager()
                                .lookupSingleGroupMaster(key, pg);

                        // This happens when a new mnemonic has been added to the tinyG firmware
                        // but not added to tgFX's MnemonicManger
                        if (rc == null) {
                            //This is the error case
                            logger.error("Mnemonic Lookup Failed in applySettingsMasterGroup.\n\t" +
                                    "Make sure there are not new elements added to TinyG and not " +
                                    "to the MnemonicManager Class.\n\tMNEMONIC FAILED: " + key);
                        } else {
                            //This is the normal case
                            rc.setSettingValue(js.get(key).toString());
                            // we will supply the parent object name for each key pair
                            applySettings(rc.buildJsonObject(), rc.getSettingParent());
                        }
                    }
                }
            }
        }
    }


    /**
     * apply setting status report
     * @param js json object
     */
    private void applySettingStatusReport(JSONObject js) {
        /*
         * This breaks the mold a bit.. but its more efficient. This gets called
         * off the top of ParseJson if it has an "SR" in it. Sr's are called so
         * often that instead of walking the normal parsing tree.. this skips to
         * the end
         */
        // This is a special multi single value response object
        for (Object o : js.keySet()) {
            String key = o.toString();
            ResponseCommand rc = new ResponseCommand(MNEMONIC_GROUP_SYSTEM, key, js.get(key).toString());
            DRIVER.getMachine().applyJsonStatusReport(rc);
            // we will supply the parent object name for each key pair
            // applySettings(rc.buildJsonObject(), rc.getSettingParent());
        }
        setChanged();
        message[0] = "STATUS_REPORT";
        message[1] = null;
        notifyObservers(message);
    }

    /**
     * set changed
     * FIXME: setChanged calls this.setChanged()???
     */
    public void setChanged() {
        // that'll give you a stack overflow ;)
        // this.setChanged();
        logger.warn("setChanged called, but it's not implemented");
    }


    /**
     * apply setting
     * @param js json object
     */
    private void applySetting(JSONObject js) {
        try {
            //If there are more than one object in the json response
            if (js.keySet().size() > 1) {
                //This is a special multi single value response object
                for (Object o : js.keySet()) {
                    String key = o.toString();
                    switch (key) {
                        case MNEMONIC_GROUP_FOOTER:
                            parseFooter(js.getJSONArray(MNEMONIC_GROUP_FOOTER));
                            // This is very important.
                            // We break out our response footer.. error codes..
                            // bytes available in hardware buffer etc.
                            break;

                        case MNEMONIC_SYSTEM_LAST_MESSAGE:
                            message[0] = "TINYG_USER_MESSAGE";
                            message[1] = js.get(key) + "\n";
                            logger.info("TinyG Message Sent:  " + js.get(key) + "\n");
                            setChanged();
                            notifyObservers(message);
                            break;
                        case MNEMONIC_SYSTEM_REPORT_RX_BUFFER:
                            DRIVER.getSerialWriter().setBuffer(js.getInt(key));
                            break;
                        default:
                            if (DRIVER.getMnemonicManager().isMasterGroupObject(key)) {
                                // logger.info("Group Status Report Detected: " + key);
                                applySettingMasterGroup(js.getJSONObject(key), key);
                                continue;
                            }
                            ResponseCommand rc = DRIVER.getMnemonicManager().lookupSingleGroup(key);
                            rc.setSettingValue(js.get(key).toString());
                            // we will supply the parent object name for each key pair
                            applySettings(rc.buildJsonObject(), rc.getSettingParent());
                            break;
                    }
                }
            } else {
                /* This else follows:
                 * Contains a single object in the JSON response
                 */
                if (js.keySet().contains(MNEMONIC_GROUP_FOOTER)) {
                    /*
                     * This is a single response footer object: Like So:
                     * {"f":[1,0,5,3330]}
                     */
                    parseFooter(js.getJSONArray(MNEMONIC_GROUP_FOOTER));
                } else {
                    /*
                     * Contains a single object in the json response I am not
                     * sure this else is needed any longer.
                     */
                    logger.info("applySettings: {}", js);
                    applySettings(js, js.keys().next());
                }
            }
        } catch (JSONException ex) {
            logger.error("Error in applySetting(JsonObject js) : " + ex.getMessage());
            logger.error("JSON String Was: " + js.toString());
            logger.error("Error in Line: " + js);
        }
    }


    /**
     * apply settings
     * @param js json object
     * @param pg parent group
     */
    private void applySettings(JSONObject js, String pg) {
        Machine machine = DRIVER.getMachine();
        switch (pg) {
            case MNEMONIC_GROUP_MOTOR_1:
                machine.getMotorByNumber(MNEMONIC_GROUP_MOTOR_1)
                        .applyJsonSystemSetting( js.getJSONObject(MNEMONIC_GROUP_MOTOR_1), MNEMONIC_GROUP_MOTOR_1);
                setChanged();
                message[0] = "CMD_GET_MOTOR_SETTINGS";
                message[1] = MNEMONIC_GROUP_MOTOR_1;
                notifyObservers(message);
                break;
            case MNEMONIC_GROUP_MOTOR_2:
                machine.getMotorByNumber(MNEMONIC_GROUP_MOTOR_2)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_MOTOR_2), MNEMONIC_GROUP_MOTOR_2);
                setChanged();
                message[0] = "CMD_GET_MOTOR_SETTINGS";
                message[1] = MNEMONIC_GROUP_MOTOR_2;
                notifyObservers(message);
                break;
            case MNEMONIC_GROUP_MOTOR_3:
                machine.getMotorByNumber(MNEMONIC_GROUP_MOTOR_3)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_MOTOR_3), MNEMONIC_GROUP_MOTOR_3);
                setChanged();
                message[0] = "CMD_GET_MOTOR_SETTINGS";
                message[1] = MNEMONIC_GROUP_MOTOR_3;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_MOTOR_4:
                machine.getMotorByNumber(MNEMONIC_GROUP_MOTOR_4)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_MOTOR_4), MNEMONIC_GROUP_MOTOR_4);
                setChanged();
                message[0] = "CMD_GET_MOTOR_SETTINGS";
                message[1] = MNEMONIC_GROUP_MOTOR_4;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_AXIS_X:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_X)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_X), MNEMONIC_GROUP_AXIS_X);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_X;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_AXIS_Y:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_Y)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_Y), MNEMONIC_GROUP_AXIS_Y);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_Y;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_AXIS_Z:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_Z)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_Z), MNEMONIC_GROUP_AXIS_Z);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_Z;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_AXIS_A:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_A)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_A), MNEMONIC_GROUP_AXIS_A);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_A;
                notifyObservers(message);
                break;
            case MNEMONIC_GROUP_AXIS_B:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_B)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_B), MNEMONIC_GROUP_AXIS_B);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_B;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_AXIS_C:
                machine.getAxisByName(MNEMONIC_GROUP_AXIS_C)
                        .applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_AXIS_C), MNEMONIC_GROUP_AXIS_C);
                setChanged();
                message[0] = "CMD_GET_AXIS_SETTINGS";
                message[1] = MNEMONIC_GROUP_AXIS_C;
                notifyObservers(message);
                break;

            case MNEMONIC_GROUP_HOME:
                logger.info("HOME");
                break;

            case MNEMONIC_GROUP_MSG:
                //NO-OP
                break;

            case MNEMONIC_GROUP_SYSTEM:
                machine.applyJsonSystemSetting(js.getJSONObject(MNEMONIC_GROUP_SYSTEM), MNEMONIC_GROUP_SYSTEM);
                message[0] = "MACHINE_UPDATE";
                message[1] = null;
                setChanged();
                notifyObservers(message);
                break;
            case MNEMONIC_GROUP_STATUS_REPORT:
                logger.info("Status Report");
                applySettingMasterGroup(js, MNEMONIC_GROUP_STATUS_REPORT);
                setChanged();
                message[0] = "STATUS_REPORT";
                message[1] = null;
                notifyObservers(message);
                break;
            case MNEMONIC_GROUP_EMERGENCY_SHUTDOWN:
                doEmergencyShutdown();
                break;
            case "err":
                // FIXME: been getting this, not sure why it's being caught here...
                logger.warn("FIXME: unable to process parent group 'err'");
                break;
            default:
                //This is for single settings xfr, 1tr etc...
                //This is pretty ugly but it gets the key and the value. For single values.
                ResponseCommand rc = DRIVER.getMnemonicManager().lookupSingleGroup(pg);
                if (rc != null) {
                    // I changed this to deal with the fb mnemonic.. not sure if this works all over.
                    rc.setSettingValue(String.valueOf(js.get(js.keys().next())));
                    logger.info("Single Key Value: Group: {}, {} : {} ",
                            rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());

                    if (!rc.getSettingValue().equals("")) {
                        // We pass the new json object we created from the string above
                        this.applySetting(rc.buildJsonObject());
                    } else {
                        logger.info(rc.getSettingKey() + " value was null");
                    }
                } else {
                    logger.error("unable to lookupSingleGroup {}", pg);
                }
        }
    }


    /**
     * convert string to json object and apply settings
     * @param newJsObjString json object string
     */
    public void applySettings(String newJsObjString) {
        //When a single key value pair is sent without the group object
        //We use this method to create a new json object
        JSONObject newJs = new JSONObject(newJsObjString);
        applySetting(newJs);
    }


    /**
     * parse footer
     * @param footerValues json footer values
     */
    private void parseFooter(JSONArray footerValues) {
        //Checking to see if we have a footer response
        //Status reports will not have a footer so this is for everything else
        responseFooter.setProtocolVersion(footerValues.getInt(FOOTER_ELEMENT_PROTOCOL_VERSION));
        responseFooter.setStatusCode(footerValues.getInt(FOOTER_ELEMENT_STATUS_CODE));
        responseFooter.setRxRecvd(footerValues.getInt(FOOTER_ELEMENT_RX_RECVD));
        responseFooter.setCheckSum(footerValues.getInt(FOOTER_ELEMENT_STATUS_CODE));
        //Out footer object is not populated

        int beforeBytesReturned = DRIVER.getSerialWriter().getBufferValue();
        // Make sure we do not add bytes to a already full buffer
        if (beforeBytesReturned != TinygDriver.MAX_BUFFER) {
            DRIVER.getSerialWriter().addBytesReturnedToBuffer(responseFooter.getRxRecvd());
            int afterBytesReturned = DRIVER.getSerialWriter().getBufferValue();
            logger.debug("Returned {} to buffer... Buffer was {}  is now {}",
                    responseFooter.getRxRecvd(), beforeBytesReturned, afterBytesReturned );
            // We let our serialWriter thread know we have added some space to the buffer.
            DRIVER.getSerialWriter().notifyAck();
            //Lets tell the UI the new size of the buffer
            message[0] = "BUFFER_UPDATE";
            message[1] = String.valueOf(afterBytesReturned);
            setChanged();
            notifyObservers(message);
        }
    }


    /**
     * parse the json
     * @param line string line
     * @throws JSONException json exception
     */
    private synchronized void parseJSON(String line) throws JSONException {
        logger.info("Got Line: " + line + " from TinyG.");

        final JSONObject js = new JSONObject(line);
        // tgfx is for messages like timeout connections
        if ( js.has(MNEMONIC_GROUP_RESPONSE)
                || js.has(MNEMONIC_GROUP_STATUS_REPORT)
                || js.has(MNEMONIC_GROUP_TGFX) ) {
            Platform.runLater(() -> {
                try {
                    if (js.has(MNEMONIC_GROUP_TGFX)) {
                        //This is for when tgfx times out when trying to connect to TinyG.
                        //tgFX puts a message in the response parser queue to be parsed here.
                        setChanged();
                        message[0] = "TINYG_CONNECTION_TIMEOUT";
                        message[1] = js.get(MNEMONIC_GROUP_TGFX) + "\n";
                        notifyObservers(message);

                    } else if (js.has(MNEMONIC_GROUP_FOOTER)) {
                        //The new version of TinyG's footer has a footer element in each response.
                        //We parse it here
                        parseFooter(js.getJSONArray(MNEMONIC_GROUP_FOOTER));
                        if (js.has(MNEMONIC_GROUP_RESPONSE)) {
                            applySetting(js.getJSONObject(MNEMONIC_GROUP_RESPONSE));
                        } else if (js.has(MNEMONIC_GROUP_STATUS_REPORT)) {
                            applySettingStatusReport(js.getJSONObject(MNEMONIC_GROUP_STATUS_REPORT));
                        }

                    } else {  //This is where the old footer style is dealt with

                        //These are the 2 types of responses we will get back.
                        switch (js.keys().next()) {
                            case MNEMONIC_GROUP_RESPONSE:
                                applySetting(js.getJSONObject(MNEMONIC_GROUP_RESPONSE));
                                break;
                            case MNEMONIC_GROUP_STATUS_REPORT:
                                applySettingStatusReport(js.getJSONObject(MNEMONIC_GROUP_STATUS_REPORT));
                                break;
                        }
                    }

                } catch (JSONException ex) {
                    logger.error(ex);
                }
            });

        } else if (js.has(MNEMONIC_GROUP_QUERY_REPORT)) {
            DRIVER.getQueryReport().parse(js);
        } else if (js.has(MNEMONIC_GROUP_EMERGENCY_SHUTDOWN)) {
            applySetting(js);
        }

    }


    /**
     * emergency shutdown
     */
    private void doEmergencyShutdown(){
        Platform.runLater(() -> {
            Main.postConsoleMessage("TinyG Alarm " + line);

            MonologFXButton btnYes = new MonologFXButton();
            btnYes.setDefaultButton(true);
            btnYes.setIcon("/testmonologfx/dialog_apply.png");
            btnYes.setType(MonologFXButton.Type.YES);

            MonologFXButton btnNo = new MonologFXButton();
            btnNo.setCancelButton(true);
            btnNo.setIcon("/testmonologfx/dialog_cancel.png");
            btnNo.setType(MonologFXButton.Type.CANCEL);

            MonologFX mono = new MonologFX();
            mono.setTitleText("Error Occurred");
            mono.setMessage("You have triggered a limit switch. " +
                    "TinyG is now in DISABLED mode. \n " +
                    "Manually back your machine off of its " +
                    "limit switches.\n  Once done, if you would" +
                    "like to re-enable TinyG click yes.");
            mono.addButton(btnYes);
            mono.addButton(btnNo);
            mono.setType(MonologFX.Type.ERROR);

            switch (mono.show()) {
                case YES:
                    logger.info("Clicked Yes");
                    try {
                        DRIVER.priorityWrite((byte) 0x18);
                    } catch (SerialPortException ex) {
                        logger.error(ex);
                    }
                    break;
                case CANCEL:
                    logger.info("Clicked No");
                    Main.postConsoleMessage("TinyG will remain in disabled " +
                            "mode until you power cycle or click the reset button.");
                    break;
            }
        });
    }
}
