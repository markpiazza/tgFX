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

    private SimpleStringProperty codeLine;
    private int gcodeLineNumber;
    
    GcodeLine(String codeLine, int codeLineNum){
        this.codeLine = new SimpleStringProperty(codeLine);
        this.gcodeLineNumber = codeLineNum;
    }
    
    public int getGcodeLineNumber(){
        return this.gcodeLineNumber;
    }
    
    public String getCodeLine(){
        return codeLine.get();
    } 
    
    public String getGcodeLineJsonified(){
        return("{\"gc\":\""+codeLine.get()+"\"}\n");
    }
}