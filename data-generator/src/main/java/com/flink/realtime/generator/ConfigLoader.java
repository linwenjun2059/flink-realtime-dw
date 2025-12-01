package com.flink.realtime.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件加载器
 */
public class ConfigLoader {
    private static Properties props = new Properties();

    static {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("generator.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static double getDouble(String key, double defaultValue) {
        String value = props.getProperty(key);
        return value != null ? Double.parseDouble(value) : defaultValue;
    }
}
