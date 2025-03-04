package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BanksConverter {

    private final MineBank plugin;
    private final MessageUtils MU = new MessageUtils();

    public BanksConverter(MineBank plugin) {
        this.plugin = plugin;
    }

    // 4.x.x a 5.0.1
    public void convertYamlToJson() {
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
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of the config.yml file to old-config.yml", plugin.placeholders));
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError creating the backup of config.yml: " + e.getMessage(), plugin.placeholders));
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
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
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cAn error occurred while processing the player data migration", plugin.placeholders));
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // 5.0.1 a 5.1.1
    public void convertJsonToJsonLeveled() {
        File jsonFile = new File(plugin.getDataFolder(), "bank/banks.json");
        File backupFile = new File(plugin.getDataFolder(), "bank/old-banks.json");
        jsonFile.getParentFile().mkdirs();

        if (!jsonFile.exists()) return;

        try {
            // Read the original JSON file
            String content = new String(Files.readAllBytes(jsonFile.toPath()));
            Gson gson = new Gson();
            JsonObject originalData = gson.fromJson(content, JsonObject.class);
            JsonObject convertedData = new JsonObject();

            boolean hasLevel = true;

            for (String key : originalData.keySet()) {
                JsonArray levelsArray = originalData.getAsJsonArray(key);

                for (JsonElement element : levelsArray) {
                    JsonObject obj = element.getAsJsonObject();

                    if (obj.has("level")) {
                        hasLevel = false;
                    } else if (obj.has("levels")) {
                        hasLevel = true;
                        break;  // Si encontramos "levels", no es necesario continuar
                    }
                }
            }

            if (hasLevel) return;

            // Create a backup
            Files.copy(jsonFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of the banks.json file to old-banks.json", plugin.placeholders));

            for (String key : originalData.keySet()) {
                JsonArray levelsArray = originalData.getAsJsonArray(key);
                JsonObject newFormat = new JsonObject();
                JsonObject levels = new JsonObject();

                for (int i = 0; i < levelsArray.size(); i++) {
                    JsonObject levelData = levelsArray.get(i).getAsJsonObject();
                    int level = levelData.get("level").getAsInt();
                    int maxBalance = levelData.get("max_balance").getAsInt();
                    int upgradeCost = levelData.get("upgrade_cost").getAsInt();

                    JsonObject newLevelData = new JsonObject();
                    newLevelData.addProperty("max_balance", maxBalance);
                    newLevelData.addProperty("upgrade_cost", upgradeCost);
                    levels.add(String.valueOf(level), newLevelData);
                }

                newFormat.add("levels", levels);
                JsonArray newArray = new JsonArray();
                newArray.add(newFormat);
                convertedData.add(key, newArray);
            }

            // Save the new JSON file
            try (FileWriter writer = new FileWriter(jsonFile)) {
                Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gsonPretty.toJson(convertedData));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBank data successfully converted to new format, updated banks.json", plugin.placeholders));
            }

        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError during conversion: " + e.getMessage(), plugin.placeholders));
            if (BankUtils.getSaveException(plugin)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cAn unexpected error occurred during JSON conversion", plugin.placeholders));
            if (BankUtils.getSaveException(plugin)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        }
    }
}
