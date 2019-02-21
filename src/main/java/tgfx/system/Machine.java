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

    private CoordinateManager coordinateManager = new CoordinateManager();
    private HardwarePlatform hardwarePlatform = new HardwarePlatform();

    //TG Specific Machine EEPROM Values binding
    private SimpleStringProperty hardwareId = new SimpleStringProperty("");
    private SimpleStringProperty hardwareVersion = new SimpleStringProperty("");
    private SimpleStringProperty firmwareVersion = new SimpleStringProperty("");
    private SimpleDoubleProperty firmwareBuild = new SimpleDoubleProperty(0.0);
    private SimpleStringProperty coordinateSystem = new SimpleStringProperty();
    private SimpleStringProperty machineState = new SimpleStringProperty();
    private SimpleStringProperty motionMode = new SimpleStringProperty();
    private SimpleStringProperty gcodeUnitMode = new SimpleStringProperty("");

    private SimpleDoubleProperty longestTravelAxisValue = new SimpleDoubleProperty();
    private SimpleIntegerProperty xjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty yjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty zjoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty ajoggingIncrement = new SimpleIntegerProperty();
    private SimpleIntegerProperty lineNumber = new SimpleIntegerProperty(0);

    private SimpleDoubleProperty gcodeUnitDivision = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty velocity = new SimpleDoubleProperty();

    private final List<Motor> motors = new ArrayList<>();
    private final List<Axis> axis = new ArrayList<>();

    private List<CoordinateSystem> gcodeCoordinateSystems = new ArrayList<>();

    private GcodeUnitMode gcodeStartupUnits;
    private GcodeSelectPlane gcodeSelectPlane;
    private CoordinateSystem gcodeCoordinateSystem;
    private GcodePathControl gcodePathControl;
    private GcodeDistanceMode gcodeDistanceMode = GcodeDistanceMode.ABSOLUTE;

    private String lastMessage = "";
    private String machineName;
    private double minSegmentTime;
    private float junctionAcceleration;
    private float minLineSegment;
    private float minArcSegment;
    private int switchType = 0; //0=normally closed 1 = normally open
    private int statusReportInterval;
    private boolean enableAcceleration;
    private boolean enableCrOnTx;
    private boolean enableEcho;
    private boolean enableXonXoff;
    private boolean enableHashcode;


    /**
     * Machine
     * machine constructor
     */
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
        xjoggingIncrement.bind(getAxisByName("X").travelMaximumProperty());
        yjoggingIncrement.bind(getAxisByName("Y").travelMaximumProperty());
        zjoggingIncrement.bind(getAxisByName("Z").travelMaximumProperty());
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
    public CoordinateManager getGcodeCoordinateManager(){
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


    /** get gcode distance mode
     * @return gcode distance mode
     */
    public GcodeDistanceMode getGcodeDistanceMode() {
        return gcodeDistanceMode;
    }


    /**
     * set gcode distance mode string
     * @param gdm gcode distance mode
     */
    private void setGcodeDistanceMode(String gdm) {
        setGcodeDistanceMode(Integer.valueOf(gdm));
    }


    /**
     * set gcode distance mode by int
     * @param gdm gcode distance mode
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
     * get gcode selected plane
     * @return gcode selected plane
     */
    public GcodeSelectPlane getGcodeSelectPlane() {
        return gcodeSelectPlane;
    }


    /**
     * set gcode selected plane by string
     * @param gsp gcode selected plane
     */
    private void setGcodeSelectPlane(String gsp) {
        setGcodeSelectPlane(Integer.valueOf(gsp));
    }


    /**
     * set gcode selected plane by int
     * @param gsp gcode selected plane
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
     * set gcode selected plane by GcodeSelectedPlane
     * @param gcodeSelectPlane gcode selected plane
     */
    public void setGcodeSelectPlane(GcodeSelectPlane gcodeSelectPlane) {
        this.gcodeSelectPlane = gcodeSelectPlane;
    }


    /**
     * get gcode path control
     * @return gcode path control
     */
    public GcodePathControl getGcodePathControl() {
        return gcodePathControl;
    }


    /**
     * set gcode path control by string
     * @param gpc gcode path control
     */
    private void setGcodePathControl(String gpc) {
        setGcodePathControl(Integer.valueOf(gpc));
    }


    /**
     * set gcode path control by int
     * @param gpc gcode path control
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
     * is enabled carriage return on transmit
     * @return enabled
     */
    public boolean isEnableCrOnTx() {
        return enableCrOnTx;
    }


    /**
     * set enabled carriage return on transmit
     * @param enableCrOnTx enabled
     */
    public void setEnableCrOnTx(boolean enableCrOnTx) {
        this.enableCrOnTx = enableCrOnTx;
    }


    /**
     * is enabled hashcode
     * @return enabled
     */
    public boolean isEnableHashcode() {
        return enableHashcode;
    }


    /**
     * set enabled hashcode
     * @param enableHashcode enabled
     */
    public void setEnableHashcode(boolean enableHashcode) {
        this.enableHashcode = enableHashcode;
    }


    /**
     * get junction acceleration
     * @return junction acceleration
     */
    public float getJunctionAcceleration() {
        return junctionAcceleration;
    }


    /**
     * set junction acceleration
     * @param junctionAcceleration junction acceleration
     */
    public void setJunctionAcceleration(float junctionAcceleration) {
        this.junctionAcceleration = junctionAcceleration;
    }


    /**
     * get motors
     * @return motor list
     */
    public List<Motor> getMotors() {
        return this.motors;
    }


    /**
     * get number of motors
     * @return number motors
     */
    public int getNumberOfMotors() {
        return this.getMotors().size();
    }


    /**
     * get machine name
     * @return machine name
     */
    public String getMachineName() {
        return machineName;
    }


    /**
     * set machine name
     * @param machineName machine name
     */
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }


    /**
     * get gcode unit mode
     * @return gcode unit mode
     */
    public StringProperty getGcodeUnitMode() {
        return gcodeUnitMode;
    }


    /**
     * get gcode unit mode as an int
     * @return gcode unit mode
     */
    public int getGcodeUnitModeAsInt() {
        return gcodeUnitMode.get().equals(GcodeUnitMode.MM.toString()) ? 1 : 0;
    }


    /**
     * set gcode units by string
     * @param gcu gcode units
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
     * set gcode units by int
     * @param unitMode gcode units
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

    public SimpleDoubleProperty getGcodeUnitDivision(){
        return gcodeUnitDivision;
    }


    /**
     * get motion mode property
     * @return motion mode property
     */
    public SimpleStringProperty getMotionMode() {
        return motionMode;
    }


    /**
     * set motion mode
     * @param mode motion mode
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
     * get status report interval
     * @return status report interval
     */
    public int getStatusReportInterval() {
        return statusReportInterval;
    }


    /**
     * set status report interval
     * @param statusReportInterval status report interval
     */
    public void setStatusReportInterval(int statusReportInterval) {
        this.statusReportInterval = statusReportInterval;
    }


    /**
     * get is enabled acceleration
     * @return enabled
     */
    public boolean isEnableAcceleration() {
        return enableAcceleration;
    }


    /**
     * set enable acceleration
     * @param enableAcceleration enabled
     */
    public void setEnableAcceleration(boolean enableAcceleration) {
        this.enableAcceleration = enableAcceleration;
    }


    /**
     * is enabled echo
     * @return enabled
     */
    public boolean isEnableEcho() {
        return enableEcho;
    }


    /**
     * set enabled echo
     * @param enableEcho enabled
     */
    private void setEnableEcho(boolean enableEcho) {
        this.enableEcho = enableEcho;
    }


    /**
     * is enabled xon/xoff
     * @return enabled
     */
    public boolean isEnableXonXoff() {
        return enableXonXoff;
    }


    /**
     * set enabled xon/xoff
     * @param enableXonXoff enabled
     */
    private void setEnableXonXoff(boolean enableXonXoff) {
        this.enableXonXoff = enableXonXoff;
    }


    /**
     * get firmware build
     * @return firmware build property
     */
    public SimpleDoubleProperty getFirmwareBuild(){
        return firmwareBuild;
    }


    /**
     * get firmware build version
     * @return firmware build version
     */
    public double getFirmwareBuildVersion(){
        return firmwareBuild.getValue();
    }


    /**
     * set firmware build
     * @param firmware_build firmware build
     * @throws JSONException json exception
     */
    public void setFirmwareBuild(double firmware_build) throws JSONException {
        this.firmwareBuild.set(firmware_build);
        TinygDriver.getInstance().notifyBuildChanged();
    }


    /**
     * get firmware version
     * @return firmware version
     */
    public StringProperty getFirmwareVersion() {
        return firmwareVersion;
    }


    /**
     * set firmware version
     * @param fv firmware version
     */
    public void setFirmwareVersion(String fv) {
        this.firmwareVersion.setValue(fv);
    }


    /**
     * get line number
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber.get();
    }


    /**
     * get line number property
     * @return line number property
     */
    public SimpleIntegerProperty getLineNumberSimple() {
        return lineNumber;
    }


    /**
     * set line number
     * @param lineNumber line number
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber.set(lineNumber);
    }


    /**
     * get coordinate system property
     * @return coordinate system
     */
    public SimpleStringProperty getCoordinateSystem() {
        return this.coordinateSystem;
    }


    /**
     * get coordinate system by name
     * @param name coordinate system name
     * @return coordinate system
     */
    public CoordinateSystem getCoordinateSystemByName(String name) {
        for (CoordinateSystem coordinateSystem : gcodeCoordinateSystems) {
            if (coordinateSystem.getCoordinate().equals(name)) {
                return coordinateSystem;
            }
        }
        return null;
    }


    /**
     * get coordinate system by number mnemonic
     * @param number number mnemonic
     * @return coordinate system
     */
    public CoordinateSystem getCoordinateSystemByNumberMnemonic(int number) {
        for (CoordinateSystem coordinateSystem : gcodeCoordinateSystems) {
            if (coordinateSystem.getCoordinateNumberMnemonic() == number) {
                return coordinateSystem;
            }
        }
        return null;
    }


    /**
     * get coordinate system by tg number
     * @param number tg number
     * @return coordinate system
     */
    public CoordinateSystem getCoordinateSystemByTgNumber(int number) {
        for (CoordinateSystem coordinateSystem : gcodeCoordinateSystems) {
            if (coordinateSystem.getCoordinateNumberByTgFormat() == number) {
                return coordinateSystem;
            }
        }
        return null;
    }

        public void setCoordinateSystem(String cord) {
            setCoordinateSystem(Integer.valueOf(cord));
        }

        public void setCoordinateSystem(double m) {
            setCoordinateSystem((int) m);
        }

        public void setCoordinateSystem(int c) {
            switch (c) {
                case 1:
                    coordinateSystem.set(GcodeCoordinateSystem.G54.toString());
                    break;
                case 2:
                    coordinateSystem.set(GcodeCoordinateSystem.G55.toString());
                    break;
                case 3:
                    coordinateSystem.set(GcodeCoordinateSystem.G56.toString());
                    break;
                case 4:
                    coordinateSystem.set(GcodeCoordinateSystem.G57.toString());
                    break;
                case 5:
                    coordinateSystem.set(GcodeCoordinateSystem.G58.toString());
                    break;
                case 6:
                    coordinateSystem.set(GcodeCoordinateSystem.G59.toString());
                    break;
                default:
                    coordinateSystem.set(GcodeCoordinateSystem.G54.toString());
                    break;
            }
        }


    /**
     * get machine state
     * @return machine state
     */
    public SimpleStringProperty getMachineState() {
        return machineState;
    }


    /**
     * set machine state
     * @param state machine state
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
     * get min arc segment
     * @return min arc segment
     */
    public float getMinArcSegment() {
        return minArcSegment;
    }


    /**
     * set min arc segment
     * @param minArcSegment min arc segment
     */
    public void setMinArcSegment(float minArcSegment) {
        this.minArcSegment = minArcSegment;
    }


    /**
     * get min line segment
     * @return min line segment
     */
    public float getMinLineSegment() {
        return minLineSegment;
    }


    /**
     * set min line segment
     * @param minLineSegment min line segment
     */
    public void setMinLineSegment(float minLineSegment) {
        this.minLineSegment = minLineSegment;
    }


    /**
     * get min segment time
     * @return min segment time
     */
    public double getMinSegmentTime() {
        return minSegmentTime;
    }


    /**
     * set min segment time
     * @param minSegmentTime min segment time
     */
    public void setMinSegmentTime(double minSegmentTime) {
        this.minSegmentTime = minSegmentTime;
    }


    /**
     * get velocity
     * @return velocity
     */
    public SimpleDoubleProperty velocityProperty() {
        return velocity;
    }

    /**
     * get velocity
     * @return velocity
     */
    public Double getVelocity() {
        return velocity.get();
    }


    /**
     * set velocity
     * @param vel velocity
     */
    public void setVelocity(double vel) {
        velocity.set(vel);
    }


    /**
     * get jogging increment by axis
     * @param axisName axis name
     * @return jogging increment
     */
    public double getJoggingIncrementByAxis(String axisName) {
        // FIXME: possible NPE
        return getAxisByName(axisName).travelMaximumProperty().get();
    }


    /**
     * get all axis list
     * @return axis list
     */
    public List<Axis> getAllAxis() {
        return axis;
    }


    /**
     * get all linear axis list
     * @return linear axis list
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
     * get axis by name
     * @param c axis name character
     * @return axis
     */
    private Axis getAxisByName(char c) {
        return getAxisByName(String.valueOf(c));
    }


    /**
     * get axis by name
     * @param name axis name string
     * @return axis
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
     * get motor by number
     * @param m motor number string
     * @return motor
     */
    public Motor getMotorByNumber(String m) {
        //Little stub method to allow calling getMotorByNumber with String arg.
        return getMotorByNumber(Integer.valueOf(m));
    }


    /**
     * get motor by number
     * @param i motor number int
     * @return motor
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
     * get motor axis
     * @param m motor
     * @return axis number
     */
    public int getMotorAxis(Motor m) {
        return m.getIdNumber();
    }


    /**
     * set motor axis
     * @param motorNumber motor number
     * @param x axis
     */
    public void setMotorAxis(int motorNumber, int x) {
        Motor m = getMotorByNumber(motorNumber);
        // FIXME: possible NPE
        m.setMapToAxis(x);
    }


    /**
     * apply json status report
     * @param rc response command
     */
    public void applyJsonStatusReport(ResponseCommand rc) {
        Machine machine = TinygDriver.getInstance().getMachine();
        switch (rc.getSettingKey()) {
            case MNEMONIC_STATUS_REPORT_LINE:
                machine.setLineNumber(Integer.valueOf(rc.getSettingValue()));
                setLineNumber(Integer.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_MOTION_MODE:
                machine.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                break;
            //Machine Position Cases
            case MNEMONIC_STATUS_REPORT_MACHINEPOSX:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_MACHINEPOSY:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_MACHINEPOSZ:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_MACHINEPOSA:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setMachinePosition(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_WORKOFFSETX:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_WORKOFFSETY:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_WORKOFFSETZ:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_WORKOFFSETA:
                machine.getAxisByName(rc.getSettingKey().charAt(3))
                    .setOffset(Double.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_TINYG_DISTANCE_MODE:
                machine.setGcodeDistanceMode(rc.getSettingValue());
                break;
            /*
             * INSERT HOMED HERE
             */
            case MNEMONIC_STATUS_REPORT_STAT:
                machine.setMachineState(Integer.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_UNIT:
                machine.setGcodeUnits(Integer.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_COORDNIATE_MODE:
                machine.coordinateManager.setCurrentGcodeCoordinateSystem(Integer.valueOf(rc.getSettingValue()));
                break;
            case MNEMONIC_STATUS_REPORT_VELOCITY:
                machine.setVelocity(Double.valueOf(rc.getSettingValue()));
                break;
        }
    }


    /**
     * main method to parser a JSON sys object
     * @param js json object
     * @param parent parent
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
                    case MNEMONIC_SYSTEM_BAUDRATE:
                        logger.info( "not applying system baudrate: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_HARDWARE_PLATFORM:
                        TinygDriver.getInstance().getHardwarePlatformManager()
                                .setHardwarePlatformByVersionNumber(
                                        Integer.valueOf(rc.getSettingValue()));
                        logger.info( "applied hardware platform: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_HARDWARE_VERSION:
                        if(Integer.valueOf(rc.getSettingValue()) == 8 ){
                            //We do this because there is no $hp in TinyG v8 in builds sub 380.08
                            TinygDriver.getInstance().getHardwarePlatformManager()
                                    .setHardwarePlatformByVersionNumber(
                                            Integer.valueOf(rc.getSettingValue()));
                        }
                        machine.setHardwareVersion(rc.getSettingValue());
                        logger.info( "applied hardware version: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_ENABLE_ECHO:
                        machine.setEnableEcho(Boolean.valueOf(rc.getSettingValue()));
                        logger.info( "applied enable echo: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_ENABLE_JSON_MODE:
                        logger.info( "not applying json mode: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_ENABLE_XON:
                        machine.setEnableXonXoff(Boolean.valueOf(rc.getSettingValue()));
                        logger.info( "applied enable xon: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_FIRMWARE_BUILD:
                        machine.setFirmwareBuild(Double.valueOf(rc.getSettingValue()));
                        logger.info( "applied firmware build: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_FIRMWARE_VERSION:
                        machine.setFirmwareVersion(rc.getSettingValue());
                        logger.info( "applied firmware version: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_DEFAULT_GCODE_COORDINATE_SYSTEM:
                        logger.info( "not applying coordinate system default gcode coord system: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_DEFAULT_GCODE_DISTANCE_MODE:
                        machine.setGcodeDistanceMode(rc.getSettingValue());
                        logger.info( "applied default gcode distance mode: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_DEFAULT_GCODE_PATH_CONTROL:
                        machine.setGcodePathControl(rc.getSettingValue());
                        logger.info( "applied default gcode path control: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_DEFAULT_GCODE_PLANE:
                        machine.setGcodeSelectPlane(rc.getSettingValue());
                        logger.info( "applied default gcode plane: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_JSON_VEBORSITY:
                        logger.info( "not applying json verbosity: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_JUNCTION_ACCELERATION:
                        logger.info( "not applying junction acceleration: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_MIN_ARC_SEGMENT:
                        logger.info( "not applying min arc segment: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_MIN_LINE_SEGMENT:
                        logger.info( "not applying min line segment: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_MIN_TIME_SEGMENT:
                        logger.info( "not applying min time segment: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_QUEUE_REPORTS:
                        logger.info( "not applying queue reports: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_STATUS_REPORT_INTERVAL:
                        logger.info( "not applying report interval: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_SWITCH_TYPE:
                        machine.setSwitchType(Integer.valueOf(rc.getSettingValue()));
                        String[] message = new String[2];
                        message[0] = "MACHINE_UPDATE";
                        message[1] = null;
                        TinygDriver.getInstance().getResponseParser().setHasChanged();
                        TinygDriver.getInstance().getResponseParser().notifyObservers(message);
                        logger.info( "applied switch type: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_TEXT_VERBOSITY:
                        logger.info( "not applying text verbosity: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    case MNEMONIC_SYSTEM_TINYG_ID_VERSION:
                        this.setHardwareId(rc.getSettingValue());
                        logger.info( "applied tinyg id version: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                        break;
                    default:
                        logger.error("unknown machine property: {}, {} : {}",
                                rc.getSettingParent(), rc.getSettingKey(), rc.getSettingValue());
                }
            }
        } catch (JSONException | NumberFormatException ex) {
            logger.error("Error in ApplyJsonSystemSetting in Machine:SYS group");
        }
    }
}