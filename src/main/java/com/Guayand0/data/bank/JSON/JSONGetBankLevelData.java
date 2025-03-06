package com.Guayand0.data.bank.JSON;

import com.Guayand0.data.bank.MYSQLBankLevelData;
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

public class JSONGetBankLevelData {

    private final MessageUtils MU = new MessageUtils();
    private final Gson gson = new Gson();

    public Map<Integer, MYSQLBankLevelData> loadBankLevels(MineBank plugin, String bankName) {
        File file = new File(plugin.getDataFolder(), "/bank/banks.json");
        if (!file.exists()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cThe banks.json file does not exist."));
            return new HashMap<>();
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            JsonElement bankElement = jsonObject.get(bankName);

            if (bankElement == null || !bankElement.isJsonArray() || bankElement.getAsJsonArray().isEmpty()) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cData not found for the bank: " + bankName));
                return new HashMap<>();
            }

            JsonObject levelsObject = bankElement.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("levels");
            Type type = new TypeToken<Map<Integer, MYSQLBankLevelData>>() {}.getType();
            return gson.fromJson(levelsObject, type);

        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cERROR while try to read bank.json file: " + e.getMessage()));
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            return new HashMap<>();
        }
    }
}
