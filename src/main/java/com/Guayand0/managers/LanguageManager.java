package com.Guayand0.managers;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class LanguageManager {

    private final MineBank plugin;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    private FileConfiguration languageConfig;
    private FileConfiguration guiConfig;

    public LanguageManager(MineBank plugin) {
        this.plugin = plugin;
        loadMessageFiles();
        loadGuiFiles();
    }

    // -------------- Messages -------------- //
    private void loadMessageFiles() {
        File langDir = new File(plugin.getDataFolder(), "messages");
        if (!langDir.exists()) langDir.mkdirs();

        String[] defaultLangFiles = {"en.yml", "es.yml"};
        for (String fileName : defaultLangFiles) {
            File outFile = new File(langDir, fileName);
            if (!outFile.exists()) {
                try (InputStream resource = plugin.getResource("messages/" + fileName)) {
                    if (resource != null) Files.copy(resource, outFile.toPath());
                    else outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                }
            }
        }
        reloadMessagesConfig();
    }

    public void reloadMessages() {
        reloadMessagesConfig();
    }

    public void reloadMessagesConfig() {
        String selectedLanguage = plugin.getConfig().getString("config.message-language", "en");
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        File selectedLangFile = new File(messagesDir, selectedLanguage + ".yml");

        if (!selectedLangFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &4Language " + selectedLanguage + ".yml does not exist in messages folder."));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &4Using en.yml by default."));
            selectedLangFile = new File(messagesDir, "en.yml");
        }

        languageConfig = YamlConfiguration.loadConfiguration(selectedLangFile);
    }

    public List<String> getAllMessage(String path) {
        List<String> messageList = languageConfig.getStringList(path);

        if (!messageList.isEmpty()) return messageList;

        // Si no es una lista, devolvemos una lista con un solo mensaje
        String message = languageConfig.getString(path, MU.getColoredText(plugin.prefix + " &4Message &c" + path + " &4not found."));
        return Arrays.asList(message);
    }

    // -------------- Gui -------------- //
    private void loadGuiFiles() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) guiDir.mkdirs();

        String[] defaultGuiFiles = {"en.yml", "es.yml"};
        for (String fileName : defaultGuiFiles) {
            File outFile = new File(guiDir, fileName);
            if (!outFile.exists()) {
                try (InputStream resource = plugin.getResource("gui/" + fileName)) {
                    if (resource != null) Files.copy(resource, outFile.toPath());
                    else outFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                }
            }
        }
        reloadGuiConfig();
    }

    public void reloadGui() {
        reloadGuiConfig();
    }

    public void reloadGuiConfig() {
        String selectedGui = plugin.getConfig().getString("config.gui-language", "en");
        File guiDir = new File(plugin.getDataFolder(), "gui");
        File selectedGuiFile = new File(guiDir, selectedGui + ".yml");

        if (!selectedGuiFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &4Gui " + selectedGui + ".yml does not exist in gui folder."));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &4Using en.yml by default."));
            selectedGuiFile = new File(guiDir, "en.yml");
        }

        guiConfig = YamlConfiguration.loadConfiguration(selectedGuiFile);
    }
}
