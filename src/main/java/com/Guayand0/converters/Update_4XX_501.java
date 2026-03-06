package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Update_4XX_501 {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    // 4.x.x a 5.0.1
    public Update_4XX_501(MineBank plugin) {
        this.plugin = plugin;
        convertPlayerBankYamlToJson();
        convertBanksYamlToJson();
        convertGuiFolder();
        convertMessagesFolder();
    }

    // bank.yml -> bank/player_data.json
    public void convertPlayerBankYamlToJson() {
        try {
            File yamlFile = new File(plugin.getDataFolder(), "bank.yml");
            File backupFile = new File(plugin.getDataFolder(), "old-bank.yml");

            if (!yamlFile.exists()) return;

            // Crear una copia de seguridad
            try {
                Files.copy(yamlFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of bank.yml -> old-bank.yml", plugin.placeholders));
            } catch (IOException e) {
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError creating the backup of bank.yml: " + e.getMessage(), plugin.placeholders));
                return;
            }

            FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);

            // Si bank.yml está vacío
            if (!yamlConfig.isConfigurationSection("bank") || yamlConfig.getConfigurationSection("bank").getKeys(false).isEmpty()) {

                if (yamlFile.renameTo(backupFile)) {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &ebank.yml was empty. Renamed to old-bank.yml", plugin.placeholders));
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cCould not rename empty bank.yml", plugin.placeholders));
                }
                return;
            }

            JsonArray playersArray = new JsonArray();

            for (String uuid : yamlConfig.getConfigurationSection("bank").getKeys(false)) {
                String playerName = yamlConfig.getConfigurationSection("bank." + uuid).getKeys(false).iterator().next();

                JsonObject bankData = new JsonObject();
                bankData.addProperty("name", capitalizeFirstLetter(yamlConfig.getString("bank." + uuid + "." + playerName + ".bank-name")));
                bankData.addProperty("level", yamlConfig.getInt("bank." + uuid + "." + playerName + ".level"));
                bankData.addProperty("balance", yamlConfig.getInt("bank." + uuid + "." + playerName + ".balance"));
                bankData.addProperty("offline_accrued_profit", 0);
                bankData.addProperty("offline_profit_times", 0);

                JsonArray bankArray = new JsonArray();
                bankArray.add(bankData);

                JsonObject playerData = new JsonObject();
                playerData.addProperty("name", playerName);
                playerData.addProperty("UUID", uuid);
                playerData.add("bank", bankArray);

                playersArray.add(playerData);
            }

            JsonObject jsonOutput = new JsonObject();
            jsonOutput.add("player", playersArray);

            File jsonFile = new File(plugin.getDataFolder(), "bank/player_data.json");
            jsonFile.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(jsonFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(jsonOutput));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eData successfully converted from config.yml to bank/player_data.json", plugin.placeholders));

                // Si la conversión fue exitosa, borrar bank.yml
                if (yamlFile.delete()) Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &ePrevious bank.yml successfully deleted after conversion", plugin.placeholders));
                else Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cCould not delete bank.yml, delete it manually", plugin.placeholders));

            } catch (IOException e) {
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError writing in the JSON file: " + e.getMessage(), plugin.placeholders));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + "&cAn error occurred while processing the player data migration", plugin.placeholders));
        }
    }

    // config.yml -> bank/banks.json
    public void convertBanksYamlToJson() {
        try {
            File yamlFile = new File(plugin.getDataFolder(), "config.yml");
            if (!yamlFile.exists()) return;

            FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
            if (!yamlConfig.contains("bank")) return; // No hay datos de bancos

            JsonObject banksObject = new JsonObject();
            boolean hasLevels = false;

            for (String bankName : yamlConfig.getConfigurationSection("bank").getKeys(false)) {
                if (!yamlConfig.contains("bank." + bankName + ".level")) continue;
                if (yamlConfig.getConfigurationSection("bank." + bankName + ".level") == null) continue;

                JsonArray levelsArray = new JsonArray();
                int maxLevel = yamlConfig.getConfigurationSection("bank." + bankName + ".level").getKeys(false).size();

                for (String levelKey : yamlConfig.getConfigurationSection("bank." + bankName + ".level").getKeys(false)) {
                    String[] values = yamlConfig.getString("bank." + bankName + ".level." + levelKey).split(";");
                    if (values.length < 2) continue;

                    int maxBalance = Integer.parseInt(values[0]);
                    int upgradeCost = Integer.parseInt(values[1]);
                    boolean isFinalLevel = Integer.parseInt(levelKey) == maxLevel;

                    JsonObject levelObject = new JsonObject();
                    levelObject.addProperty("level", Integer.parseInt(levelKey));
                    levelObject.addProperty("max_balance", maxBalance);
                    levelObject.addProperty("upgrade_cost", isFinalLevel ? 0 : upgradeCost);
                    levelObject.addProperty("final_level", isFinalLevel);

                    levelsArray.add(levelObject);
                    hasLevels = true;
                }

                if (!levelsArray.isEmpty()) banksObject.add(capitalizeFirstLetter(bankName), levelsArray);
            }

            if (!hasLevels) return; // No hay bancos con niveles, no hacer nada

            File backupFile = new File(plugin.getDataFolder(), "old-config.yml");
            try {
                Files.copy(yamlFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of config.yml -> old-config.yml", plugin.placeholders));
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError creating the backup of config.yml: " + e.getMessage(), plugin.placeholders));
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                return;
            }

            File jsonFile = new File(plugin.getDataFolder(), "bank/banks.json");
            jsonFile.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(jsonFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(banksObject));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBank data successfully converted from config.yml to bank/banks.json", plugin.placeholders));

                if (yamlFile.delete()) Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &ePrevious config.yml successfully deleted after conversion", plugin.placeholders));
                else Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cCould not delete config.yml, delete it manually", plugin.placeholders));

            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError writing in the JSON file: " + e.getMessage(), plugin.placeholders));
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cAn error occurred while processing the player data migration", plugin.placeholders));
        }
    }

    // bankInventory -> gui
    public void convertGuiFolder() {
        try {
            File bankInventoryFolder = new File(plugin.getDataFolder(), "bankInventory");
            File backupFolder = new File(plugin.getDataFolder(), "old-bankInventory");

            // Verificar si la carpeta original existe
            if (!bankInventoryFolder.exists()) return;

            // Crear copia de seguridad
            try {
                if (backupFolder.exists()) {
                    deleteFolder(backupFolder);
                }
                Files.move(bankInventoryFolder.toPath(), backupFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eBackup of bankInventory -> old-bankInventory"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eThe new folder that replaces it is called gui."));

            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cError creating the backup of bankInventory: " + e.getMessage()));
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cAn error occurred while processing the player data migration."));
        }
    }

     // lang -> messages
    public void convertMessagesFolder() {
        try {
            File langFolder = new File(plugin.getDataFolder(), "lang");
            File backupFolder = new File(plugin.getDataFolder(), "old-lang");

            // Verificar si la carpeta original existe
            if (!langFolder.exists()) return;

            // Crear copia de seguridad
            try {
                if (backupFolder.exists()) deleteFolder(backupFolder);
                Files.move(langFolder.toPath(), backupFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eBackup of lang -> old-lang"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eThe new folder that replaces it is called messages."));

            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cError creating the backup of lang: " + e.getMessage()));
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cAn error occurred while processing the player data migration"));
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) for (File file : folder.listFiles()) deleteFolder(file);
        if (folder.delete()) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &ePrevious bankInventory folder successfully deleted after conversion."));
        else Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCould not delete bankInventory folder, delete it manually."));
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
