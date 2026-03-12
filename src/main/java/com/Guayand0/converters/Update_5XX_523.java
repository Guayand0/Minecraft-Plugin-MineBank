package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class Update_5XX_523 {

    private final MineBank plugin;
    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public Update_5XX_523(MineBank plugin) {
        this.plugin = plugin;
        splitGuiFiles();
    }

    // gui/<lang>.yml -> gui/<lang>/<gui>.yml
    public void splitGuiFiles() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists() || !guiDir.isDirectory()) return;

        File[] legacyFiles = guiDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".yml") && !name.toLowerCase().startsWith("old-")
        );

        if (legacyFiles == null || legacyFiles.length == 0) return;

        Map<String, FileConfiguration> languageConfigs = new HashMap<>();

        for (File legacyFile : legacyFiles) {
            String fileName = legacyFile.getName();
            File backupFile = new File(guiDir, "old-" + fileName);
            try {
                Files.copy(legacyFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of gui/" + fileName + " -> gui/old-" + fileName, plugin.placeholders));
            } catch (IOException e) {
                if (GV.getBoolean(plugin, "exception.save", true)) {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                }
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(
                        plugin.prefix + " &cError creating backup of gui/" + fileName,
                        plugin.placeholders
                ));
                continue;
            }

            String language = fileName.substring(0, fileName.length() - 4);
            languageConfigs.put(language, YamlConfiguration.loadConfiguration(legacyFile));
        }

        clearGuiFolderExceptBackups(guiDir);

        for (Map.Entry<String, FileConfiguration> entry : languageConfigs.entrySet()) {
            String language = entry.getKey();
            FileConfiguration legacyConfig = entry.getValue();

            writeGuiFile(guiDir, language, "main", legacyConfig);
            writeGuiFile(guiDir, language, "transactions", legacyConfig);
        }
    }

    private void writeGuiFile(File guiDir, String language, String guiId, FileConfiguration legacyConfig) {
        String sourcePath = "gui." + guiId;
        ConfigurationSection sourceSection = legacyConfig.getConfigurationSection(sourcePath);
        if (sourceSection == null) return;

        File langDir = new File(guiDir, language);
        if (!langDir.exists()) langDir.mkdirs();

        File outFile = new File(langDir, guiId + ".yml");
        YamlConfiguration outConfig = new YamlConfiguration();
        ConfigurationSection targetSection = outConfig.createSection(sourcePath);
        copySection(sourceSection, targetSection);

        try {
            outConfig.save(outFile);
        } catch (IOException e) {
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(
                    plugin.prefix + " &cError writing gui/" + language + "/" + guiId + ".yml",
                    plugin.placeholders
            ));
        }
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection childSource = source.getConfigurationSection(key);
                ConfigurationSection childTarget = target.createSection(key);
                if (childSource != null) copySection(childSource, childTarget);
            } else {
                target.set(key, value);
            }
        }
    }

    private void clearGuiFolderExceptBackups(File guiDir) {
        File[] allEntries = guiDir.listFiles();
        if (allEntries == null) return;

        for (File entry : allEntries) {
            String lowerName = entry.getName().toLowerCase();
            if (entry.isFile() && lowerName.startsWith("old-") && lowerName.endsWith(".yml")) {
                continue;
            }
            deleteRecursively(entry);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(
                    plugin.prefix + " &cCould not delete " + file.getPath(),
                    plugin.placeholders
            ));
        }
    }
}
