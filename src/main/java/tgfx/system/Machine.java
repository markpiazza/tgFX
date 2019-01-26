/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.system.enums.*;
import tgfx.tinyg.MnemonicManager;
import org.json.JSONObject;
import tgfx.tinyg.TinygDriver;
import org.json.JSONException;
import tgfx.hardwarePlatforms.HardwarePlatform;
import tgfx.tinyg.ResponseCommand;

/**
 *
 * @author ril3y
 */
public final class Machine {
    private static final Logger logger = LogManager.getLogger();
    private static Machine machineInstance;

    //TG Specific
    //Machine EEPROM Values
    //binding
    public SimpleDoubleProperty longestTravelAxisValue = new SimpleDoubleProperty();
    public SimpleIntegerProperty xjoggingIncrement = new SimpleIntegerProperty();
    public SimpleIntegerProperty yjoggingIncrement = new SimpleIntegerProperty();
    public SimpleIntegerProperty zjoggingIncrement = new SimpleIntegerProperty();
    public SimpleIntegerProperty ajoggingIncrement = new SimpleIntegerProperty();
    public SimpleStringProperty m_state = new SimpleStringProperty();
    public SimpleStringProperty m_mode = new SimpleStringProperty();
    public SimpleDoubleProperty firmwareBuild = new SimpleDoubleProperty();
    public StringProperty firmwareVersion = new SimpleStringProperty();
    
    public HardwarePlatform hardwarePlatform = new HardwarePlatform();
    
    public StringProperty hardwareId = new SimpleStringProperty("na");
    public StringProperty hardwareVersion = new SimpleStringProperty("na");
    public SimpleDoubleProperty velocity = new SimpleDoubleProperty();
    private StringProperty gcodeUnitMode = new SimpleStringProperty("mm");
    public SimpleDoubleProperty gcodeUnitDivision = new SimpleDoubleProperty(1);
//    private SimpleStringProperty gcodeDistanceMode = new SimpleStringProperty();
    private int switchType = 0; //0=normally closed 1 = normally open
    private int status_report_interval;
//    public GcodeUnitMode gcode_startup_units;
    public GcodeSelectPlane gcode_selectPlane;
    public GcodeCoordinateSystem gcode_select_coord_system;
    public GcodePathControl gcode_path_control;
    public GcodeDistanceMode gcode_distance_mode = GcodeDistanceMode.ABSOLUTE;
    private boolean enable_acceleration;
    private float junction_acceleration;
    private float min_line_segment;
    private float min_arc_segment;
    private double min_segment_time;
    private boolean enable_CR_on_TX;
    private boolean enable_echo;
    private boolean enable_xon_xoff;
    private boolean enable_hashcode;
    //Misc
    public SimpleIntegerProperty lineNumber = new SimpleIntegerProperty(0);
    private String last_message = "";
//    public static MotionMode motion_mode = new SimpleIntegerProperty();
    public static MotionMode motion_mode;
    private final List<Motor> motors = new ArrayList<>();
    private final  List<Axis> axis = new ArrayList<>();
    private List<GcodeCoordinateSystem> gcodeCoordinateSystems = new ArrayList<>();
    public GcodeCoordinateManager gcm = new GcodeCoordinateManager();
    private SimpleStringProperty coordinateSystem = new SimpleStringProperty();

    public static MachineState machine_state;
    private String machineName;

    public Machine() {
        motors.add(new Motor(1));
        motors.add(new Motor(2));
        motors.add(new Motor(3));
        motors.add(new Motor(4));

        axis.add(new Axis(AxisName.X, AxisType.LINEAR, AxisMode.STANDARD));
        axis.add(new Axis(AxisName.Y, AxisType.LINEAR, AxisMode.STANDARD));
        axis.add(new Axis(AxisName.Z, AxisType.LINEAR, AxisMode.STANDARD));
        axis.add(new Axis(AxisName.A, AxisType.ROTATIONAL, AxisMode.STANDARD));
        axis.add(new Axis(AxisName.B, AxisType.ROTATIONAL, AxisMode.STANDARD));
        axis.add(new Axis(AxisName.C, AxisType.ROTATIONAL, AxisMode.STANDARD));

        setMotionMode(0);
        xjoggingIncrement.bind(getAxisByName("X").getTravelMaxSimple());
        yjoggingIncrement.bind(getAxisByName("Y").getTravelMaxSimple());
        zjoggingIncrement.bind(getAxisByName("Z").getTravelMaxSimple());
    }

