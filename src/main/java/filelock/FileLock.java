package filelock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileLock {
    private final String lockPath;

    public FileLock(String baseLockPath, String key) {
        this.lockPath = baseLockPath + File.separator + key + ".lock";
        File lockFile = new File(lockPath);
        lockFile.getParentFile().mkdirs();  // 确保锁文件的目录存在
    }

    public synchronized boolean acquireLock() {
        try {
            return Files.write(Paths.get(lockPath), new byte[0], StandardOpenOption.CREATE_NEW).toFile().exists();
        } catch (IOException e) {
            System.out.println("Lock acquisition failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean releaseLock() {
        try {
            return Files.deleteIfExists(Paths.get(lockPath));
        } catch (IOException e) {
            System.out.println("Lock release failed: " + e.getMessage());
            return false;
        }
    }
}
