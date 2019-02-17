package tgfx.hardwarePlatforms;

/**
 * HardwarePlatform
 * holds the hardware platform information
 */
public class HardwarePlatform {

    private String platformName;
    private Double minimalBuildVersion;
    private String latestVersionUrl;
    private String manufacturer;
    private String firmwareUrl;
    private int hardwarePlatformVersion;
    private boolean isUpgradeable;

    /**
     * Hardware platform constructor
     */
    public HardwarePlatform() {
    }


    /**
     * get the hardware platform version
     * @return platform version
     */
    public int getHardwarePlatformVersion() {
        return hardwarePlatformVersion;
    }


    /**
     * set the hardware platform version
     * @param hardwarePlatformVersion platform version
     */
    public void setHardwarePlatformVersion(int hardwarePlatformVersion) {
        this.hardwarePlatformVersion = hardwarePlatformVersion;
    }


    /**
     * is this hardware platform upgradable
     * @return is upgradable
     */
    public boolean isIsUpgradeable() {
        return isUpgradeable;
    }


    /**
     * set is this hardware platform upgradable
     * @param isUpgradeable is upgradable
     */
    public void setIsUpgradeable(boolean isUpgradeable) {
        this.isUpgradeable = isUpgradeable;
    }


    /**
     * get hardware platform name
     * @return platform name
     */
    public String getPlatformName() {
        return platformName;
    }


    /**
     * set hardware platform name
     * @param platformName platform name
     */
    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }


    /**
     * get the minimal build version
     * @return minimal build version
     */
    public Double getMinimalBuildVersion() {
        return minimalBuildVersion;
    }


    /**
     * set the minimum build version
     * @param minimalBuildVersion minimal build version
     */
    public void setMinimalBuildVersion(Double minimalBuildVersion) {
        this.minimalBuildVersion = minimalBuildVersion;
    }


    /**
     * get the url to the latest version
     * @return latest version url string
     */
    public String getLatestVersionUrl() {
        return latestVersionUrl;
    }


    /**
     * set the url to the latest version
     * @param latestVersionUrl latest version url string
     */
    public void setLatestVersionUrl(String latestVersionUrl) {
        this.latestVersionUrl = latestVersionUrl;
    }


    /**
     * get the platform manufacturer
     * @return platform manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }


    /**
     * set the platform manufacturer
     * @param manufacturer platform manufacturer
     */
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }


    /**
     * get the firmware url
     * @return firmware url string
     */
    public String getFirmwareUrl() {
        return firmwareUrl;
    }


    /**
     * set the firmware url
     * @param firmwareUrl firmware url string
     */
    public void setFirmwareUrl(String firmwareUrl) {
        this.firmwareUrl = firmwareUrl;
    }

}
