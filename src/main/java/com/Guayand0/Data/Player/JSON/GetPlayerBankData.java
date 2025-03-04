package com.Guayand0.Data.Player.JSON;

import com.Guayand0.MineBank;
import com.Guayand0.Data.Player.PlayerBankData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class GetPlayerBankData {

    public PlayerBankData getPlayerBankData(MineBank plugin, String playerName) {
        PlayerBankData playerBankData = null;
        try {
            // Obtener el archivo JSON
            FileReader fileReader = new FileReader(plugin.getDataFolder() + "/bank/player_data.json");
            JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();

            // Buscar al jugador
            JsonArray playersArray = jsonObject.getAsJsonArray("player");
            for (int i = 0; i < playersArray.size(); i++) {
                JsonObject playerObj = playersArray.get(i).getAsJsonObject();
                if (playerObj.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    // Si el nombre coincide, obtener los datos del banco
                    JsonArray bankArray = playerObj.getAsJsonArray("bank");
                    if (bankArray.size() > 0) {
                        JsonObject bankData = bankArray.get(0).getAsJsonObject();

                        // Crear el objeto PlayerBankData usando los datos del JSON
                        String name = bankData.get("name").getAsString();
                        int level = bankData.get("level").getAsInt();
                        int balance = bankData.get("balance").getAsInt();
                        int offlineAccruedProfit = bankData.get("offline_accrued_profit").getAsInt();
                        int offlineProfitTimes = bankData.get("offline_profit_times").getAsInt();

                        // Crear el objeto PlayerBankData
                        playerBankData = new PlayerBankData(name, level, balance, offlineAccruedProfit, offlineProfitTimes);
                    }
                    break;
                }
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return playerBankData; // Si no encuentra el jugador, retornará null
    }
}
