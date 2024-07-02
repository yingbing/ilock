package taskscheduler;

import filelock.FileLockWithTimeout;
import filelock.FileLockWithTimeout1;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private final ScheduledExecutorService scheduler;
    private final FileLockWithTimeout lock;
    private final int maxRetries;
    private final long retryDelay;

    public TaskScheduler(String baseLockPath, String key, long timeout, TimeUnit unit, int maxRetries, long retryDelay) throws Exception {
        this.lock = new FileLockWithTimeout(baseLockPath, key, timeout, unit);
//        this.lock = new FileLockWithTimeout1(baseLockPath, key, timeout, unit, 2000, 4000);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    public void scheduleTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runWithRetries(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Task execution interrupted.");
            }
        }, initialDelay, period, unit);
    }

    private void runWithRetries(Runnable task) throws InterruptedException {
        int attempts = 0;
        while (attempts < maxRetries) {
            if (lock.acquireLock()) {
                try {
                    task.run();
                    return; // Task executed successfully, exit the loop
                } catch (Exception e) {
                    System.out.println("Task execution failed: " + e.getMessage());
                    // Task failed, retry after delay
                } finally {
                    lock.releaseLock();
                }
            } else {
                System.out.println("Failed to acquire lock, will retry...");
            }
            attempts++;
            Thread.sleep(retryDelay);
        }
        System.out.println("Task failed after " + maxRetries + " retries.");
    }

    public void stopScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
