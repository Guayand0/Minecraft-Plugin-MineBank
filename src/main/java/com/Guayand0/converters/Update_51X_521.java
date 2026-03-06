package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.google.gson.*;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Update_51X_521 {

    private final MineBank plugin;
    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    // 5.1.x a 5.2.1
    public Update_51X_521(MineBank plugin) {
        this.plugin = plugin;
        splitBanksJson();
        copyInterestsData();
        splitPlayerDataJson();
    }

    // bank/banks.json -> data/bank_data/<bank>.json
    public void splitBanksJson() {
        File sourceFile = new File(plugin.getDataFolder(), "bank/banks.json");
        if (!sourceFile.exists()) return;

        File outputDir = new File(plugin.getDataFolder(), "data/bank_data");
        outputDir.mkdirs();

        File backupFile = new File(plugin.getDataFolder(), "bank/old-banks.json");

        try {
            // Backup
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of bank/banks.json -> bank/old-banks.json", plugin.placeholders)
            );

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(new String(Files.readAllBytes(sourceFile.toPath())), JsonObject.class);

            for (String bankName : root.keySet()) {
                JsonArray bankArray = root.getAsJsonArray(bankName);
                if (bankArray.isEmpty()) continue;

                JsonObject bankData = bankArray.get(0).getAsJsonObject();
                if (!bankData.has("levels")) continue;

                JsonObject newFormat = new JsonObject();
                newFormat.add("levels", bankData.getAsJsonObject("levels"));

                File outFile = new File(outputDir, bankName + ".json");
                try (FileWriter writer = new FileWriter(outFile)) {
                    Gson pretty = new GsonBuilder().setPrettyPrinting().create();
                    writer.write(pretty.toJson(newFormat));
                }
            }

            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBanks successfully split into data/bank_data/", plugin.placeholders));

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError splitting banks.json: " + e.getMessage(), plugin.placeholders));
            if (GV.getBoolean(plugin, "exception.save", true)) {Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        }
    }

    // bank/interests_data.json -> data/interests_data.json
    public void copyInterestsData() {
        File sourceFile = new File(plugin.getDataFolder(), "bank/interests_data.json");
        if (!sourceFile.exists()) return;

        File targetDir = new File(plugin.getDataFolder(), "data");
        targetDir.mkdirs();

        File targetFile = new File(targetDir, "interests_data.json");
        File backupFile = new File(plugin.getDataFolder(), "bank/old-interests_data.json");

        try {
            // Backup del original
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of bank/interests_data.json -> bank/old-interests_data.json", plugin.placeholders));

            // Copia al nuevo destino
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eCopied bank/interests_data.json to data/interests_data.json", plugin.placeholders));

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError converting interests_data.json: " + e.getMessage(), plugin.placeholders));
            if (GV.getBoolean(plugin, "exception.save", true)) {Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
        }
    }

    // bank/player_data.json -> data/player_data/<UUID>.json
    public void splitPlayerDataJson() {
        File sourceFile = new File(plugin.getDataFolder(), "bank/player_data.json");
        if (!sourceFile.exists()) return;

        File outputDir = new File(plugin.getDataFolder(), "data/player_data");
        outputDir.mkdirs();

        File backupFile = new File(plugin.getDataFolder(), "bank/old-player_data.json");

        Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &eBackup of bank/player_data.json -> bank/old-player_data.json", plugin.placeholders));

        try {
            // Backup
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(new String(Files.readAllBytes(sourceFile.toPath())), JsonObject.class);

            if (!root.has("player")) return;

            JsonArray players = root.getAsJsonArray("player");

            for (JsonElement playerEl : players) {
                JsonObject playerObj = playerEl.getAsJsonObject();

                String uuid = playerObj.get("UUID").getAsString();
                JsonArray banks = playerObj.getAsJsonArray("bank");
                if (banks == null || banks.isEmpty()) continue;

                // Solo se usa el primer banco
                JsonObject bankObj = banks.get(0).getAsJsonObject();

                JsonObject offline = new JsonObject();
                offline.addProperty("accrued_profit", bankObj.get("offline_accrued_profit").getAsInt());
                offline.addProperty("profit_times", bankObj.get("offline_profit_times").getAsInt());

                JsonObject newBank = new JsonObject();
                newBank.addProperty("name", bankObj.get("name").getAsString());
                newBank.addProperty("level", bankObj.get("level").getAsInt());
                newBank.addProperty("balance", bankObj.get("balance").getAsInt());
                newBank.add("offline", offline);

                JsonObject finalData = new JsonObject();
                finalData.add("bank", newBank);

                File outFile = new File(outputDir, uuid + ".json");
                try (FileWriter writer = new FileWriter(outFile)) {
                    Gson pretty = new GsonBuilder().setPrettyPrinting().create();
                    writer.write(pretty.toJson(finalData));
                }
            }

            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &ePlayer data successfully converted to data/player_data/", plugin.placeholders));

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(plugin.prefix + " &cError converting player_data.json: " + e.getMessage(), plugin.placeholders));
            if (GV.getBoolean(plugin, "exception.save", true)) {Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));}
        }
    }
}