package com.Guayand0.Data;

import com.Guayand0.Data.Bank.BankLevelData;
import com.Guayand0.Data.Bank.JSON.GetBankLevelData;
import com.Guayand0.Data.Player.JSON.GetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Map;

public class BankManager {

    private final MessageUtils MU = new MessageUtils();
    private final GetPlayerBankData GPBD = new GetPlayerBankData();
    private final GetBankLevelData GBLD = new GetBankLevelData();

    public BankData getBankData(MineBank plugin, String playerName) {
        // Obtener los datos del banco del jugador
        PlayerBankData playerBankData = GPBD.getPlayerBankData(plugin, playerName);
        if (playerBankData == null) {
            return null;
        }

        String bankName = playerBankData.getName();
        int bankLevel = playerBankData.getLevel();
        int bankBalance = playerBankData.getBalance();
        int offlineProfitAccrued = playerBankData.getOfflineAccruedProfit();
        int offlineProfitTimes = playerBankData.getOfflineProfitTimes();

        // Cargar los niveles del banco
        Map<Integer, BankLevelData> levels = GBLD.loadBankLevels(plugin, bankName);
        // Obtener el último nivel del banco
        int bankMaxLevel = levels.isEmpty() ? 0 : Collections.max(levels.keySet());

        // Si el nivel del jugador no existe, ajustarlo al nivel más alto disponible
        if (!levels.containsKey(bankLevel)) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cLevel " + bankLevel +
                    " not found in bank " + bankName + " for player " + playerName));

            bankLevel = bankMaxLevel;
        }

        // Obtener datos del nivel actual
        BankLevelData levelData = levels.get(bankLevel);
        int bankMaxBalance = levelData.getMaxBalance();
        int bankLevelUpgradeCost = (bankLevel < bankMaxLevel) ? levelData.getUpgradeCost() : 0;


        return new BankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes, bankMaxBalance, bankMaxLevel, bankLevelUpgradeCost);
    }
}
