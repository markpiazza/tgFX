package tgfx.utility;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncTimerTest {
    
    public AsyncTimerTest() {
    }

    @Test
    public void testRun() {
        System.out.println("run");
        TimableTester tt = new TimableTester();
        AsyncTimer instance = new AsyncTimer(5000, tt);
        instance.start();
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < 1000*1000; i++) {
            if (tt.timerKicked.get()) {
                System.out.println("timer kicked off " + i);
                break;
            }
            if (i % 50 == 1) {
                System.out.print("");
            }
            if (i % 1000 == 1) {
                System.out.println(i);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                System.err.println(ex.getMessage());
            }
        }
        long stop = System.currentTimeMillis();
        System.out.printf(" delta = %d kicked: %b", stop-start, tt.getTimeSemaphore().get());
        
    }
    class TimableTester implements Timeable {
        private AtomicBoolean timerKicked = new AtomicBoolean(false);  
        @Override
        public AtomicBoolean getTimeSemaphore() {
            return timerKicked;
        }
    }
}