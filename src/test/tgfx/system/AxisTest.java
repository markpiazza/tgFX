package tgfx.system;

import org.junit.Assert;
import org.junit.Test;
import tgfx.system.enums.AxisMode;
import tgfx.system.enums.AxisName;
import tgfx.system.enums.AxisType;

import static org.junit.Assert.*;

public class AxisTest {

    @Test
    public void setAxisCommandTest(){
        AxisName axisName = AxisName.X;
        AxisType axisType = AxisType.LINEAR;
        AxisMode axisMode = AxisMode.DISABLE;
        Axis axis = new Axis(axisName, axisType, axisMode);
        String command = "am";
        int value = AxisMode.STANDARD.getModeNumber();
        axis.setAxisCommand(command, String.valueOf(value));

        assertEquals(AxisMode.STANDARD, axis.getAxisMode());
    }
}
