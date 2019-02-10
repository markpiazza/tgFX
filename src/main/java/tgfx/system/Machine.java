package tgfx.system;

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
import org.json.JSONObject;
import tgfx.tinyg.TinygDriver;
import org.json.JSONException;
import tgfx.hardwarePlatforms.HardwarePlatform;
import tgfx.tinyg.ResponseCommand;

import static tgfx.tinyg.Mnemonics.*;

/**
 * Machine
 * contains machine specific values (states, switches, acceleration, axises, jog and travel )
 *
 */
@SuppressWarnings({"FieldCanBeLocal","unused"})
public final class Machine {
    private static final Logger logger = LogManager.getLogger();
    private static Machine machineInstance;

    private HardwarePlatform hardwarePlatform = HardwarePlatform.getInstance();

    //TG Specific Machine EEPROM Values binding
    private SimpleDoubleProperty longestTravelAxisValue = new SimpleDoubleProperty();
    private SimpleIntegerProperty xjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty yjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty zjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty ajoggingIncrement = new SimpleIntegerProperty();

    private StringProperty hardwareId = new SimpleStringProperty("na");
    private StringProperty hardwareVersion = new SimpleStringProperty("na");
    private StringProperty firmwareVersion = new SimpleStringProperty();
    private SimpleDoubleProperty firmwareBuild = new SimpleDoubleProperty();

    private SimpleStringProperty machineState = new SimpleStringProperty();
    private SimpleStringProperty motionMode = new SimpleStringProperty();

    public SimpleDoubleProperty velocity = new SimpleDoubleProperty();
    private StringProperty gcodeUnitMode = new SimpleStringProperty("mm");
    public SimpleDoubleProperty gcodeUnitDivision = new SimpleDoubleProperty(1);
//    private SimpleStringProperty gcodeDistanceMode = new SimpleStringProperty();

    private int switchType = 0; //0=normally closed 1 = normally open
    private int statusReportInterval;

//    public GcodeUnitMode gcode_startup_units;
    private GcodeSelectPlane gcodeSelectPlane;
    private GcodeCoordinateSystem gcodeCoordinateSystem;
    private GcodePathControl gcodePathControl;
    private GcodeDistanceMode gcodeDistanceMode = GcodeDistanceMode.ABSOLUTE;

    private float junctionAcceleration;
    private float minLineSegment;
    private float minArcSegment;
    private double minSegmentTime;
    private boolean enableAcceleration;
    private boolean enableCrOnTx;
    private boolean enableEcho;
    private boolean enableXonXoff;
    private boolean enableHashcode;

    private final List<Motor> motors = new ArrayList<>();
    private final List<Axis> axis = new ArrayList<>();

    private List<GcodeCoordinateSystem> gcodeCoordinateSystems = new ArrayList<>();
    private GcodeCoordinateManager coordinateManager = new GcodeCoordinateManager();
    private SimpleStringProperty coordinateSystem = new SimpleStringProperty();
    private SimpleIntegerProperty lineNumber = new SimpleIntegerProperty(0);

    private String lastMessage = "";
    private String machineName;

