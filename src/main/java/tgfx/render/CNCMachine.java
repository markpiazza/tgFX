/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.render;

import java.text.DecimalFormat;
import java.util.Iterator;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgfx.Main;
import tgfx.system.Machine;
import tgfx.system.enums.GcodeUnitMode;
import tgfx.tinyg.TinygDriver;

import static tgfx.tinyg.Commands.*;

/**
 * CNCMachine pane
 *
 */
public class CNCMachine extends Pane {
    private static final Logger logger = LogManager.getLogger();

    private final Circle cursorPoint = new Circle(2, javafx.scene.paint.Color.RED);

    private TinygDriver DRIVER = TinygDriver.getInstance();
    private Machine MACHINE = DRIVER.getMachine();

    private static StackPane gcodePane = new StackPane(); //Holds CNCMachine

    private double xPrevious;
    private double yPrevious;

    private SimpleDoubleProperty cncHeight = new SimpleDoubleProperty();
    private SimpleDoubleProperty cncWidth = new SimpleDoubleProperty();
    private BooleanExpression cursorVisibleBinding;

    private DecimalFormat df = new DecimalFormat(".");
    private boolean msgSent = false;
    private double magnification = 1;

    /**
     * CNCMachine
     *
     */
    public CNCMachine() {
        //Cursor point indicator
        cursorPoint.setRadius(1);
        this.setMaxSize(0, 0);  //hide this element until we connect

        //Set our machine size from tinyg travel max
        this.setVisible(false);
        this.setPadding(new Insets(10));
        this.setFocusTraversable(true);
        this.setFocused(true);

        /*
         * PositionCursor Set
         */
        final Circle c = new Circle(2, Color.RED);
        final Text cursorText = new Text("None");
        cursorText.setFill(Color.YELLOW);
        cursorText.setFont(Font.font("Arial", 6));

        setupLayout(); //initial layout setup in constructor

        ChangeListener posChangeListener = (observableValue, oldValue, newValue) -> {
            if (MACHINE.getAxisByName("y").getMachinePosition() > heightProperty().get()
                    || MACHINE.getAxisByName("x").getMachinePosition() > widthProperty().get()) {
                hideOrShowCursor(false);
            } else {
                hideOrShowCursor(true);
            }

        };

        this.setOnMouseExited(me -> {
//             gcodePane.getChildren().remove(c);
            getChildren().remove(cursorText);
            unFocusForJogging();
        });

        this.setOnMouseEntered(me -> {
            setFocusForJogging();
            requestFocus();
        });

        this.setOnMouseClicked(me -> {
            //T his is so we can set our machine position when a machine does not have homing switches
            if (me.getButton().equals(MouseButton.SECONDARY)) {
                // Right Clicked
                ContextMenu cm = new ContextMenu();
                MenuItem menuItem1 = new MenuItem("Set Machine Position");
                menuItem1.setOnAction(t -> {
                    // We do not want to draw a line from our previous position
                    Draw2d.setFirstDraw(true);
                    DRIVER.getCommandManager().setMachinePosition(getNormalizedX(me.getX()), getNormalizedY(me.getY()));
                    // This allows us to move our drawing to a new place without drawing a line from the old.
                    Draw2d.setFirstDraw(true);
                    DRIVER.write(CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
                    DRIVER.write(CMD_QUERY_STATUS_REPORT);
                    // G92 does not invoke a status report... So we need to generate one to have
                    // Our GUI update the coordinates to zero
                });
                cm.getItems().add(menuItem1);
                cm.show((Node) me.getSource(), me.getScreenX(), me.getScreenY());
            }
        });


        /*
         * Bindings
         */
        maxHeightProperty().bind(MACHINE.getAxisByName("y").getTravelMaxSimple().multiply(MACHINE.gcodeUnitDivision));
        maxWidthProperty().bind(MACHINE.getAxisByName("x").getTravelMaxSimple().multiply(MACHINE.gcodeUnitDivision));

        cursorPoint.translateYProperty().bind(this.heightProperty().subtract(MACHINE.getAxisByName("y").getMachinePositionSimple()));
        cursorPoint.layoutXProperty().bind(MACHINE.getAxisByName("x").getMachinePositionSimple());

            cncHeight.bind(this.heightProperty());
            cncWidth.bind(this.widthProperty());
//            //When the x or y pos changes we see if we want to show or hide the cursor
            cursorPoint.layoutXProperty().addListener(posChangeListener);
            cursorPoint.layoutYProperty().addListener(posChangeListener);
    }

    public StackPane getGcodePane() {
        return gcodePane;
    }

    public void setGcodePane(StackPane gcodePane) {
        this.gcodePane = gcodePane;
    }

    private void hideOrShowCursor(boolean choice) {
        this.visibleProperty().set(choice);
    }

    private void unFocusForJogging() {
        this.setFocused(true);
//        GcodeTabController.hideGcodeText();
    }

    private void setFocusForJogging() {
        this.setFocused(true);
//        Main.postConsoleMessage("Focused");
        //GcodeTabController.setGcodeText("Jogging Enabled");
    }

    private double getNormalizedX(double x) {
        return x / MACHINE.gcodeUnitDivision.get();
    }

    private double getNormalizedY(double y) {
        return (getHeight() - y) / MACHINE.gcodeUnitDivision.get();
    }

    public String getNormalizedYasString(double y) {
        return df.format(getNormalizedY(y));
    }

    public String getNormalizedXasString(double x) {
        return df.format(getNormalizedX(x));
    }

    private boolean checkBoundsY(Line l) {
        return this.getHeight() - l.getEndY() >= 0
                && this.getHeight() - l.getEndY() <= this.getHeight() + 1;
    }

    private boolean checkBoundsX(Line l) {
        return l.getEndX() >= 0
                && l.getEndX() <= this.getWidth();
    }

    public void clearScreen() {
        this.getChildren().clear();
        Draw2d.setFirstDraw(true);  //We don't want to draw a line from where the previous point was when a clear screen is called.
        setupLayout();  //re-draw the needed elements.
    }

    public void drawLine(String moveType, double vel) {
        Line l = new Line();
        l.setSmooth(true);
        //Code to make mm's look the same size as inches
        double scale = 1;
        double unitMagnication = 1;

        if (MACHINE.getGcodeUnitMode().get().equals(GcodeUnitMode.INCHES.toString())) {
            unitMagnication = 5;  //INCHES
        } else {
            unitMagnication = 2; //MM
        }

        double newX = unitMagnication * (MACHINE.getAxisByName("X").getWorkPosition().get() + 80);// + magnification;
        double newY = unitMagnication * (MACHINE.getAxisByName("Y").getWorkPosition().get() + 80);// + magnification;

        if (newX > getGcodePane().getWidth() || newX > getGcodePane().getWidth()) {
            scale = scale / 2;
            Line line;
            Iterator ii = getGcodePane().getChildren().iterator();
            getGcodePane().getChildren().clear(); //remove them after we have the iterator

            while (ii.hasNext()) {
                if (ii.next().getClass().toString().contains("Line")) {
                    //This is a line.
                    line = (Line) ii.next();
                    line.setStartX(line.getStartX() / 2);
                    line.setStartY(line.getStartY() / 2);
                    line.setEndX(line.getEndX() / 2);
                    line.setEndY(line.getEndY() / 2);
                    getGcodePane().getChildren().add(line);
                }
            }
            Main.postConsoleMessage("Finished Drawing Preview Scale Change.\n");
            getGcodePane().setScaleX(scale);
            getGcodePane().setScaleY(scale);
        }

//        Main.print(gcodePane.getHeight() - MACHINE.getAxisByName("y").getWorkPosition().get());
//        double newX = MACHINE.getAxisByName("x").getMachinePositionSimple().get(); // + magnification;
//        double newY = this.getHeight() - MACHINE.getAxisByName("y").getMachinePositionSimple().get(); // + magnification;
//
        if (Draw2d.isFirstDraw()) {
            //This is to not have us draw a line on the first connect.
            l = new Line(newX, this.getHeight(), newX, this.getHeight());
            Draw2d.setFirstDraw(false);
        } else {
            l = new Line(xPrevious, yPrevious, newX, newY);
            l.setStrokeWidth(.5);
        }

        // TODO: Pull these out to CNC machine or Draw2d these are out of place
        xPrevious = newX;
        yPrevious = newY;

        if (MACHINE.getMotionMode().get().equals("traverse")) {
            //G0 Moves
            l.getStrokeDashArray().addAll(1d, 5d);
            l.setStroke(Draw2d.TRAVERSE);
        } else {
//            l.setStroke(Draw2d.getLineColorFromVelocity(vel));
            l.setStroke(Draw2d.FAST);
        }

        if (this.checkBoundsX(l) && this.checkBoundsY(l)) {
            //Line is within the travel max gcode preview box.  So we will draw it.
            this.getChildren().add(l);  //Add the line to the Pane
//            cursorPoint.visibleProperty().set(true);
            msgSent = false;
            if (!getChildren().contains(cursorPoint)) { //If the cursorPoint is not in the Group and we are in bounds
                this.getChildren().add(cursorPoint);  //Adding the cursorPoint back
            }
        } else {
            logger.info("Outside of Bounds X");

            if (getWidth() != 21 && getHeight() != 21) { //This is a bug fix to avoid the cursor being hidden on the initial connect.
                //This should be fairly harmless as it will always show the cursor if its the inital connect size 21,21
                //its a bit of a hack but it works for now.
//                    cursorPoint.visibleProperty().set(false);
//                    Draw2d.setFirstDraw(true);
                if (getChildren().contains(cursorPoint)) { //If cursor is in the group we are going to remove it util above is true
                    getChildren().remove(this.getChildren().indexOf(cursorPoint)); //Remove it.
                    if (!msgSent) {
                        Main.postConsoleMessage("You are out of your TinyG machine working envelope. " +
                                " You need to either move back in by jogging, homing \n" +
                                " or you can right click on the Gcode Preview and click set position " +
                                " to set your estimated position.\n");
                        msgSent = true; //We do this as to not continue to spam the user with out of bound errors.
                    }
                }
            }
        }

    }

    public void zeroSystem() {
        if (DRIVER.isConnected().get()) {
            try {
                Draw2d.setFirstDraw(true); //This allows us to move our drawing to a new place without drawing a line from the old.
                DRIVER.write(CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
                //G92 does not invoke a status report... So we need to generate one to have
                //Our GUI update the coordinates to zero
                DRIVER.write(CMD_QUERY_STATUS_REPORT);
                //We need to set these to 0 so we do not draw a line from the last place we were to 0,0
                resetDrawingCoords();
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    public void resetDrawingCoords() {
        //After a reset has occured we call this ot reset the previous coords.
        xPrevious = 0;
        yPrevious = 0;
    }

    private void setupLayout() {
        //This draws the x axis text as well as grid etc
        Text xText = new Text("X Axis");
        Text yText = new Text("Y Axis");

        xText.setY(-10);
        xText.xProperty().bind(this.heightProperty().divide(2));
        xText.setRotate(0);
        xText.setFill(Color.YELLOW);
        xText.setFont(Font.font("Arial", 10));

        yText.setX(-25);
        yText.yProperty().bind(this.widthProperty().divide(2));
        yText.setRotate(-90);
        yText.setFill(Color.YELLOW);
        yText.setFont(Font.font("Arial", 10));

        this.getChildren().add(xText);
        this.getChildren().add(yText);

        this.setCursor(Cursor.CROSSHAIR);
        this.getChildren().add(cursorPoint);
    }

    public void autoScaleWorkTravelSpace(double scaleAmount) {
        //Get the axis with the smallest available space.  Think aspect ratio really
        double stroke = 2 / scaleAmount;
        this.setScaleX(scaleAmount);
        this.setScaleY(scaleAmount);
        Iterator ii = this.getChildren().iterator();

        while (ii.hasNext()) {
            if (ii.next().getClass().getName().endsWith("Line")) {
                Line l = (Line) ii.next();
                l.setStrokeWidth(stroke);
            }
        }
    }
}
