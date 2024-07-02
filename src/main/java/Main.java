import taskscheduler.TaskScheduler;
import config.ConfigManager;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        ConfigManager.loadProperties("config.properties");

        try {
            TaskScheduler scheduler = new TaskScheduler(
                    ConfigManager.getProperty("lock.path"),
                    "uniqueTaskKey",
                    Long.parseLong(ConfigManager.getProperty("lock.timeout")),
                    TimeUnit.MILLISECONDS,
                    Integer.parseInt(ConfigManager.getProperty("max.retries")),
                    Long.parseLong(ConfigManager.getProperty("retry.delay"))
            );
            scheduler.scheduleTask(() -> {
                System.out.println("Task executed at: " + System.currentTimeMillis());
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Simulated task failure");
                }
            }, 0, 10, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stopScheduler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
