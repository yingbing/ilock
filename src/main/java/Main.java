import taskscheduler.TaskScheduler;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            TaskScheduler scheduler1 = new TaskScheduler("locks", "task1", 5000, TimeUnit.MILLISECONDS, 3, 1000);
            scheduler1.scheduleTask(() -> {
                System.out.println("Task 1 executed at: " + System.currentTimeMillis());
                // Simulate a task that might fail
                if (Math.random() > 0.7) {
                    throw new RuntimeException("Simulated task failure");
                }
            }, 0, 10, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(scheduler1::stopScheduler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