    /**
     * Machine
     * machine constructor
     */
    private Machine() {
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


    /**
     * get singleton instance of a machine
     * @return singleton machine
     */
    public static Machine getInstance(){
        if(machineInstance == null){
            machineInstance = new Machine();
        }
        return machineInstance;
    }


    /**
     * get hardware platform
     * @return hardware platform
     */
    public HardwarePlatform getHardwarePlatform() {
        return hardwarePlatform;
    }


    /**
     * set hardware platform
     * @param hardwarePlatform hardware platform
     */
    public void setHardwarePlatform(HardwarePlatform hardwarePlatform) {
        this.hardwarePlatform = hardwarePlatform;
    }


    /**
     * get coordinate system
     * @return coordinate system
     */
    public GcodeCoordinateManager getGcodeCoordinateManager(){
        return coordinateManager;
    }


    /**
     * get last message
     * @return message
     */
    public String getLastMessage() {
        return lastMessage;
    }


    /**
     * set last message
     * @param lastMessage message
     */
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    /**
     * get hardware id
     * @return hardware id
     */
    public StringProperty getHardwareId(){
        return hardwareId;
    }


    /**
     * set hardware id
     * @param hardwareId hardware id
     */
    public void setHardwareId(String hardwareId){
        this.hardwareId.setValue(hardwareId);
    }


    /**
     * get hardware version
     * @return hardware version
     */
    public StringProperty getHardwareVersion(){
        return hardwareVersion;
    }


    /**
     * set hardware version
     * @param hardwareVersion hardware version
     */
    public void setHardwareVersion(String hardwareVersion){
        this.hardwareVersion.setValue(hardwareVersion);
    }


    /**
     * get switch type
     * @return switch type
     */
    public int getSwitchType() {
        return switchType;
    }


    /**
     * set switch type
     * @param swType switch type
     */
    private void setSwitchType(int swType) {
        this.switchType = swType;
    }


    /**
     * get switch type as a string
     * @return switch type string
     */
    public String getSwitchTypeAsString() {
        return switchType == 0 ? "Normally Open" : "Normally Closed";
    }


    /**
     * get longest travel axis value
     * @return longest travel
     */
    public double getLongestTravelAxisValue() {
        return longestTravelAxisValue.get();
    }


    /**
     * set longest travel axis value
     * @param value longest travel
     */
    public void setLongestTravelAxisValue(double value){
        longestTravelAxisValue.set(value);
    }


    /**
     *
     * @return
     */
    public GcodeDistanceMode getGcodeDistanceMode() {
        return gcodeDistanceMode;
    }


    /**
     *
     * @param gdm
     */
    private void setGcodeDistanceMode(String gdm) {
        setGcodeDistanceMode(Integer.valueOf(gdm));
    }


    /**
     *
     * @param gdm
     */
    private void setGcodeDistanceMode(int gdm) {
        switch (gdm) {
            case 0:
                this.gcodeDistanceMode = GcodeDistanceMode.ABSOLUTE;
                break;
            case 1:
                this.gcodeDistanceMode = GcodeDistanceMode.INCREMENTAL;
                break;
        }
    }


    /**
     *
     * @return
     */
    public GcodeSelectPlane getGcodeSelectPlane() {
        return gcodeSelectPlane;
    }


    /**
     *
     * @param gsp
     */
    private void setGcodeSelectPlane(String gsp) {
        setGcodeSelectPlane(Integer.valueOf(gsp));
    }


    /**
     *
     * @param gsp
     */
    private void setGcodeSelectPlane(int gsp) {
        switch (gsp) {
            case 0:
                this.gcodeSelectPlane = GcodeSelectPlane.XY;
                break;
            case 1:
                this.gcodeSelectPlane = GcodeSelectPlane.XZ;
                break;
            case 2:
                this.gcodeSelectPlane = GcodeSelectPlane.YZ;
                break;
        }
    }


    /**
     *
     * @param gcodeSelectPlane
     */
    public void setGcodeSelectPlane(GcodeSelectPlane gcodeSelectPlane) {
        this.gcodeSelectPlane = gcodeSelectPlane;
    }


    /**
     *
     * @return
     */
    public GcodePathControl getGcodePathControl() {
        return gcodePathControl;
    }


    /**
     *
     * @param gpc
     */
    private void setGcodePathControl(String gpc) {
        setGcodePathControl(Integer.valueOf(gpc));
    }


    /**
     *
     * @param gpc
     */
    private void setGcodePathControl(int gpc) {
        switch (gpc) {
            case 0:
                this.gcodePathControl = GcodePathControl.G61;
                break;
            case 1:
                this.gcodePathControl = GcodePathControl.G61POINT1;
                break;
            case 2:
                this.gcodePathControl = GcodePathControl.G64;
                break;
        }
    }


    /**
     *
     * @return
     */
    public boolean isEnableCrOnTx() {
        return enableCrOnTx;
    }


    /**
     *
     * @param enableCrOnTx
     */
    public void setEnableCrOnTx(boolean enableCrOnTx) {
        this.enableCrOnTx = enableCrOnTx;
    }


    /**
     *
     * @return
     */
    public boolean isEnableHashcode() {
        return enableHashcode;
    }


    /**
     *
     * @param enableHashcode
     */
    public void setEnableHashcode(boolean enableHashcode) {
        this.enableHashcode = enableHashcode;
    }


    /**
     *
     * @return
     */
    public float getJunctionAcceleration() {
        return junctionAcceleration;
    }

    public void setJunctionAcceleration(float junctionAcceleration) {
        this.junctionAcceleration = junctionAcceleration;
    }


    /**
     *
     * @return
     */
    public List<Motor> getMotors() {
        return this.motors;
    }


    /**
     *
     * @return
     */
    public int getNumberOfMotors() {
        return this.getMotors().size();
    }


    /**
     *
     * @return
     */
    public String getMachineName() {
        return machineName;
    }


    /**
     *
     * @param machineName
     */
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }


