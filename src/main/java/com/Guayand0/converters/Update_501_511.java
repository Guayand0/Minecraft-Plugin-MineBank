package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.google.gson.*;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Update_501_511 {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public Update_501_511(MineBank plugin) {
        this.plugin = plugin;
        convertJsonToJsonLeveled();
    }

    // bank/banks.json#level -> bank/banks.json#levels
    public void convertJsonToJsonLeveled() {
        File jsonFile = new File(plugin.getDataFolder(), "bank/banks.json");
        File backupFile = new File(plugin.getDataFolder(), "bank/old-banks.json");

        if (!jsonFile.exists()) return;

        jsonFile.getParentFile().mkdirs();

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
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of bank/banks.json -> bank/old-banks.json", plugin.placeholders));

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
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cAn unexpected error occurred during JSON conversion", plugin.placeholders));
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        }
    }
}
