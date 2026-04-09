package pc.crawler;

import java.util.concurrent.atomic.AtomicInteger;

public class ActivityMonitor {
    private final AtomicInteger counter;

    public ActivityMonitor() {
        this.counter = new AtomicInteger(0);
    }

    public void taskStarted() {
        counter.incrementAndGet();
    }

    public synchronized void taskCompleted() {
        int remaining = counter.decrementAndGet();
        if (remaining == 0) {
            notifyAll();
        }
    }

    public synchronized void awaitCompletion() throws InterruptedException {
        while (counter.get() != 0) {
            wait();
        }
    }
}
