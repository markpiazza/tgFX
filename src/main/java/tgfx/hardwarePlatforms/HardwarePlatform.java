package tgfx.hardwarePlatforms;

/**
 * HardwarePlatform
 *
 */
public class HardwarePlatform {
    private static HardwarePlatform hardwarePlatformInstance;

    private String platformName;
    private Double minimalBuildVersion;
    private String latestVersionUrl;
    private String manufacturer;
    private String firmwareUrl;
    private int hardwarePlatformVersion = -1;
    private boolean isUpgradeable;

    private HardwarePlatform() {
    }

    public static HardwarePlatform getInstance() {
        if(hardwarePlatformInstance==null) {
            hardwarePlatformInstance = new HardwarePlatform();
        }
        return hardwarePlatformInstance;
    }

    public int getHardwarePlatformVersion() {
        return hardwarePlatformVersion;
    }

    public void setHardwarePlatformVersion(int hardwarePlatformVersion) {
        this.hardwarePlatformVersion = hardwarePlatformVersion;
    }

    public boolean isIsUpgradeable() {
        return isUpgradeable;
    }

    public void setIsUpgradeable(boolean isUpgradeable) {
        this.isUpgradeable = isUpgradeable;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public Double getMinimalBuildVersion() {
        return minimalBuildVersion;
    }

    public void setMinimalBuildVersion(Double minimalBuildVersion) {
        this.minimalBuildVersion = minimalBuildVersion;
    }

    public String getLatestVersionUrl() {
        return latestVersionUrl;
    }

    public void setLatestVersionUrl(String latestVersionUrl) {
        this.latestVersionUrl = latestVersionUrl;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getFirmwareUrl() {
        return firmwareUrl;
    }

    public void setFirmwareUrl(String firmwareUrl) {
        this.firmwareUrl = firmwareUrl;
    }

}
