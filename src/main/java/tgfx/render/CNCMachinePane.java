package tgfx.render;

import java.text.DecimalFormat;
import java.util.Iterator;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
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
import tgfx.MainController;
import tgfx.system.Machine;
import tgfx.system.enums.GcodeUnitMode;
import tgfx.tinyg.TinygDriver;

import static javafx.scene.paint.Color.RED;
import static tgfx.tinyg.Commands.*;

/**
 * CNCMachinePane
 *
 */
public class CNCMachinePane extends Pane {
    private static final Logger logger = LogManager.getLogger();

    private static final TinygDriver DRIVER = TinygDriver.getInstance();
    private static final Machine MACHINE = DRIVER.getMachine();

    private final Circle cursorPoint = new Circle(2, RED);

    private SimpleDoubleProperty cncHeight = new SimpleDoubleProperty();
    private SimpleDoubleProperty cncWidth = new SimpleDoubleProperty();
    private BooleanExpression cursorVisibleBinding;

    private DecimalFormat df = new DecimalFormat("#.###");

    private StackPane gcodePane = new StackPane();
    private Draw2d draw2d = new Draw2d();

    private double xPrevious;
    private double yPrevious;

    private boolean msgSent = false;
    private double magnification = 1;


    /**
     * CNCMachinePane
     * CNCMachinePane constructor
     */
    public CNCMachinePane() {

        //Cursor point indicator
        cursorPoint.setRadius(1);

        // hide this element until we connect
        this.setMaxSize(0, 0);

        // Set our machine size from tinyg travel max
        this.setVisible(false);
        this.setPadding(new Insets(10));
        this.setFocusTraversable(true);
        this.setFocused(true);

        // initial layout setup in constructor
        setupLayout();

        // mouse moved inside the CNCMachinePane
        ChangeListener posChangeListener = (observableValue, oldValue, newValue) -> {
            boolean showCursor = true;
            if (MACHINE.getAxisByName("y").getMachinePosition() > heightProperty().get() ||
                 MACHINE.getAxisByName("x").getMachinePosition() > widthProperty().get()) {
                showCursor = false;
            }
            hideOrShowCursor(showCursor);

        };

        // mouse exited the CNCMachinePane
        this.setOnMouseExited(me -> setFocusForJogging(false));

        // mouse entered the CNCMachinePane
        this.setOnMouseEntered(me -> {
            setFocusForJogging(true);
            requestFocus();
        });

        //  mouse clicked the CNCMachinePane
        this.setOnMouseClicked(me -> {
            logger.info("set machine position");
            //T his is so we can set our machine position when a machine does not have homing switches
            if (me.getButton().equals(MouseButton.SECONDARY)) {
                // Right Clicked
                ContextMenu cm = new ContextMenu();
                MenuItem menuItem1 = new MenuItem("Set Machine Position");
                menuItem1.setOnAction(t -> {
                    // We do not want to draw a line from our previous position
                    draw2d.setFirstDraw(true);
                    DRIVER.getCommandManager().setMachinePosition(
                            getNormalizedX(me.getX()),
                            getNormalizedY(me.getY()));
                    // This allows us to move our drawing to a new place without drawing a line from the old.
                    draw2d.setFirstDraw(true);
                    DRIVER.write(CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
                    DRIVER.write(CMD_QUERY_STATUS_REPORT);
                    // G92 does not invoke a status report... So we need to generate one to have
                    // Our GUI update the coordinates to zero
                });
                cm.getItems().add(menuItem1);
                cm.show((Node) me.getSource(), me.getScreenX(), me.getScreenY());
            }
        });


        maxHeightProperty().bind(MACHINE.getAxisByName("y").travelMaximumProperty()
                .multiply(MACHINE.getGcodeUnitDivision()));
        maxWidthProperty().bind(MACHINE.getAxisByName("x").travelMaximumProperty()
                .multiply(MACHINE.getGcodeUnitDivision()));

        cursorPoint.translateYProperty().bind(
                heightProperty().subtract(MACHINE.getAxisByName("y").machinePositionProperty()));
        cursorPoint.layoutXProperty().bind(
                MACHINE.getAxisByName("x").machinePositionProperty());

        cncHeight.bind(this.heightProperty());
        cncWidth.bind(this.widthProperty());

        //When the x or y pos changes we see if we want to show or hide the cursor
        cursorPoint.layoutXProperty().addListener(posChangeListener);
        cursorPoint.layoutYProperty().addListener(posChangeListener);

        logger.info("CNCMachinePane preview pane initialized");
    }


    /**
     * get the draw plotter
     * @return Draw2D
     */
    public Draw2d getDraw2d() {
        return draw2d;
    }

    /**
     * get the pane that the CNCMachinePane lives in
     * @return gcode pane
     */
    private StackPane getGcodePane() {
        return gcodePane;
    }


    /**
     * hide or show the cursor
     * @param choice sets visibility
     */
    private void hideOrShowCursor(boolean choice) {
        //logger.info(choice?"show cursor":"hide cursor");
        // FIXME: RuntimeException: CNCMachinePane.visible : A bound value cannot be set
        // this.visibleProperty().set(choice);
    }


    /**
     * focus or unfocus for jogging
     * @param focus sets focus
     */
    private void setFocusForJogging(boolean focus) {
        this.setFocused(focus);
    }


    /**
     * get normalized x position
     * @param x x axis position
     * @return normalized x position
     */
    private double getNormalizedX(double x) {
        return x / MACHINE.getGcodeUnitDivision().get();
    }


    /**
     * get normalized y position
     * @param y y axis position
     * @return normalized y position
     */
    private double getNormalizedY(double y) {
        return (getHeight() - y) / MACHINE.getGcodeUnitDivision().get();
    }


    /**
     * get normalized x string
     * @param y y axis position
     * @return formatted y axis position
     */
    public String getNormalizedYasString(double y) {
        return df.format(getNormalizedY(y));
    }


    /**
     * get normalized x string
     * @param x x axis position
     * @return formatted x axis position
     */
    public String getNormalizedXasString(double x) {
        return df.format(getNormalizedX(x));
    }


    /**
     * check y bounds
     * @param l line
     * @return is inside bounds
     */
    private boolean checkBoundsY(Line l) {
        return this.getHeight() - l.getEndY() >= 0
                && this.getHeight() - l.getEndY() <= this.getHeight() + 1;
    }


    /**
     * check x bounds
     * @param l line
     * @return is inside bounds
     */
    private boolean checkBoundsX(Line l) {
        return l.getEndX() >= 0
                && l.getEndX() <= this.getWidth();
    }


    /**
     * clear screen
     * reset layout
     */
    public void clearScreen() {
        logger.info("screen clear triggered CNCMachinePane layout setup");
        this.getChildren().clear();
        draw2d.setFirstDraw(true);  //We don't want to draw a line from where the previous point was when a clear screen is called.
        setupLayout();  //re-draw the needed elements.
    }


    /**
     * draw a line
     * @param moveType type of movement
     * @param vel velocity
     */
    public void drawLine(String moveType, double vel) {
        logger.info("drawLine");
        Line l = new Line();
        l.setSmooth(true);
        //Code to make mm's look the same size as inches
        double scale = 1;
        double unitMagnification = 1;

        if (MACHINE.gcodeUnitModeProperty().get().equals(GcodeUnitMode.INCHES.toString())) {
            unitMagnification = 5;  //INCHES
        } else {
            unitMagnification = 2; //MM
        }

        // FIXME: not moving from point 160x160 (magnification+80)
//        double newX = unitMagnification * (MACHINE.getAxisByName("X").getWorkPosition().get() + 80);
//        double newY = unitMagnification * (MACHINE.getAxisByName("Y").getWorkPosition().get() + 80);

        //FIXME: copied from below, seems to work better than above, but still not quite right
        double newX = MACHINE.getAxisByName("x").machinePositionProperty().get() * 2;
        double newY = this.getHeight() - MACHINE.getAxisByName("y").machinePositionProperty().get() * 2;

        if (newX > getGcodePane().getWidth() || newX > getGcodePane().getWidth()) {
            scale = scale / 2;

            int i = 0;
            ObservableList<Node> nodes = getGcodePane().getChildren();
            getGcodePane().getChildren().clear();
            for(Node node : nodes){
                i++;
                logger.info("Node {} : {}", i, node.getClass().toString());
                if(node.getClass().toString().contains("Line")) {
                    Line line = (Line) node;
                    line.setStartX(line.getStartX() / 2);
                    line.setStartY(line.getStartY() / 2);
                    line.setEndX(line.getEndX() / 2);
                    line.setEndY(line.getEndY() / 2);
                    logger.info("Line {} : ({},{}), ({},{})", i,
                            line.getStartX(), line.getStartY(),
                            line.getEndX(), line.getEndY() );
                    getGcodePane().getChildren().add(line);
                }
            }

            getGcodePane().setScaleX(scale);
            getGcodePane().setScaleY(scale);
        }

//        MainController.print(gcodePane.getHeight() - MACHINE.getAxisByName("y").getWorkPosition().get());
//        double newX = MACHINE.getAxisByName("x").machinePositionProperty().get(); // + magnification;
//        double newY = this.getHeight() - MACHINE.getAxisByName("y").machinePositionProperty().get(); // + magnification;

        if (draw2d.isFirstDraw()) {
            //This is to not have us draw a line on the first connect.
            l = new Line(newX, this.getHeight(), newX, this.getHeight());
            draw2d.setFirstDraw(false);
        } else {
            l = new Line(xPrevious, yPrevious, newX, newY);
            l.setStrokeWidth(.5);
        }

        xPrevious = newX;
        yPrevious = newY;

        if (MACHINE.motionModeProperty().get().equals("traverse")) {
            //G0 Moves
            l.getStrokeDashArray().addAll(1d, 5d);
            l.setStroke(Draw2d.TRAVERSE);
        } else {
            l.setStroke(draw2d.getLineColorFromVelocity(vel));
            l.setStroke(Draw2d.FAST);
        }

        if (this.checkBoundsX(l) && this.checkBoundsY(l)) {
            // Line is within the travel max gcode preview box.  So we will draw it.
            this.getChildren().add(l);  //Add the line to the Pane
            logger.info("Line : ({},{}), ({},{})",
                    l.getStartX(), l.getStartY(),
                    l.getEndX(), l.getEndY() );
            cursorPoint.visibleProperty().set(true);
            msgSent = false;
            // If the cursorPoint is not in the Group and we are in bounds
            if (!getChildren().contains(cursorPoint)) {
                // Adding the cursorPoint back
                this.getChildren().add(cursorPoint);
            }
        } else {
            logger.info("Outside of Bounds X");

            if (getWidth() != 21 && getHeight() != 21) { //This is a bug fix to avoid the cursor being hidden on the initial connect.
                //This should be fairly harmless as it will always show the cursor if its the inital connect size 21,21
                //its a bit of a hack but it works for now.
                    cursorPoint.visibleProperty().set(false);
                    draw2d.setFirstDraw(true);
                if (getChildren().contains(cursorPoint)) { //If cursor is in the group we are going to remove it util above is true
                    getChildren().remove(this.getChildren().indexOf(cursorPoint)); //Remove it.
                    if (!msgSent) {
                        MainController.postConsoleMessage("You are out of your TinyG machine working envelope. " +
                                " You need to either move back in by jogging, homing \n" +
                                " or you can right click on the Gcode Preview and click set position " +
                                " to set your estimated position.\n");
                        msgSent = true; //We do this as to not continue to spam the user with out of bound errors.
                    }
                }
            }
        }
    }


    /**
     * zero coordinates
     */
    public void zeroSystem() {
        logger.info("zeroSystem");
        if (DRIVER.isConnected().get()) {
            draw2d.setFirstDraw(true); //This allows us to move our drawing to a new place without drawing a line from the old.
            DRIVER.write(CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
            //G92 does not invoke a status report... So we need to generate one to have
            //Our GUI update the coordinates to zero
            DRIVER.write(CMD_QUERY_STATUS_REPORT);
            //We need to set these to 0 so we do not draw a line from the last place we were to 0,0
            resetDrawingCoords();
        }
    }


    /**
     * reset drawing coordinates
     */
    public void resetDrawingCoords() {
        logger.info("resetDrawingCoords");
        //After a reset has occurred we call this ot reset the previous coords.
        xPrevious = 0;
        yPrevious = 0;
    }


    /**
     * setup layout
     */
    private void setupLayout() {
        logger.info("Setting up CNCMachinePane layout");
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


    /**
     * scale workspace
     * @param scaleAmount scale amount
     */
    public void autoScaleWorkTravelSpace(double scaleAmount) {
        logger.info("autoScaleWorkTravelSpace");
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


    public double getCncHeight() {
        return cncHeight.get();
    }

    public SimpleDoubleProperty cncHeightProperty() {
        return cncHeight;
    }

    public double getCncWidth() {
        return cncWidth.get();
    }

    public SimpleDoubleProperty cncWidthProperty() {
        return cncWidth;
    }

    public Boolean getCursorVisibleBinding() {
        return cursorVisibleBinding.get();
    }

    public BooleanExpression cursorVisibleBindingProperty() {
        return cursorVisibleBinding;
    }
}
