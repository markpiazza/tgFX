/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.ui.gcode;

import javafx.beans.property.SimpleStringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author ril3y
 */
public class GcodeLine {
    private static final Logger logger = LogManager.getLogger();

    private SimpleStringProperty codeLine;// = new SimpleStringProperty();// = new SimpleStringProperty("<gcodeLine>");
    private int gcodeLineNumber;
    

    GcodeLine(String gc, int gcl_number){
        this.codeLine = new SimpleStringProperty(gc);
        this.gcodeLineNumber = gcl_number;
    }
    
    public int getGcodeLineNumber(){
        return this.gcodeLineNumber;
    }
    
    String getCodeLine(){
        return codeLine.get();
    } 
    
    String getGcodeLineJsonified(){
        return("{\"gc\":\""+codeLine.get()+"\"}\n");
    }
    
    
    
}