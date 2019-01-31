package tgfx.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The
 * <code>QueueUsingTimer</code> class is a class that sets a timer and when the
 * timer expires, adds an entry to a queue
 *
 * @see AsyncTimer
 *
 */
public class QueueUsingTimer<T> extends Thread {

    private static final Logger logger = LogManager.getLogger();

    private final QueuedTimerable<T> callback;
    private final long naptime;
    private boolean report_timeout = true;
    private final T makeEntryOf;

    /**
     * construct an QueueUsingTimer
     *
     * @param nap milliseconds to nap
     * @param cb a QueuedTimerable for callback access to semaphore
     */
    public QueueUsingTimer(long nap, QueuedTimerable<T> cb, T entry) {
        callback = cb;
        naptime = nap;
        makeEntryOf = entry;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(naptime);
            if (report_timeout) {
                callback.addToQueue(makeEntryOf);
            }
        } catch (InterruptedException ex) {
            logger.error("sleep interupted", ex);
        }
    }

    public void disarm() {
        report_timeout = false;
    }
}
