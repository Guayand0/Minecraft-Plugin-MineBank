package com.Guayand0.data.bank.JSON;

import com.Guayand0.MineBank;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONGetBankNames {

    public List<String> getBankNames(MineBank plugin) throws IOException {
        // Obtener los datos de los bancos
        JsonObject banksData = getBankData(plugin);

        // Crear una lista para almacenar los nombres de los bancos
        List<String> bankNames = new ArrayList<>();

        // Iterar sobre las claves del objeto JSON
        for (Map.Entry<String, JsonElement> entry : banksData.entrySet()) {
            bankNames.add(entry.getKey());
        }

        return bankNames;
    }

    public JsonObject getBankData(MineBank plugin) throws IOException {
        // Leer banks.json
        Reader bankReader = new FileReader(plugin.getFileManager().getBanksFile());
        JsonObject banksData = new JsonParser().parse(bankReader).getAsJsonObject();
        bankReader.close();
        return banksData;
    }
}