    public static Machine getInstance(){
        if(machineInstance == null){
            machineInstance = new Machine();
        }
        return machineInstance;
    }

    private void setSwitchType(int swType) {
        this.switchType = swType;
    }

    public int getSwitchType() {
        return switchType;
    }

    public String getSwitchTypeAsString() {
        return switchType == 0 ? "Normally Open" : "Normally Closed";
    }

    public GcodeSelectPlane getGcode_selectPlane() {
        return gcode_selectPlane;
    }

    public GcodeDistanceMode getGcode_distance_mode() {
        return gcode_distance_mode;
    }

    private void setGcodeDistanceMode(String gdm) {
        setGcodeDistanceMode(Integer.valueOf(gdm));
    }

    private void setGcodeDistanceMode(int gdm) {
        switch (gdm) {
            case 0:
                this.gcode_distance_mode = GcodeDistanceMode.ABSOLUTE;
                break;
            case 1:
                this.gcode_distance_mode = GcodeDistanceMode.INCREMENTAL;
                break;
        }
    }

    private void setGcodeSelectPlane(String gsp) {
        setGcodeSelectPlane(Integer.valueOf(gsp));
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    private void setGcodeSelectPlane(int gsp) {
        switch (gsp) {
            case 0:
                this.gcode_selectPlane = GcodeSelectPlane.XY;
                break;
            case 1:
                this.gcode_selectPlane = GcodeSelectPlane.XZ;
                break;
            case 2:
                this.gcode_selectPlane = GcodeSelectPlane.YZ;
                break;
        }
    }

    public void setGcode_selectPlane(GcodeSelectPlane gcode_selectPlane) {
        this.gcode_selectPlane = gcode_selectPlane;
    }

    public StringProperty getHardwareId() {
        return hardwareId;
    }

    private void setHardwareId(String hwIdString) {
        hardwareId.set(hwIdString);
    }

    public StringProperty getHardwareVersion() {
        return hardwareVersion;
    }

    private void setHardwareVersion(String hardwareVersion) {
        if(Integer.valueOf(hardwareVersion) == 8 &&
                hardwarePlatform.getHardwarePlatformVersion() == -1){
            //We do this beacause early builds of TinyG did not have a $hp value so we assume it is an v8 tinyg
            TinygDriver.getInstance().hardwarePlatformManager.setHardwarePlatformByVersionNumber(8);
        }else if(Integer.valueOf(hardwareVersion) == 7 &&
                hardwarePlatform.getHardwarePlatformVersion() == -1) {  //-1 means we have not set it yet
            TinygDriver.getInstance().hardwarePlatformManager.setHardwarePlatformByVersionNumber(7);
        }
        this.hardwareVersion.set(hardwareVersion);
    }

    public GcodePathControl getGcode_path_control() {
        return gcode_path_control;
    }

    private void setGcodePathControl(String gpc) {
        setGcodePathControl(Integer.valueOf(gpc));
    }

    private void setGcodePathControl(int gpc) {
        switch (gpc) {
            case 0:
                this.gcode_path_control = GcodePathControl.G61;
                break;
            case 1:
                this.gcode_path_control = GcodePathControl.G61POINT1;
                break;
            case 2:
                this.gcode_path_control = GcodePathControl.G64;
                break;
        }
    }

    public boolean isEnable_CR_on_TX() {
        return enable_CR_on_TX;
    }

    public void setEnable_CR_on_TX(boolean enable_CR_on_TX) {
        this.enable_CR_on_TX = enable_CR_on_TX;
    }

    public boolean isEnable_hashcode() {
        return enable_hashcode;
    }

    public void setEnable_hashcode(boolean enable_hashcode) {
        this.enable_hashcode = enable_hashcode;
    }

    public float getJunction_acceleration() {
        return junction_acceleration;
    }

    public void setJunction_acceleration(float junction_acceleration) {
        this.junction_acceleration = junction_acceleration;
    }

    public List<Motor> getMotors() {
        return this.motors;
    }

    public int getNumberOfMotors() {
        //return how many numbers are in the system
        return this.getMotors().size();
    }

    public String getMachineName() {
        return machineName;
    }

    private void setGcodeUnits(int unitMode) {
        if (unitMode == 0) {
            gcodeUnitMode.setValue("inches");
            gcodeUnitDivision.set(25.4);  //mm to inches conversion
        } else if (unitMode == 1) {
            gcodeUnitMode.setValue("mm");
            gcodeUnitDivision.set(1.0);
        }
    }

    public StringProperty getGcodeUnitMode() {
        return gcodeUnitMode;
    }

    public int getGcodeUnitModeAsInt() {
        return gcodeUnitMode.get().equals(GcodeUnitMode.MM.toString()) ? 1 : 0;
    }

    public void setGcodeUnits(String gcu) {
        int _tmpgcu = Integer.valueOf(gcu);

        switch (_tmpgcu) {
            case (0):
                gcodeUnitMode.set(GcodeUnitMode.INCHES.toString());
                break;
            case (1):
                gcodeUnitMode.set(GcodeUnitMode.MM.toString());
                break;
        }
    }

    public SimpleStringProperty getMotionMode() {
        return (m_mode);
    }

    public void setMotionMode(int mode) {
        if (mode == 0) {
            m_mode.set(MotionMode.TRAVERSE.toString());
        } else if (mode == 1) {
            m_mode.set(MotionMode.FEED.toString());
        } else if (mode == 2) {
            m_mode.set(MotionMode.CW_ARC.toString());
        } else if (mode == 3) {
            m_mode.set(MotionMode.CCW_ARC.toString());
        } else {
            m_mode.set(MotionMode.CANCEL.toString());
        }
    }

    public int getStatus_report_interval() {
        return status_report_interval;
    }

    public void setStatus_report_interval(int status_report_interval) {
        this.status_report_interval = status_report_interval;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public boolean isEnable_acceleration() {
        return enable_acceleration;
    }

    public void setEnable_acceleration(boolean enable_acceleration) {
        this.enable_acceleration = enable_acceleration;
    }

    public boolean isEnable_echo() {
        return enable_echo;
    }

    private void setEnable_echo(boolean enable_echo) {
        this.enable_echo = enable_echo;
    }

    public boolean isEnable_xon_xoff() {
        return enable_xon_xoff;
    }

    private void setEnableXonXoff(boolean enable_xon_xoff) {
        this.enable_xon_xoff = enable_xon_xoff;
    }

    public double getFirmwareBuild() {
        return firmwareBuild.getValue();
    }

    public void setFirmwareBuild(double firmware_build) throws IOException, JSONException {
        this.firmwareBuild.set(firmware_build);
        TinygDriver.getInstance().notifyBuildChanged();
    }

    public StringProperty getFirmwareVersion() {
        return firmwareVersion;
    }

    private void setFirmwareVersion(String fv) {
        this.firmwareVersion.setValue(fv);
    }

    public int getLineNumber() {
        return lineNumber.get();
    }

    public SimpleIntegerProperty getLineNumberSimple() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber.set(lineNumber);
    }

    public SimpleStringProperty getMachineState() {
        return this.m_state;
    }

//    public void setCoordinateSystem(String cord) {
//        setCoordinate_mode(Integer.valueOf(cord));
//
//    }

    public SimpleStringProperty getCoordinateSystem() {
        return this.coordinateSystem;
    }

    //    public void setCoordinate_mode(double m) {
    //        int c = (int) (m); //Convert this to a int
    //        setCoordinate_mode(c);
    //    }
    //    public int getCoordinateSystemOrd() {
    //        CoordinateSystems[] cs = CoordinateSystems.values();
    //        return 1;
    //    }
    //
    //    public void setCoordinate_mode(int c) {
    //        switch (c) {
    //            case 1:
    //                coordinateSystem.set(CoordinateSystems.G54.toString());
    //                break;
    //            case 2:
    //                coordinateSystem.set(CoordinateSystems.G55.toString());
    //                break;
    //            case 3:
    //                coordinateSystem.set(CoordinateSystems.G56.toString());
    //                break;
    //            case 4:
    //                coordinateSystem.set(CoordinateSystems.G57.toString());
    //                break;
    //            case 5:
    //                coordinateSystem.set(CoordinateSystems.G58.toString());
    //                break;
    //            case 6:
    //                coordinateSystem.set(CoordinateSystems.G59.toString());
    //                break;
    //            default:
    //                coordinateSystem.set(CoordinateSystems.G54.toString());
    //                break;
    //        }
    //    }

    private void setMachineState(int state) {
        switch (state) {
            case 1:
                m_state.set(MachineState.RESET.toString());
                break;
            case 2:
                m_state.set(MachineState.CYCLE.toString());
                break;
            case 3:
                m_state.set(MachineState.STOP.toString());
                break;
            case 4:
                m_state.set(MachineState.END.toString());
                break;
            case 5:
                m_state.set(MachineState.RUN.toString());
                break;
            case 6:
                m_state.set(MachineState.HOLD.toString());
                break;
            case 7:
                m_state.set(MachineState.HOMING.toString());
                break;
            case 8:
                m_state.set(MachineState.PROBE.toString());
                break;
            case 9:
                m_state.set(MachineState.JOG.toString());
                break;
        }
    }

    public float getMin_arc_segment() {
        return min_arc_segment;
    }

    public void setMin_arc_segment(float min_arc_segment) {
        this.min_arc_segment = min_arc_segment;
    }

    public float getMin_line_segment() {
        return min_line_segment;
    }

    public void setMin_line_segment(float min_line_segment) {
        this.min_line_segment = min_line_segment;
    }

    public double getMin_segment_time() {
        return min_segment_time;
    }

    public void setMin_segment_time(double min_segment_time) {
        this.min_segment_time = min_segment_time;
    }

    public Double getVelocity() {
        return velocity.get();
    }

    public void setVelocity(double vel) {
        velocity.set(vel);
    }


    public double getJoggingIncrementByAxis(String _axisName) {
        return getAxisByName(_axisName).getTravelMaxSimple().get();
    }

    public GcodeCoordinateSystem getCoordinateSystemByName(String name) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinate().equals(name)) {
                return _tmpGCS;
            }
        }
        return null;
    }

    public GcodeCoordinateSystem getCoordinateSystemByNumberMnemonic(int number) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinateNumberMnemonic() == number) {
                logger.info("Returned " + _tmpGCS.getCoordinate() + " coord system");
                return _tmpGCS;
            }
        }
        return null;
    }

    public GcodeCoordinateSystem getCoordinateSystemByTgNumber(int number) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinateNumberByTgFormat() == number) {
                logger.info("Returned " + _tmpGCS.getCoordinate() + " coord system");
                return (_tmpGCS);
            }
        }
        return null;
    }

    public List<Axis> getAllAxis() {
        return axis;
    }

    List<Axis> getAllLinearAxis() {
        List<Axis> _allAxis = getAllAxis();
        List<Axis> _retAxisList = new ArrayList<>();
        for (Axis a : _allAxis) {
            if (a.getAxisType().equals(AxisType.LINEAR)) {
                _retAxisList.add(a);
            }
        }
        return _retAxisList;
    }

    private Axis getAxisByName(char c) {
        return getAxisByName(String.valueOf(c));
    }

    public Axis getAxisByName(String name) {
        for (Axis tmpAxis : axis) {
            if (tmpAxis.getAxisName().equals(name.toUpperCase())) {
                return tmpAxis;
            }
        }
        return null;
    }

    public Motor getMotorByNumber(String m) {
        //Little stub method to allow calling getMotorByNumber with String arg.
        return getMotorByNumber(Integer.valueOf(m));
    }

    public Motor getMotorByNumber(int i) {
        for (Motor m : motors) {
            if (m.getId_number() == i) {
                return m;
            }
        }
        return null;
    }

    public int getMotorAxis(Motor m) {
        return m.getId_number();
    }

    public void setMotorAxis(int motorNumber, int x) {
        Motor m = getMotorByNumber(motorNumber);
        m.setMapToAxis(x);
    }

    public void applyJsonStatusReport(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();
        switch (rc.getSettingKey()) {
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_LINE):
                machine.setLineNumber(Integer.valueOf(rc.getSettingValue()));
                setLineNumber(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MOTION_MODE):
                machine.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                break;
            //Machine Position Cases
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MACHINEPOSX):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MACHINEPOSY):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MACHINEPOSZ):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MACHINEPOSA):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_WORKOFFSETX):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_WORKOFFSETY):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_WORKOFFSETZ):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_WORKOFFSETA):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_TINYG_DISTANCE_MODE):
                machine.setGcodeDistanceMode(rc.getSettingValue());
                break;
            /*
             * INSERT HOMED HERE
             */
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_STAT):
                machine.setMachineState(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_UNIT):
                machine.setGcodeUnits(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_COORDNIATE_MODE):
                machine.gcm.setCurrentGcodeCoordinateSystem(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MnemonicManager.MNEMONIC_STATUS_REPORT_VELOCITY):
                machine.setVelocity(Double.valueOf(rc.getSettingValue()));
                break;
        }
    }

    //This is the main method to parser a JSON sys object
    public void applyJsonSystemSetting(JSONObject js, String parent) throws IOException {
        Machine machine = TinygDriver.getInstance().getMachine();
        logger.info("Applying JSON Object to System Group");
        Iterator ii = js.keySet().iterator();
        try {
            while (ii.hasNext()) {
                String _key = ii.next().toString();
                String _val = js.get(_key).toString();
                final ResponseCommand rc = new ResponseCommand(parent, _key, _val);

                switch (_key) {
                    case (MnemonicManager.MNEMONIC_SYSTEM_BAUDRATE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_HARDWARD_PLATFORM):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        TinygDriver.getInstance().hardwarePlatformManager
                                .setHardwarePlatformByVersionNumber(
                                        Integer.valueOf(rc.getSettingValue()));
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_HARDWARE_VERSION):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        if(Integer.valueOf(rc.getSettingValue()) == 8 ){
                            //We do this because there is no $hp in TinyG v8 in builds sub 380.08
                            TinygDriver.getInstance().hardwarePlatformManager
                                    .setHardwarePlatformByVersionNumber(
                                            Integer.valueOf(rc.getSettingValue()));
                        }
                        machine.setHardwareVersion(rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_ENABLE_ECHO):
                        machine.setEnable_echo(Boolean.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_ENABLE_JSON_MODE):
                        //TinygDriver.getInstance().m(Float.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_ENABLE_XON):
                        machine.setEnableXonXoff(Boolean.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_FIRMWARE_BUILD):
                        machine.setFirmwareBuild(Double.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_FIRMWARE_VERSION):
                        machine.setFirmwareVersion(rc.getSettingValue());
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_DEFAULT_GCODE_COORDINATE_SYSTEM):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_DEFAULT_GCODE_DISTANCE_MODE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodeDistanceMode(rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_DEFAULT_GCODE_PATH_CONTROL):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodePathControl(rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_DEFAULT_GCODE_PLANE):
                        //TinygDriver.getInstance().m.setGcodeSelectPlane(Float.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodeSelectPlane(rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_JSON_VOBERSITY):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_JUNCTION_ACCELERATION):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_MIN_ARC_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_MIN_LINE_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_MIN_TIME_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_QUEUE_REPORTS):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_STATUS_REPORT_INTERVAL):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_SWITCH_TYPE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setSwitchType(Integer.valueOf(rc.getSettingValue()));
                        String[] message = new String[2];
                        message[0] = "MACHINE_UPDATE";
                        message[1] = null;
                        TinygDriver.getInstance().resParse.set_Changed();
                        TinygDriver.getInstance().resParse.notifyObservers(message);
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_TEXT_VOBERSITY):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MnemonicManager.MNEMONIC_SYSTEM_TINYG_ID_VERSION):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        this.setHardwareId(rc.getSettingValue());
                        break;
                }
            }
        } catch (JSONException | NumberFormatException ex) {
            logger.error("Error in ApplyJsonSystemSetting in Machine:SYS group");
        }
    }
}