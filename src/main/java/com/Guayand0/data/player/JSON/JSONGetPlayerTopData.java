package com.Guayand0.data.player.JSON;

import com.Guayand0.MineBank;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class JSONGetPlayerTopData {

    public List<List<String>> getTopPlayerBanks(MineBank plugin, int amount) throws IOException {
        List<List<String>> bankInfoList = new ArrayList<>();

        // Leer player_data.json
        Reader playerReader = new FileReader(plugin.getFileManager().getPlayerDataFile());
        JsonObject playerData = new JsonParser().parse(playerReader).getAsJsonObject();
        playerReader.close();

        JsonArray players = playerData.getAsJsonArray("player");

        for (JsonElement element : players) {
            JsonObject playerObj = element.getAsJsonObject();
            JsonArray bankArray = playerObj.getAsJsonArray("bank");

            if (bankArray.size() > 0) {
                JsonObject bankObj = bankArray.get(0).getAsJsonObject();
                List<String> bankInfo = new ArrayList<>();
                bankInfo.add(playerObj.get("name").getAsString()); // Nombre del jugador
                bankInfo.add(bankObj.get("name").getAsString());   // Nombre del banco
                bankInfo.add(String.valueOf(bankObj.get("level").getAsInt())); // Nivel del banco
                bankInfo.add(String.valueOf(bankObj.get("balance").getAsInt())); // Balance del banco
                bankInfoList.add(bankInfo);
            }
        }

        // Ordenar por balance en orden descendente
        bankInfoList.sort((a, b) -> Integer.compare(Integer.parseInt(b.get(3)), Integer.parseInt(a.get(3))));

        // Devolver los 10 primeros o menos si hay menos de 10 jugadores
        return bankInfoList.size() > amount ? bankInfoList.subList(0, amount) : bankInfoList;
    }
}
