package carlvbn.raytracing.rendering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadPool {
    private final BlockingQueue<Runnable> queue;
    private final Thread[] workers;

    public ThreadPool(int workerCount, int queueCapacity) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be > 0");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be > 0");
        }
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.workers = new Thread[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Thread(this::workerLoop, "renderer-pool-" + i);
            workers[i].setDaemon(true);
            workers[i].start();
        }
    }

    public void execute(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while enqueueing task", e);
        }
    }

    private void workerLoop() {
        while (true) {
            try {
                Runnable task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
