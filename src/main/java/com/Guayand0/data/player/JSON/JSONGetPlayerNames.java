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
import java.util.Collections;
import java.util.List;

public class JSONGetPlayerNames {

    public List<String> getAllRegisteredPlayerName(MineBank plugin) throws IOException {
        List<String> playerNames = new ArrayList<>();

        // Leer player_data.json
        Reader playerReader = new FileReader(plugin.getFileManager().getPlayerDataFile());
        JsonObject playerData = new JsonParser().parse(playerReader).getAsJsonObject();
        playerReader.close();

        JsonArray players = playerData.getAsJsonArray("player");

        // Obtener todos los nombres de jugadores que se han conectado
        for (JsonElement element : players) {
            JsonObject player_obj = element.getAsJsonObject();
            playerNames.add(player_obj.get("name").getAsString());
        }

        // Ordenar la lista alfabéticamente
        Collections.sort(playerNames);

        return playerNames;
    }
}
