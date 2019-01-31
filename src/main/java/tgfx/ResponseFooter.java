package tgfx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ResponseFooter
 *
 */
public class ResponseFooter {
    private static final Logger logger = LogManager.getLogger();

    //{"b":{"xvm":12000},"f":[1,0,255,1234]}
    //"f":[<protocol_version>, <status_code>, <input_available>, <checksum>]
    
    private int protocolVersion;
    private static int statusCode = 0;
    private static int rxRecvd = 254;
    private static long checkSum;    

    ResponseFooter(){
    }

    int getRxRecvd() {
        return rxRecvd;
    }

    long getCheckSum() {
        return checkSum;
    }

    void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    void setStatusCode(int statusCode) {
        ResponseFooter.statusCode = statusCode;
    }

    void setRxRecvd(int rxRecvd) {
        ResponseFooter.rxRecvd = rxRecvd;
    }

    void setCheckSum(long checkSum) {
        ResponseFooter.checkSum = checkSum;
    }

    int getProtocolVersion() {
        return protocolVersion;
    }
    
    int getBufferAvailable() {
        return rxRecvd;
    }
  
    static int getStatusCode() {
        return statusCode;
    }

//    public void parseResponseFooter(JsonNode responseNodeObject){
//        protocolVersion = Integer.valueOf(responseNodeObject.getNode("f").getElements().get(0).getText());
//        statusCode = Integer.valueOf(responseNodeObject.getNode("f").getElements().get(1).getText());
//        if(statusCode != 0 && statusCode !=60 ){  //60 is a zero length move.
//            TinygDriver.getInstance().serialWriter.setThrottled(true);
//        }
//        rxRecvd = Integer.valueOf(responseNodeObject.getNode("f").getElements().get(2).getText());
//        checkSum = Long.valueOf(responseNodeObject.getNode("f").getElements().get(3).getText());
//    }    
}
