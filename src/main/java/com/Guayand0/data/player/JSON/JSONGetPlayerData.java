package com.Guayand0.data.player.JSON;

import com.Guayand0.data.BankData;
import com.Guayand0.data.bank.MYSQLBankLevelData;
import com.Guayand0.data.bank.JSON.JSONGetBankLevelData;
import com.Guayand0.MineBank;
import com.Guayand0.data.player.PlayerBankData;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JSONGetPlayerData {

    private final MessageUtils MU = new MessageUtils();
    private final JSONGetBankLevelData GBLD = new JSONGetBankLevelData();

    public BankData getPlayerBankData(MineBank plugin, String playerName) {
        PlayerBankData playerBankData = null;
        try (FileReader fileReader = new FileReader(plugin.getDataFolder() + "/bank/player_data.json")) {
            JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
            JsonArray playersArray = jsonObject.getAsJsonArray("player");

            for (int i = 0; i < playersArray.size(); i++) {
                JsonObject playerObj = playersArray.get(i).getAsJsonObject();
                if (playerObj.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    JsonArray bankArray = playerObj.getAsJsonArray("bank");
                    if (!bankArray.isEmpty()) {
                        JsonObject bankData = bankArray.get(0).getAsJsonObject();
                        playerBankData = new PlayerBankData(
                                bankData.get("name").getAsString(),
                                bankData.get("level").getAsInt(),
                                bankData.get("balance").getAsInt(),
                                bankData.get("offline_accrued_profit").getAsInt(),
                                bankData.get("offline_profit_times").getAsInt()
                        );
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (playerBankData == null) {
            return null;
        }

        String bankName = playerBankData.getName();
        int bankLevel = playerBankData.getLevel();
        int bankBalance = playerBankData.getBalance();
        int offlineProfitAccrued = playerBankData.getOfflineAccruedProfit();
        int offlineProfitTimes = playerBankData.getOfflineProfitTimes();

        Map<Integer, MYSQLBankLevelData> levels = GBLD.loadBankLevels(plugin, bankName);
        int bankMaxLevel = levels.isEmpty() ? 0 : Collections.max(levels.keySet());

        if (!levels.containsKey(bankLevel)) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cLevel " + bankLevel +
                    " not found in bank " + bankName + " for player " + playerName));
            bankLevel = bankMaxLevel;
        }

        MYSQLBankLevelData levelData = levels.get(bankLevel);
        int bankMaxBalance = levelData.getMaxBalance();
        int bankLevelUpgradeCost = (bankLevel < bankMaxLevel) ? levelData.getUpgradeCost() : 0;

        return new BankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes, bankMaxBalance, bankMaxLevel, bankLevelUpgradeCost);
    }
}
