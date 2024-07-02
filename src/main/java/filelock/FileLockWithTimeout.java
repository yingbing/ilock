package filelock;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;

public class FileLockWithTimeout {
    private final FileChannel fileChannel;
    private final RandomAccessFile raf;
    private FileLock lock = null;
    private final long timeout;

    public FileLockWithTimeout(String lockPath, long timeout, TimeUnit unit) throws Exception {
        this.raf = new RandomAccessFile(lockPath, "rw");
        this.fileChannel = raf.getChannel();
        this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
    }

    public synchronized boolean acquireLock() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                lock = fileChannel.tryLock();
                if (lock != null) {
                    return true; // 锁成功获取
                }
            } catch (OverlappingFileLockException e) {
                // 文件锁冲突，当前进程的其他线程或文件通道已持有锁
            } catch (Exception e) {
                System.out.println("Failed to acquire lock: " + e.getMessage());
                return false;
            }
            // 暂停一段时间后再尝试，避免过度消耗CPU
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public synchronized void releaseLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
                lock = null;
            }
            raf.close();
            fileChannel.close();
        } catch (Exception e) {
            System.out.println("Failed to release lock: " + e.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        releaseLock();  // 确保在对象被垃圾回收时释放资源
        super.finalize();
    }
}
