package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class OnPlayerJoin implements Listener {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private int playerOfflineAccruedProfit = -1;

    public OnPlayerJoin(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            File file = fileManager.getPlayerDataFile(); // Obtener el archivo de datos de los jugadores

            JsonObject playerDataObject;

            // Si el archivo no existe o está vacío, crear la estructura base
            if (file.exists()) {
                if (file.length() > 0) {
                    try (FileReader reader = new FileReader(file)) {
                        playerDataObject = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                } else {
                    // Si el archivo está vacío, escribir la estructura base
                    playerDataObject = new JsonObject();
                    playerDataObject.add("player", new JsonArray());
                    try (FileWriter writer = new FileWriter(file)) {
                        gson.toJson(playerDataObject, writer);
                    }
                }
            } else {
                playerDataObject = new JsonObject();
                playerDataObject.add("player", new JsonArray());
            }

            JsonArray playersArray = playerDataObject.getAsJsonArray("player");
            Optional<JsonObject> existingPlayer = findPlayer(playersArray, player.getName());

            if (existingPlayer.isPresent()) {
                updatePlayerData(existingPlayer.get(), player);
            } else {
                JsonObject newPlayerData = createNewPlayerData(player);
                playersArray.add(newPlayerData);
            }

            // Escribir de nuevo el archivo con los datos actualizados
            try (FileWriter writer = new FileWriter(file)) { gson.toJson(playerDataObject, writer); }

            String playerName = player.getName();

            // Obtener solo el banco del jugador una vez
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            boolean bankUseAllowed = BU.getBankAllowed(plugin);
            playerOfflineAccruedProfit = BU.getPlayerOfflineAccruedProfit(bank);

            // Si el banco esta activado y el jugador tiene beneficios acumulados
            if (bankUseAllowed && playerOfflineAccruedProfit > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        bankProfitOfflineAccumulatedMessage(player); // Mensaje
                    }
                }.runTaskLater(plugin, 10); // Ejecuta la tarea después de 10 ticks (0.5 segundos)
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }
    }

    private Optional<JsonObject> findPlayer(JsonArray playersArray, String playerName) {
        for (JsonElement element : playersArray) {
            JsonObject playerData = element.getAsJsonObject();
            if (playerData.has("name") && playerData.get("name").getAsString().equals(playerName))
                return Optional.of(playerData);
        }
        return Optional.empty();
    }

    private void updatePlayerData(JsonObject playerData, Player player) {
        if (!playerData.has("name")) playerData.addProperty("name", player.getName());
        if (!playerData.has("UUID")) playerData.addProperty("UUID", player.getUniqueId().toString());

        // Asegurarse de que el banco siempre tenga los valores iniciales
        JsonArray bankArray = playerData.has("bank") ? playerData.getAsJsonArray("bank") : new JsonArray();
        if (bankArray.isEmpty()) bankArray.add(setDefaultBank());
        else {
            JsonObject bank = bankArray.get(0).getAsJsonObject();
            // Agregar valores predeterminados si faltan
            if (!bank.has("name")) bank.addProperty("name", BU.getBankStartBankName(plugin));
            if (!bank.has("level")) bank.addProperty("level", BU.getBankStartLevel(plugin));
            if (!bank.has("balance")) bank.addProperty("balance", BU.getBankStartBalance(plugin));
            if (!bank.has("offline_accrued_profit")) bank.addProperty("offline_accrued_profit", 0);
            if (!bank.has("offline_profit_times")) bank.addProperty("offline_profit_times", 0);
        }
        playerData.add("bank", bankArray);
    }

    private JsonObject createNewPlayerData(Player player) {
        JsonObject playerData = new JsonObject();
        playerData.addProperty("name", player.getName());
        playerData.addProperty("UUID", player.getUniqueId().toString());

        JsonArray bankArray = new JsonArray();
        bankArray.add(setDefaultBank());
        playerData.add("bank", bankArray);

        return playerData;
    }

    private JsonObject setDefaultBank() {
        JsonObject bank = new JsonObject();
        bank.addProperty("name", BU.getBankStartBankName(plugin));
        bank.addProperty("level", BU.getBankStartLevel(plugin));
        bank.addProperty("balance", BU.getBankStartBalance(plugin));
        bank.addProperty("offline_accrued_profit", 0);
        bank.addProperty("offline_profit_times", 0);
        return bank;
    }

    private void bankProfitOfflineAccumulatedMessage(Player player) {
        plugin.placeholders.put("%offlineprofitamount%", String.valueOf(playerOfflineAccruedProfit));

        for (String message : languageManager.getAllMessage("bank.profit.offline-accumulated")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}