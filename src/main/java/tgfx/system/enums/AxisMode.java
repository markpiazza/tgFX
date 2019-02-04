package tgfx.system.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum AxisMode {
    DISABLE(0),
    STANDARD(1),
    INHIBITED(2),
    RADIUS(3),
    SLAVE_X(4),
    SLAVE_Y(5),
    SLAVE_Z(6),
    SLAVE_XY(7),
    SLAVE_XZ(8),
    SLAVE_YZ(9),
    SLAVE_XYZ(10),
    UNKNOWN(-1);

    private static final Map<Integer, AxisMode> axisMap = new HashMap<>();

    static{
        for(AxisMode axisMode : AxisMode.values()){
            axisMap.put(axisMode.number, axisMode);
        }
    }

    int number;

    AxisMode(int number){
        this.number = number;
    }

    public static AxisMode getAxisMode(int number){
        return Optional.ofNullable(axisMap.get(number)).orElse(UNKNOWN);
    }

    public int getModeNumber(){
        return this.number;
    }
}
