package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Properties properties = new Properties();

    public static void loadProperties(String resourcePath) {
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Cannot find configuration file: " + resourcePath);
            }
            properties.load(stream);
        } catch (IOException e) {
            System.err.println("Failed to load properties from " + resourcePath);
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
