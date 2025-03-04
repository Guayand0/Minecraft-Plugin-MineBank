package com.Guayand0.Data.Player.JSON;

import com.Guayand0.MineBank;
import com.Guayand0.Data.Player.PlayerBankData;
import com.google.gson.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SetPlayerBankData {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public boolean setPlayerBankData(MineBank plugin, String playerName, PlayerBankData newBankData) {
        try {
            // Obtener el archivo JSON
            FileReader fileReader = new FileReader(plugin.getDataFolder() + "/bank/player_data.json");
            JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
            fileReader.close();

            // Buscar al jugador
            JsonArray playersArray = jsonObject.getAsJsonArray("player");
            for (int i = 0; i < playersArray.size(); i++) {
                JsonObject playerObj = playersArray.get(i).getAsJsonObject();
                if (playerObj.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    // Obtener el array "bank"
                    JsonArray bankArray = playerObj.getAsJsonArray("bank");
                    if (bankArray.size() > 0) {
                        JsonObject bankData = bankArray.get(0).getAsJsonObject();

                        // Actualizar los datos con los nuevos valores
                        bankData.addProperty("name", newBankData.getName());
                        bankData.addProperty("level", newBankData.getLevel());
                        bankData.addProperty("balance", newBankData.getBalance());
                        bankData.addProperty("offline_accrued_profit", newBankData.getOfflineAccruedProfit());
                        bankData.addProperty("offline_profit_times", newBankData.getOfflineProfitTimes());

                        // Guardar los cambios en el archivo
                        FileWriter fileWriter = new FileWriter(plugin.getDataFolder() + "/bank/player_data.json");
                        gson.toJson(jsonObject, fileWriter);
                        fileWriter.flush();
                        fileWriter.close();
                        return true; // Datos actualizados correctamente
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // No se encontró el jugador o hubo un error
    }
}

/*
PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

boolean success = SPBD.setPlayerBankData(plugin, targetPlayerName, newBankData);
if (success) {
    // Mensaje
} else {
    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
}
*/