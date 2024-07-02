package util;

public class Logger {
    public static void log(String message) {
        System.out.println(message);
    }

    public static void error(String message, Throwable throwable) {
        System.err.println(message);
        throwable.printStackTrace(System.err);
    }
}
