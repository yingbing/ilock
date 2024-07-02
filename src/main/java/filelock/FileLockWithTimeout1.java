package filelock;

import util.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FileLockWithTimeout1 {
    private FileChannel fileChannel;
    private RandomAccessFile raf;
    private FileLock lock = null;
    private final String lockFilePath;
    private final long timeout;
    private final long heartbeatInterval;  // Interval to update heartbeat
    private final long maxHeartbeatGap;    // Maximum allowed gap in heartbeat

    public FileLockWithTimeout1(String baseLockPath, String key, long timeout, TimeUnit unit, long heartbeatInterval, long maxHeartbeatGap) {
        this.lockFilePath = baseLockPath + "/" + key + ".lock";
        this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
        this.heartbeatInterval = heartbeatInterval;
        this.maxHeartbeatGap = maxHeartbeatGap;
    }

    public synchronized boolean acquireLock() throws IOException {
        long startTime = System.currentTimeMillis();
        initResources();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (checkHeartbeat()) {
                try {
                    lock = fileChannel.tryLock();
                    if (lock != null) {
                        updateHeartbeat();  // Update heartbeat upon acquiring the lock
                        return true;
                    }
                } catch (OverlappingFileLockException e) {
                    Logger.log("Lock already acquired by another thread or process.");
                } catch (Exception e) {
                    Logger.error("Failed to acquire lock: ", e);
                    break;
                }
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
                updateHeartbeat(true);  // Clear heartbeat on releasing lock
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

    private void updateHeartbeat() throws IOException {
        updateHeartbeat(false);
    }

    private void updateHeartbeat(boolean clear) throws IOException {
        if (clear) {
            Files.write(Paths.get(lockFilePath), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            String timestamp = String.valueOf(System.currentTimeMillis());
            Files.write(Paths.get(lockFilePath), timestamp.getBytes(), StandardOpenOption.CREATE);
        }
    }

    private boolean checkHeartbeat() throws IOException {
        if (!Files.exists(Paths.get(lockFilePath))) {
            return true;
        }
        String content = new String(Files.readAllBytes(Paths.get(lockFilePath)));
        long lastBeat = Long.parseLong(content);
        return (System.currentTimeMillis() - lastBeat > maxHeartbeatGap);
    }
}
