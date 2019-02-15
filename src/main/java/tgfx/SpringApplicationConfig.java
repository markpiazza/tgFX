package tgfx;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tgfx.ui.firmware.FirmwareUpdaterController;
import tgfx.ui.gcode.GcodeTabController;
import tgfx.ui.machinesettings.MachineSettingsController;
import tgfx.ui.tgfxsettings.TgfxSettingsController;
import tgfx.ui.tinygconfig.TinyGConfigController;

@Configuration
public class SpringApplicationConfig {

    @Bean
    public Main getMain(){
        return new Main();
    }

    @Bean
    public GcodeTabController getGcodeTabController(){
        return new GcodeTabController();
    }

    @Bean
    public TinyGConfigController getTinyGConfigController(){
        return new TinyGConfigController();
    }

    @Bean
    public FirmwareUpdaterController getFirmwareUpdaterController(){
        return new FirmwareUpdaterController();
    }

    @Bean
    public MachineSettingsController getMachineSettingsController(){
        return new MachineSettingsController();
    }

    @Bean
    public TgfxSettingsController getTgfxSettingsController(){
        return new TgfxSettingsController();
    }


}
