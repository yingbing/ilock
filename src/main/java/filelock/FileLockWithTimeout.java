package filelock;

import util.Logger;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;

public class FileLockWithTimeout {
    private FileChannel fileChannel;
    private RandomAccessFile raf;
    private FileLock lock = null;
    private final String lockFilePath;
    private final long timeout;

    public FileLockWithTimeout(String baseLockPath, String key, long timeout, TimeUnit unit) {
        this.lockFilePath = baseLockPath + "/" + key + ".lock";
        this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
    }

    public synchronized boolean acquireLock() {
        long startTime = System.currentTimeMillis();
        initResources();
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                lock = fileChannel.tryLock();
                if (lock != null) {
                    return true;
                }
            } catch (OverlappingFileLockException e) {
                Logger.log("Lock already acquired by another thread or process.");
            } catch (Exception e) {
                Logger.error("Failed to acquire lock: ", e);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.log("Lock acquisition interrupted.");
                break;
            }
        }
        closeResources();
        return false;
    }

    public synchronized void releaseLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (Exception e) {
            Logger.error("Failed to release lock: ", e);
        } finally {
            closeResources();
        }
    }

    private void initResources() {
        try {
            if (raf == null || fileChannel == null) {
                raf = new RandomAccessFile(lockFilePath, "rw");
                fileChannel = raf.getChannel();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize lock resources: " + e.getMessage(), e);
        }
    }

    private void closeResources() {
        try {
            if (fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
            if (raf != null) {
                raf.close();
                raf = null;
            }
        } catch (Exception e) {
            Logger.error("Failed to close lock resources: ", e);
        }
    }
}
