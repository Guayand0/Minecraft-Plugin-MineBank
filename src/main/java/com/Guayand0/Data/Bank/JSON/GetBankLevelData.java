package com.Guayand0.Data.Bank.JSON;

import com.Guayand0.Data.Bank.BankLevelData;
import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GetBankLevelData {

    private final MessageUtils MU = new MessageUtils();
    private final Gson gson = new Gson();

    public Map<Integer, BankLevelData> loadBankLevels(MineBank plugin, String bankName) {
        File file = new File(plugin.getDataFolder(), "/bank/banks.json");
        if (!file.exists()) {
            plugin.getLogger().warning("El archivo banks.json no existe.");
            return new HashMap<>();
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            JsonElement bankElement = jsonObject.get(bankName);

            if (bankElement == null || !bankElement.isJsonArray() || bankElement.getAsJsonArray().isEmpty()) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cNo se encontraron datos para el banco: " + bankName));
                return new HashMap<>();
            }

            JsonObject levelsObject = bankElement.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("levels");
            Type type = new TypeToken<Map<Integer, BankLevelData>>() {}.getType();
            return gson.fromJson(levelsObject, type);

        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cError al leer el archivo JSON: " + e.getMessage()));
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            return new HashMap<>();
        }
    }
}
