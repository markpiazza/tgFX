package tgfx.render;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draw2d
 *
 */
public class Draw2d {
    private static final Logger logger = LogManager.getLogger();

    static Paint retPaint;
    static Paint FAST = Color.web("#85ff22");
    static Paint TRAVERSE = Color.GRAY;

    private static Paint SLOWEST = Color.web("#ee1a0f");
    private static Paint SLOW = Color.web("#ff9933");
    private static Paint MEDIUM_SLOW = Color.web("#eed00f");
    private static Paint MEDUIM = Color.web("#c1ff66");
    private static Paint FASTEST = Color.web("#0fee17");
    private float MAX_MACHINE_VELOCITY;
    private static double stroke_weight = .5;
    private static double magnification = 3;
    private static double magZoomIncrement = 2;
    private static double strokeIncrement = .1;
    private static boolean firstDraw = true;

    public Draw2d(){
    }

    public boolean isFirstDraw() {
        return firstDraw;
    }

    public void setFirstDraw(boolean firstDraw) {
        Draw2d.firstDraw = firstDraw;
    }

    public double getMagnification() {
        return magnification;
    }

    public void setMagnification(boolean b) {
        if (b) {
            Draw2d.magnification = magnification + magZoomIncrement;
        } else {
            Draw2d.magnification = magnification - magZoomIncrement;
        }
    }

    private void calculateStroke() {
        if (stroke_weight <= 5) {
            strokeIncrement = 1;
        }
        if (stroke_weight <= 2) {
            strokeIncrement = .5;
        }
        if (stroke_weight <= 1) {
            strokeIncrement = .1;
        }
        if (stroke_weight <= .1) {
            strokeIncrement = .01;
        }
        if (strokeIncrement <= .01) {
            strokeIncrement = .001;
        }
    }

    public void incrementSetStrokeWeight() {
        calculateStroke();
        stroke_weight = stroke_weight + strokeIncrement;
    }

    public void decrementSetStrokeWeight() {
        calculateStroke();
        stroke_weight = stroke_weight - strokeIncrement;
    }

//    public static void setStrokeWeight(double w) {
//        if (w < 0) {
//            w = 0.01;
//        }
//        stroke_weight = w;
//    }

    public double getStrokeWeight() {
        return stroke_weight;
    }

    public Paint getLineColorFromVelocity(double vel) {
        if (vel > 1 && vel < 100) {
            return SLOWEST;
        } else if (vel > 101 && vel <= 250) {
            return SLOW;
        } else if (vel > 251 && vel <= 350) {
            return MEDIUM_SLOW;
        } else if (vel > 351 && vel <= 390) {
            return MEDUIM;
        } else if (vel > 391 && vel <= 650) {
            return FAST;
        } else if (vel > 651) {
            return FASTEST;
        }
        return retPaint;
    }
}