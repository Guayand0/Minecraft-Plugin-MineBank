package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PlayerBankDataConverter {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();

    public PlayerBankDataConverter(MineBank plugin) {
        this.plugin = plugin;
    }

    public void convertYamlToJson() {
        try {
            File yamlFile = new File(plugin.getDataFolder(), "bank.yml");
            File backupFile = new File(plugin.getDataFolder(), "old-bank.yml");

            if (!yamlFile.exists()) return;

            // Crear una copia de seguridad
            try {
                Files.copy(yamlFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of the bank.yml file to old-bank.yml", plugin.placeholders));
            } catch (IOException e) {
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError creating the backup of bank.yml: " + e.getMessage(), plugin.placeholders));
                return;
            }

            FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
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
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError writing in the JSON file: " + e.getMessage(), plugin.placeholders));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + "&cAn error occurred while processing the player data migration", plugin.placeholders));
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
