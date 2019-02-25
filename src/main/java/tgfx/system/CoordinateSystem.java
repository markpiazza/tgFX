package tgfx.system;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * CoordinateSystem
 *
 */
@SuppressWarnings("WeakerAccess")
public final class CoordinateSystem {

    private StringProperty coordinateSystemName = new SimpleStringProperty();
    private int coordinateNumber;
    private int coordinateNumberTgFormat;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private double aOffset;
    private double bOffset;
    private double cOffset;

    /**
     * gcode coordinate access system constructor
     */
    public CoordinateSystem() {
    }


    /**
     * gcode coorcinate access system constructor
     * @param coordinateName coordinate system name
     */
    public CoordinateSystem(String coordinateName) {
        setCoordinate(coordinateName);
        setCoordinateNumberMnemonic(Integer.valueOf(coordinateName.substring(1, 2)));
    }


    /**
     * get coordinate system property
     * @return coordinate system property
     */
    public StringProperty getGcodeCoordinateSystemProperty() {
        return this.coordinateSystemName;
    }


    /**
     * get coordinate number mnemonic
     * @return coordinate number mnemonic
     */
    public int getCoordinateNumberMnemonic() {
        //Returns a 54 vs a 1 
        return coordinateNumber;
    }


    /**
     * set coordinate number mnemonic
     * @param coordinateNumber coordinate number mnemonic
     */
    public void setCoordinateNumberMnemonic(int coordinateNumber) {
        if (coordinateNumber > 59 || coordinateNumber < 54) {
            //invalid range
        } else {
            this.coordinateNumber = coordinateNumber;
        }
    }


    /**
     * get coordinate number by tg format
     * @return coordinate number
     */
    public int getCoordinateNumberByTgFormat() {
        //Returns a 54 vs a 1 
        return coordinateNumberTgFormat;
    }


    /**
     * set coordinate number tg format
     * @param coordinateNumberTgFormat coordinate number tf format
     */
    public void setCoordinateNumberTgFormat(int coordinateNumberTgFormat) {
        if (coordinateNumberTgFormat > 6 || coordinateNumberTgFormat < 1) {
            //invalid number range
        } else {
            this.coordinateNumberTgFormat = coordinateNumberTgFormat;
        }
    }


    /**
     * set coordinate number
     * @param number coordinate number
     */
    public void setCoordinateNumber(int number) {
        //sets a 1 for g54 etc...
        switch (number) {
            case 1:
                setCoordinate("g54");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(54);
                break;
            case 2:
                setCoordinate("g55");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(55);
                break;
            case 3:
                setCoordinate("g56");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(56);
                break;
            case 4:
                setCoordinate("g57");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(57);
                break;
            case 5:
                setCoordinate("g58");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(59);
                break;
            case 6:
                setCoordinate("g59");
                setCoordinateNumber(number);
                setCoordinateNumberMnemonic(59);
                break;
        }
    }


    /**
     * get coordinate
     * @return coordinate
     */
    public String getCoordinate() {
        return coordinateSystemName.get();
    }


    /**
     * set coordinate
     * @param coordinate coordinate
     */
    public void setCoordinate(String coordinate) {
        this.coordinateSystemName.set(coordinate);
        this.coordinateSystemName.set(coordinate);
    }


    /**
     * get x offset
     * @return x offset
     */
    public double getxOffset() {
        return xOffset;
    }


    /**
     * set x offset
     * @param xOffset x offset
     */
    public void setxOffset(double xOffset) {
        this.xOffset = xOffset;
    }


    /**
     * get y offset
     * @return y offset
     */
    public double getyOffset() {
        return yOffset;
    }


    /**
     * set y offset
     * @param yOffset y offset
     */
    public void setyOffset(double yOffset) {
        this.yOffset = yOffset;
    }


    /**
     * get z offset
     * @return z offset
     */
    public double getzOffset() {
        return zOffset;
    }


    /**
     * set z offset
     * @param zOffset z offset
     */
    public void setzOffset(double zOffset) {
        this.zOffset = zOffset;
    }


    /**
     * get a offset
     * @return a offset
     */
    public double getaOffset() {
        return aOffset;
    }


    /**
     * set a offset
     * @param aOffset a offset
     */
    public void setaOffset(double aOffset) {
        this.aOffset = aOffset;
    }


    /**
     * get b offset
     * @return b offset
     */
    public double getbOffset() {
        return bOffset;
    }


    /**
     * set b offset
     * @param bOffset b offset
     */
    public void setbOffset(double bOffset) {
        this.bOffset = bOffset;
    }


    /**
     * get c offset
     * @return c offset
     */
    public double getcOffset() {
        return cOffset;
    }


    /**
     * set c offset
     * @param cOffset c offset
     */
    public void setcOffset(double cOffset) {
        this.cOffset = cOffset;
    }
}