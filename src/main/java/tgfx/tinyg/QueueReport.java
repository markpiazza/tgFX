package tgfx.tinyg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * QueueReport POJO
 *
 */
public class QueueReport {
    private static final Logger logger = LogManager.getLogger();

    private int availableBufferSize = 24;
    private int added = 0;
    private int removed = 0;

    public QueueReport() {
    }

    synchronized void parse(JSONObject js) throws JSONException {
        logger.info("QUEUE REPORT");
        JSONArray jsa = js.getJSONArray("qr");
        setAvailableBufferSize(jsa.getInt(0));
        setAdded(jsa.getInt(1));
        setRemoved(jsa.getInt(2));
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getRemoved() {
        return removed;
    }

    private void setRemoved(int removed) {
        this.removed = removed;
    }

    public int getAvailableBufferSize() {
        return availableBufferSize;
    }

    private void setAvailableBufferSize(int pba) {
        this.availableBufferSize = pba;
    }

}