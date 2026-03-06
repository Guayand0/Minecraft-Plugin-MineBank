package com.Guayand0.managers;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class FileManager {

    private final MineBank plugin;

    public File banksFile;
    public File interestsDataFile;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public FileManager(MineBank plugin) {
        this.plugin = plugin;
        loadDataFiles();
    }

    private void loadDataFiles() {

        File pluginFolder = plugin.getDataFolder();

        File playerDataFolder = new File(pluginFolder, "data/player_data");
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();

        File dataFolder = new File(pluginFolder, "data");
        File bankDataFolder = new File(pluginFolder, "data/bank_data");
        if (!bankDataFolder.exists()) bankDataFolder.mkdirs();

        // Crear interests_data.json
        interestsDataFile = new File(dataFolder, "interests_data.json");
        createFileIfNotExists(interestsDataFile, "data/interests_data.json");

        // Crear banks.yml
        banksFile = new File(dataFolder, "banks.yml");
        createFileIfNotExists(banksFile, "data/banks.yml");

        // Cargar banks.yml
        File banksYml = new File(dataFolder, "banks.yml");
        FileConfiguration banksConfig = YamlConfiguration.loadConfiguration(banksYml);

        // Crear bank-priority si no existe
        ConfigurationSection section = banksConfig.getConfigurationSection("bank-priority");
        if (section == null) {
            section = banksConfig.createSection("bank-priority");
            section.set("1", "User");
            section.set("2", "Vip");
            section.set("3", "Mod");

            try {
                banksConfig.save(banksFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bukkit.getConsoleSender().sendMessage(
                    MU.getColoredText(plugin.prefix + " &e`bank-priority` created by default in banks.yml file")
            );
        }

        // Bancos permitidos
        Set<String> allowedBanks = new HashSet<>();
        allowedBanks.add("User");
        allowedBanks.add("Vip");
        allowedBanks.add("Mod");

        int expectedKey = 1;

        while (true) {
            String key = String.valueOf(expectedKey);

            // Si falta número → se corta todo
            if (!section.contains(key)) break;

            String bankName = section.getString(key);
            if (bankName == null || bankName.isEmpty()) {
                expectedKey++;
                continue;
            }

            // Si no permitido → ignorar pero NO cortar
            if (!allowedBanks.contains(bankName)) {
                expectedKey++;
                continue;
            }

            File bankFile = new File(bankDataFolder, bankName + ".json");
            createFileIfNotExists(bankFile, "data/bank_data/" + bankName + ".json");

            expectedKey++;
        }
    }

    private void createFileIfNotExists(File file, String resourcePath) {
        if (!file.exists()) {
            try (InputStream resource = plugin.getResource(resourcePath)) {
                if (resource != null) {
                    Files.copy(resource, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        }
    }
}
