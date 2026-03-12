package com.Guayand0.zlib;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class InventoryUtils {

    public String getGuiLangFile(Plugin plugin) {
        return plugin.getConfig().getString("config.gui-language", "en");
    }

    /**
     * Loads a GUI configuration file by gui id and selected language.
     * Format: gui/<lang>/<guiId>.yml
     */
    public FileConfiguration getGuiConfig(Plugin plugin, String guiId) {
        String selectedLanguage = getGuiLangFile(plugin);
        String filePath = "gui/" + selectedLanguage + "/" + guiId + ".yml";
        File configFile = new File(plugin.getDataFolder(), filePath);

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                plugin.saveResource(filePath, false);
            } catch (IllegalArgumentException ignored) {
                // If language resource does not exist, use english resource for this gui.
            }
        }

        if (!configFile.exists()) {
            String fallbackPath = "gui/en/" + guiId + ".yml";
            File fallbackConfig = new File(plugin.getDataFolder(), fallbackPath);
            if (!fallbackConfig.exists()) {
                fallbackConfig.getParentFile().mkdirs();
                try {
                    plugin.saveResource(fallbackPath, false);
                } catch (IllegalArgumentException ignored) {}
            }
            return YamlConfiguration.loadConfiguration(fallbackConfig);
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }
}