    /**
     *
     * @return
     */
    public StringProperty getGcodeUnitMode() {
        return gcodeUnitMode;
    }


    /**
     *
     * @return
     */
    public int getGcodeUnitModeAsInt() {
        return gcodeUnitMode.get().equals(GcodeUnitMode.MM.toString()) ? 1 : 0;
    }


    /**
     *
     * @param unitMode
     */
    private void setGcodeUnits(int unitMode) {
        if (unitMode == 0) {
            gcodeUnitMode.setValue("inches");
            gcodeUnitDivision.set(25.4);  //mm to inches conversion
        } else if (unitMode == 1) {
            gcodeUnitMode.setValue("mm");
            gcodeUnitDivision.set(1.0);
        }
    }


    /**
     *
     * @param gcu
     */
    public void setGcodeUnits(String gcu) {
        switch (Integer.valueOf(gcu)) {
            case 0:
                gcodeUnitMode.set(GcodeUnitMode.INCHES.toString());
                break;
            case 1:
                gcodeUnitMode.set(GcodeUnitMode.MM.toString());
                break;
        }
    }


    /**
     *
     * @return
     */
    public SimpleStringProperty getMotionMode() {
        return motionMode;
    }


    /**
     *
     * @param mode
     */
    public void setMotionMode(int mode) {
        switch (mode) {
            case 0:
                motionMode.set(MotionMode.TRAVERSE.toString());
                break;
            case 1:
                motionMode.set(MotionMode.FEED.toString());
                break;
            case 2:
                motionMode.set(MotionMode.CW_ARC.toString());
                break;
            case 3:
                motionMode.set(MotionMode.CCW_ARC.toString());
                break;
            default:
                motionMode.set(MotionMode.CANCEL.toString());
                break;
        }
    }


    /**
     *
     * @return
     */
    public int getStatusReportInterval() {
        return statusReportInterval;
    }


    /**
     *
     * @param statusReportInterval
     */
    public void setStatusReportInterval(int statusReportInterval) {
        this.statusReportInterval = statusReportInterval;
    }


    /**
     *
     * @return
     */
    public boolean isEnableAcceleration() {
        return enableAcceleration;
    }


    /**
     *
     * @param enableAcceleration
     */
    public void setEnableAcceleration(boolean enableAcceleration) {
        this.enableAcceleration = enableAcceleration;
    }


    /**
     *
     * @return
     */
    public boolean isEnableEcho() {
        return enableEcho;
    }


    /**
     *
     * @param enableEcho
     */
    private void setEnableEcho(boolean enableEcho) {
        this.enableEcho = enableEcho;
    }


    /**
     *
     * @return
     */
    public boolean isEnableXonXoff() {
        return enableXonXoff;
    }


    /**
     *
     * @param enableXonXoff
     */
    private void setEnableXonXoff(boolean enableXonXoff) {
        this.enableXonXoff = enableXonXoff;
    }


    /**
     *
     * @return
     */
    public SimpleDoubleProperty getFirmwareBuild(){
        return firmwareBuild;
    }


    /**
     *
     * @return
     */
    public double getFirmwareBuildVersion(){
        return firmwareBuild.getValue();
    }


    /**
     *
     * @param firmware_build
     * @throws JSONException
     */
    public void setFirmwareBuild(double firmware_build) throws JSONException {
        this.firmwareBuild.set(firmware_build);
        TinygDriver.getInstance().notifyBuildChanged();
    }


    /**
     *
     * @return
     */
    public StringProperty getFirmwareVersion() {
        return firmwareVersion;
    }


    /**
     *
     * @param fv
     */
    public void setFirmwareVersion(String fv) {
        this.firmwareVersion.setValue(fv);
    }


    /**
     *
     * @return
     */
    public int getLineNumber() {
        return lineNumber.get();
    }


    /**
     *
     * @return
     */
    public SimpleIntegerProperty getLineNumberSimple() {
        return lineNumber;
    }


    /**
     *
     * @param lineNumber
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber.set(lineNumber);
    }


    /**
     *
     * @return
     */
    public SimpleStringProperty getCoordinateSystem() {
        return this.coordinateSystem;
    }


