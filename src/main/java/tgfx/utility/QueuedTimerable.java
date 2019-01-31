package tgfx.utility;

/**
 * The <code>QueuedTimerable</code> interface declares that the object contains a
 * queue of T that is used by the QueueTimerable class
 * @see QueuedTimerable
 *
 */
public interface QueuedTimerable<T> {
    void addToQueue(T t);
    
}
