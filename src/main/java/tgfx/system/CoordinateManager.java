package tgfx.system;

import java.util.ArrayList;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CoordinateManager
 *
 */
public class CoordinateManager {
    private static final Logger logger = LogManager.getLogger();

    private CoordinateSystem currentGcodeCoordinateSystem;
    private ArrayList<CoordinateSystem> coordinateSystems = new ArrayList<>();

    /**
     * coordinate manager constructor
     */
    public CoordinateManager() {
        coordinateSystems.add(new CoordinateSystem("g54"));
        coordinateSystems.add(new CoordinateSystem("g55"));
        coordinateSystems.add(new CoordinateSystem("g56"));
        coordinateSystems.add(new CoordinateSystem("g57"));
        coordinateSystems.add(new CoordinateSystem("g58"));
        coordinateSystems.add(new CoordinateSystem("g59"));
        currentGcodeCoordinateSystem = new CoordinateSystem();
    }


    /**
     * get current gcode coordinate system
     * @return current gcode coordinate system
     */
    public CoordinateSystem getCurrentGcodeCoordinateSystem() {
        return currentGcodeCoordinateSystem;
    }


    /**
     * get current gcode coordinate system
     * @return current gcode coordinate system
     */
    public StringProperty getCurrentGcodeCoordinateSystemName() {
        return currentGcodeCoordinateSystem.getGcodeCoordinateSystemProperty();
    }


    /**
     * set current gcode coordinate system
     * @param gcu gcode coordinate system
     */
    void setCurrentGcodeCoordinateSystem(int gcu) {
        switch(gcu){
            case 1: 
                setCurrentGcodeCoordinateSystem("g54");
                break;
            case 2: 
                setCurrentGcodeCoordinateSystem("g55");
                break;
            case 3: 
                setCurrentGcodeCoordinateSystem("g56");
                break;
            case 4: 
                setCurrentGcodeCoordinateSystem("g57");
                break;
            case 5: 
                setCurrentGcodeCoordinateSystem("g58");
                break;
            case 6: 
                setCurrentGcodeCoordinateSystem("g59");
                break;         
        }
    }


    /**
     * set current gcode coordinate system
     * @param gcu gcode coordinate system
     */
    private void setCurrentGcodeCoordinateSystem(String gcu) {
        for (CoordinateSystem system : coordinateSystems) {
            switch (system.getCoordinate().toLowerCase()) {
                case "g54":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
                case "g55":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
                case "g56":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
                case "g57":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
                case "g58":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
                case "g59":
                    currentGcodeCoordinateSystem.setCoordinate(gcu);
                    break;
            }
        }
    }


    /**
     * set current gcode coordinate system
     * @param currentGcodeCoordinateSystem gcode coordinate system
     */
    public void setCurrentGcodeCoordinateSystem(CoordinateSystem currentGcodeCoordinateSystem) {
        this.currentGcodeCoordinateSystem = currentGcodeCoordinateSystem;
    }
}
