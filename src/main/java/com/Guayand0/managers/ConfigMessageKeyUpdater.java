package com.Guayand0.managers;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigMessageKeyUpdater {

    private final MineBank plugin;
    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public ConfigMessageKeyUpdater(MineBank plugin) {
        this.plugin = plugin;
    }

    public boolean syncConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return true;
        }

        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        FileConfiguration defaultConfig = loadDefaultConfig("config.yml");
        if (defaultConfig == null) return false;

        boolean changed = addMissingKeys(currentConfig, defaultConfig);
        if (changed) {
            try {
                currentConfig.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        }

        return changed;
    }

    public boolean syncMessages() {
        boolean changed = false;
        String[] defaultLangFiles = {"en.yml", "es.yml"};

        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) messagesDir.mkdirs();

        for (String fileName : defaultLangFiles) {
            String resourcePath = "messages/" + fileName;
            File outFile = new File(messagesDir, fileName);

            if (!outFile.exists()) {
                copyResource(resourcePath, outFile);
            }

            FileConfiguration defaults = loadDefaultConfig(resourcePath);
            if (defaults == null) continue;

            FileConfiguration current = YamlConfiguration.loadConfiguration(outFile);
            boolean fileChanged = addMissingKeys(current, defaults);
            if (fileChanged) {
                try {
                    current.save(outFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                }
            }
            changed = changed || fileChanged;
        }

        return changed;
    }

    private FileConfiguration loadDefaultConfig(String resourcePath) {
        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            return null;
        }
    }

    private void copyResource(String resourcePath, File outFile) {
        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource != null) {
                outFile.getParentFile().mkdirs();
                java.nio.file.Files.copy(resource, outFile.toPath());
            } else {
                outFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
        }
    }

    private boolean addMissingKeys(FileConfiguration target, FileConfiguration defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }
}
