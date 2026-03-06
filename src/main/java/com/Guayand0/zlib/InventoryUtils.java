package com.Guayand0.zlib;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class InventoryUtils {

    /**
     * Gets the selected GUI language file name from the plugin config.
     *
     * @param plugin The plugin instance.
     * @return The file name of the selected language.
     */
    public String getGuiLangFile(Plugin plugin) {
        String selectedLanguage = plugin.getConfig().getString("config.gui-language", "en");
        return selectedLanguage + ".yml";
    }

    /**
     * Loads a GUI configuration file located within the plugin's data folder.
     * If the file doesn't exist, it will be created from the plugin's resources.
     *
     * @param plugin The plugin instance.
     * @param filePath The relative path to the GUI config file.
     * @return The loaded FileConfiguration object.
     */
    public FileConfiguration getGuiConfig(Plugin plugin, String filePath) {
        File configFile = new File(plugin.getDataFolder(), filePath);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(filePath, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }
}
