package tgfx.utility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The <code>Timeable</code> interface declares that implementing classes
 * can be using an AsyncTimer
 * @see AsyncTimer
 *
 */
public interface Timeable {
    AtomicBoolean getTimeSemaphore();
}
