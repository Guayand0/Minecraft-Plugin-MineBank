package com.Guayand0.managers;

import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.*;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.file.Files;

public class FileManager {

    private final MineBank plugin;
    private File banksFile;
    private File playerDataFile;
    private File interestsDataFile;

    private final MessageUtils MU = new MessageUtils();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FileManager(MineBank plugin) {
        this.plugin = plugin;
        loadDataFiles();
    }

    private void loadDataFiles() {
        File dataFolder = plugin.getDataFolder();

        // -------- //

        File bankFolder = new File(dataFolder, "bank");

        if (!bankFolder.exists()) {
            bankFolder.mkdirs();
        }

        playerDataFile = new File(bankFolder, "player_data.json");
        banksFile = new File(bankFolder, "banks.json");
        interestsDataFile = new File(bankFolder, "interests_data.json");

        createFileIfNotExists(playerDataFile, "bank/player_data.json");
        createFileIfNotExists(banksFile, "bank/banks.json");
        createFileIfNotExists(interestsDataFile, "bank/interests_data.json");
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
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        }
    }

    public File getBanksFile() {
        return banksFile;
    }

    public File getPlayerDataFile() {
        return playerDataFile;
    }

    public File getInterestsDataFile() {
        return interestsDataFile;
    }

    public void updatePlayerInfo(JsonObject updatedBank, String playerName) {
        try (Reader reader = new FileReader(playerDataFile)) {
            // Leer el JSON actual
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            JsonArray players = data.getAsJsonArray("player");

            for (JsonElement playerElement : players) {
                JsonObject playerObj = playerElement.getAsJsonObject();
                if (playerObj.get("name").getAsString().equals(playerName)) {
                    // Obtener la lista de bancos del jugador
                    JsonArray bankArray = playerObj.getAsJsonArray("bank");
                    if (bankArray.size() > 0) {
                        JsonObject bank = bankArray.get(0).getAsJsonObject();

                        // Solo actualiza los valores sin reemplazar la estructura
                        if (updatedBank.has("name")) bank.addProperty("name", updatedBank.get("name").getAsString());
                        if (updatedBank.has("level")) bank.addProperty("level", updatedBank.get("level").getAsInt());
                        if (updatedBank.has("balance")) bank.addProperty("balance", updatedBank.get("balance").getAsInt());
                        if (updatedBank.has("offline_accrued_profit")) bank.addProperty("offline_accrued_profit", updatedBank.get("offline_accrued_profit").getAsInt());
                        if (updatedBank.has("offline_profit_times")) bank.addProperty("offline_profit_times", updatedBank.get("offline_profit_times").getAsInt());
                    }
                    break;
                }
            }

            // Guardar el archivo con los cambios
            try (Writer writer = new FileWriter(playerDataFile)) {
                gson.toJson(data, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }
    }

    public void updateInterestsData(int newAccruedInterest) {
        try (Reader reader = new FileReader(interestsDataFile)) {

            // Leer el JSON actual
            JsonObject data = gson.fromJson(reader, JsonObject.class);

            // Actualizar el valor de "accrued_interest"
            data.addProperty("accrued_interest", newAccruedInterest);

            // Guardar el archivo con los cambios
            try (Writer writer = new FileWriter(interestsDataFile)) {
                gson.toJson(data, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }
    }
}
