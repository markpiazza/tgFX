/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.tinyg;

import org.json.JSONObject;

/**
 * ResponseCommand POJO
 *
 */
public class ResponseCommand {

    private String settingParent;
    private String settingKey;
    private String settingValue;
    
    public ResponseCommand(){
        
    }

    public ResponseCommand(String sp, String sk, String sv){
        settingParent = sp;
        settingKey = sk;
        settingValue = sv;
    }

    public String getSettingParent() {
        return settingParent;
    }

    void setSettingParent(String settingParent) {
        this.settingParent = settingParent;
    }

    public String getSettingKey() {
        return settingKey;
    }

    void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
    
    public JSONObject buildJsonObject() {
        return new JSONObject("{\"" + this.getSettingParent() + "\"" +
                ":{\"" + this.getSettingKey() + "\":" + this.getSettingValue() + "}}");
    }
}