    /**
     *
     * @param name
     * @return
     */
    public GcodeCoordinateSystem getCoordinateSystemByName(String name) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinate().equals(name)) {
                return _tmpGCS;
            }
        }
        return null;
    }


    /**
     *
     * @param number
     * @return
     */
    public GcodeCoordinateSystem getCoordinateSystemByNumberMnemonic(int number) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinateNumberMnemonic() == number) {
                logger.info("Returned " + _tmpGCS.getCoordinate() + " coord system");
                return _tmpGCS;
            }
        }
        return null;
    }


    /**
     *
     * @param number
     * @return
     */
    public GcodeCoordinateSystem getCoordinateSystemByTgNumber(int number) {
        for (GcodeCoordinateSystem _tmpGCS : gcodeCoordinateSystems) {
            if (_tmpGCS.getCoordinateNumberByTgFormat() == number) {
                logger.info("Returned " + _tmpGCS.getCoordinate() + " coord system");
                return _tmpGCS;
            }
        }
        return null;
    }

    //    public void setCoordinateSystem(String cord) {
    //        setCoordinate_mode(Integer.valueOf(cord));
    //    }
    //
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


    /**
     *
     * @return
     */
    public SimpleStringProperty getMachineState() {
        return this.machineState;
    }


    /**
     *
     * @param state
     */
    public void setMachineState(int state) {
        switch (state) {
            case 1:
                machineState.set(MachineState.RESET.toString());
                break;
            case 2:
                machineState.set(MachineState.CYCLE.toString());
                break;
            case 3:
                machineState.set(MachineState.STOP.toString());
                break;
            case 4:
                machineState.set(MachineState.END.toString());
                break;
            case 5:
                machineState.set(MachineState.RUN.toString());
                break;
            case 6:
                machineState.set(MachineState.HOLD.toString());
                break;
            case 7:
                machineState.set(MachineState.HOMING.toString());
                break;
            case 8:
                machineState.set(MachineState.PROBE.toString());
                break;
            case 9:
                machineState.set(MachineState.JOG.toString());
                break;
        }
    }


    /**
     *
     * @return
     */
    public float getMinArcSegment() {
        return minArcSegment;
    }


    /**
     *
     * @param minArcSegment
     */
    public void setMinArcSegment(float minArcSegment) {
        this.minArcSegment = minArcSegment;
    }


    /**
     *
     * @return
     */
    public float getMinLineSegment() {
        return minLineSegment;
    }


    /**
     *
     * @param minLineSegment
     */
    public void setMinLineSegment(float minLineSegment) {
        this.minLineSegment = minLineSegment;
    }


    /**
     *
     * @return
     */
    public double getMinSegmentTime() {
        return minSegmentTime;
    }


    /**
     *
     * @param minSegmentTime
     */
    public void setMinSegmentTime(double minSegmentTime) {
        this.minSegmentTime = minSegmentTime;
    }


    /**
     *
     * @return
     */
    public Double getVelocity() {
        return velocity.get();
    }


    /**
     *
     * @param vel
     */
    public void setVelocity(double vel) {
        velocity.set(vel);
    }


    /**
     *
     * @param _axisName
     * @return
     */
    public double getJoggingIncrementByAxis(String _axisName) {
        return getAxisByName(_axisName).getTravelMaxSimple().get();
    }


    /**
     *
     * @return
     */
    public List<Axis> getAllAxis() {
        return axis;
    }


    /**
     *
     * @return
     */
    List<Axis> getAllLinearAxis() {
        List<Axis> allAxis = getAllAxis();
        List<Axis> retAxisList = new ArrayList<>();
        for (Axis a : allAxis) {
            if (a.getAxisType().equals(AxisType.LINEAR)) {
                retAxisList.add(a);
            }
        }
        return retAxisList;
    }


    /**
     *
     * @param c
     * @return
     */
    private Axis getAxisByName(char c) {
        return getAxisByName(String.valueOf(c));
    }


    /**
     *
     * @param name
     * @return
     */
    public Axis getAxisByName(String name) {
        for (Axis tmpAxis : axis) {
            if (tmpAxis.getAxisName().equals(name.toUpperCase())) {
                return tmpAxis;
            }
        }
        return null;
    }


    /**
     *
     * @param m
     * @return
     */
    public Motor getMotorByNumber(String m) {
        //Little stub method to allow calling getMotorByNumber with String arg.
        return getMotorByNumber(Integer.valueOf(m));
    }


    /**
     *
     * @param i
     * @return
     */
    public Motor getMotorByNumber(int i) {
        for (Motor m : motors) {
            if (m.getIdNumber() == i) {
                return m;
            }
        }
        return null;
    }


    /**
     *
     * @param m
     * @return
     */
    public int getMotorAxis(Motor m) {
        return m.getIdNumber();
    }


    /**
     *
     * @param motorNumber
     * @param x
     */
    public void setMotorAxis(int motorNumber, int x) {
        Motor m = getMotorByNumber(motorNumber);
        m.setMapToAxis(x);
    }


    /**
     *
     * @param rc
     */
    public void applyJsonStatusReport(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();
        switch (rc.getSettingKey()) {
            case (MNEMONIC_STATUS_REPORT_LINE):
                machine.setLineNumber(Integer.valueOf(rc.getSettingValue()));
                setLineNumber(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_MOTION_MODE):
                machine.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                break;
            //Machine Position Cases
            case (MNEMONIC_STATUS_REPORT_MACHINEPOSX):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_MACHINEPOSY):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_MACHINEPOSZ):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_MACHINEPOSA):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_WORKOFFSETX):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_WORKOFFSETY):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_WORKOFFSETZ):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_WORKOFFSETA):
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_TINYG_DISTANCE_MODE):
                machine.setGcodeDistanceMode(rc.getSettingValue());
                break;
            /*
             * INSERT HOMED HERE
             */
            case (MNEMONIC_STATUS_REPORT_STAT):
                machine.setMachineState(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_UNIT):
                machine.setGcodeUnits(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_COORDNIATE_MODE):
                machine.coordinateManager.setCurrentGcodeCoordinateSystem(Integer.valueOf(rc.getSettingValue()));
                break;
            case (MNEMONIC_STATUS_REPORT_VELOCITY):
                machine.setVelocity(Double.valueOf(rc.getSettingValue()));
                break;
        }
    }


    /**
     * main method to parser a JSON sys object
     * @param js
     * @param parent
     */
    public void applyJsonSystemSetting(JSONObject js, String parent) {
        Machine machine = TinygDriver.getInstance().getMachine();
        logger.info("Applying JSON Object to System Group");
        Iterator ii = js.keySet().iterator();
        try {
            while (ii.hasNext()) {
                String _key = ii.next().toString();
                String _val = js.get(_key).toString();
                final ResponseCommand rc = new ResponseCommand(parent, _key, _val);

                switch (_key) {
                    case (MNEMONIC_SYSTEM_BAUDRATE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_HARDWARD_PLATFORM):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        TinygDriver.getInstance().getHardwarePlatformManager()
                                .setHardwarePlatformByVersionNumber(
                                        Integer.valueOf(rc.getSettingValue()));
                        break;
                    case (MNEMONIC_SYSTEM_HARDWARE_VERSION):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        if(Integer.valueOf(rc.getSettingValue()) == 8 ){
                            //We do this because there is no $hp in TinyG v8 in builds sub 380.08
                            TinygDriver.getInstance().getHardwarePlatformManager()
                                    .setHardwarePlatformByVersionNumber(
                                            Integer.valueOf(rc.getSettingValue()));
                        }
                        machine.setHardwareVersion(rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_ENABLE_ECHO):
                        machine.setEnableEcho(Boolean.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_ENABLE_JSON_MODE):
                        //TinygDriver.getInstance().m(Float.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_ENABLE_XON):
                        machine.setEnableXonXoff(Boolean.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_FIRMWARE_BUILD):
                        machine.setFirmwareBuild(Double.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_FIRMWARE_VERSION):
                        machine.setFirmwareVersion(rc.getSettingValue());
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_DEFAULT_GCODE_COORDINATE_SYSTEM):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_DEFAULT_GCODE_DISTANCE_MODE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodeDistanceMode(rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_DEFAULT_GCODE_PATH_CONTROL):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodePathControl(rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_DEFAULT_GCODE_PLANE):
                        // TinygDriver.getInstance().m.setGcodeSelectPlane(
                        //   Float.valueOf(rc.getSettingValue()));
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setGcodeSelectPlane(rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_JSON_VOBERSITY):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_JUNCTION_ACCELERATION):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_MIN_ARC_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_MIN_LINE_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_MIN_TIME_SEGMENT):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_QUEUE_REPORTS):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_STATUS_REPORT_INTERVAL):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_SWITCH_TYPE):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        machine.setSwitchType(Integer.valueOf(rc.getSettingValue()));
                        String[] message = new String[2];
                        message[0] = "MACHINE_UPDATE";
                        message[1] = null;
                        TinygDriver.getInstance().getResponseParser().setChanged();
                        TinygDriver.getInstance().getResponseParser().notifyObservers(message);
                        break;
                    case (MNEMONIC_SYSTEM_TEXT_VOBERSITY):
                        logger.info("[APPLIED:" + rc.getSettingParent() + " " +
                                rc.getSettingKey() + ":" + rc.getSettingValue());
                        break;
                    case (MNEMONIC_SYSTEM_TINYG_ID_VERSION):
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