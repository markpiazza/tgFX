package tgfx.ui;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.skins.LcdSkin;

public class DROLcdSkin extends LcdSkin {
    public DROLcdSkin(Gauge gauge) {
        super(gauge);
        gauge.setMinMeasuredValueVisible(false);
        gauge.setMaxMeasuredValueVisible(false);
        gauge.setAverageVisible(false);
        redraw();
        resize();
    }
}
