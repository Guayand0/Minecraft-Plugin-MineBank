package com.Guayand0.zlib;

import org.bukkit.plugin.Plugin;

public class GetValues {

    /**
     * Gets a boolean value from the config.
     *
     * @param plugin       plugin instance
     * @param key          config path
     * @param defaultValue default value if not found
     * @return configured boolean value
     */
    public boolean getBoolean(Plugin plugin, String key, boolean defaultValue) {
        return plugin.getConfig().getBoolean(key, defaultValue);
    }

    public boolean getBoolean(Plugin plugin, String key) {
        return plugin.getConfig().getBoolean(key);
    }

    /**
     * Gets a String value from the config.
     *
     * @param plugin       plugin instance
     * @param key          config path
     * @param defaultValue default value if not found
     * @return configured String value
     */
    public String getString(Plugin plugin, String key, String defaultValue) {
        return plugin.getConfig().getString(key, defaultValue);
    }

    public String getString(Plugin plugin, String key) {
        return plugin.getConfig().getString(key);
    }

    /**
     * Gets an integer value from the config.
     *
     * @param plugin       plugin instance
     * @param key          config path
     * @param defaultValue default value if not found
     * @return configured integer value
     */
    public int getInt(Plugin plugin, String key, int defaultValue) {
        return plugin.getConfig().getInt(key, defaultValue);
    }

    public int getInt(Plugin plugin, String key) {
        return plugin.getConfig().getInt(key);
    }

    /**
     * Gets a double value from the config.
     *
     * @param plugin       plugin instance
     * @param key          config path
     * @param defaultValue default value if not found
     * @return configured double value
     */
    public double getDouble(Plugin plugin, String key, double defaultValue) {
        return plugin.getConfig().getDouble(key, defaultValue);
    }

    public double getDouble(Plugin plugin, String key) {
        return plugin.getConfig().getDouble(key);
    }
}