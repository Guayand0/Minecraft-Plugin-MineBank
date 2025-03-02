package com.Guayand0.tasks;

import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class ProfitBankTask extends BukkitRunnable {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private int amountRoundedProfit = -1;
    private double profitPercentage = -1;
    private int minBankBalanceToApplyProfit = -1;

    public ProfitBankTask(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public void run() {
        if (BU.getBankAllowed(plugin)) {
            try {
                executeBankTask();
            } catch (Exception e) {
                e.printStackTrace();
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        }
    }

    private void executeBankTask() throws IOException {
        
        List<String> bankPlayerNames = BU.getPlayerNameOfBank(plugin);

        for (String playerName : bankPlayerNames) {
            
            // Obtener solo el banco del jugador una vez
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            int playerBankBalance = BU.getPlayerBankBalance(bank);
            int playerOfflineAccruedProfit = BU.getPlayerOfflineAccruedProfit(bank);
            int playerOfflineProfitTimes = BU.getPlayerOfflineProfitTimes(bank);
            int bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
            minBankBalanceToApplyProfit = BU.getProfitMinBankBalanceToReceive(plugin);
            double profitKeepInBankPercentage = BU.getProfitKeepInBankPercentage(plugin);
            boolean profitMultiplyByBankLevel = BU.getProfitMultiplyByBankLevel(plugin);
            boolean notBalanceProfitMessage = BU.getProfitNotEnoughBalanceToReveiveMessage(plugin);
            int timesProfitsOffline = BU.getTimesProfitsOffline(plugin);
            boolean isPlayerOnline = BU.isPlayerOnline(playerName);

            Player player = null;

            try {

                // Si el jugador no tiene dinero o si tiene exceso de dinero
                if (playerBankBalance <= 0 || playerBankBalance > bankMaxBalanceByLevel) return;

                // Si el jugador esta online establecerlo como player
                if (isPlayerOnline) player = Bukkit.getPlayerExact(playerName);

                // Si el jugador tiene suficiente dinero para ganar beneficio
                if (playerBankBalance >= minBankBalanceToApplyProfit) {

                    // Si multiplicar beneficio por nivel está activado
                    if (profitMultiplyByBankLevel) profitPercentage = profitKeepInBankPercentage * BU.getPlayerBankLevel(bank);
                    else profitPercentage = profitKeepInBankPercentage;

                    double profit = Math.floor(playerBankBalance * profitPercentage / 100.0);

                    amountRoundedProfit = (int) profit;

                    // Si el jugador no esta online añadirle el dinero del beneficio a "offline_accrued_profit"
                    if (!isPlayerOnline) {

                        // Si la opcion esta desactivada o el jugador ha llegado al maximo de la opcion
                        if (timesProfitsOffline <= 0 || playerOfflineProfitTimes >= timesProfitsOffline) return;

                        // Sumar el profit y las veces que se han sumado
                        BU.setPlayerOfflineAccruedProfit(bank, playerOfflineAccruedProfit + amountRoundedProfit);
                        BU.setPlayerOfflineProfitTimes(bank, playerOfflineProfitTimes + 1);

                        // Actualizar solo datos del banco
                        fileManager.updatePlayerInfo(bank, playerName);
                        return;
                    }

                    // Si el jugador supera el maximo de almacenamiento de su nuvel de banco
                    if (playerBankBalance + amountRoundedProfit > bankMaxBalanceByLevel) {
                        assert player != null;
                        maxStorageProfitMessage(player); // Message

                    } else {
                        // Establecer nuevo balance del banco
                        BU.setPlayerBankBalance(bank, playerBankBalance + amountRoundedProfit);

                        // Actualizar solo datos del banco
                        fileManager.updatePlayerInfo(bank, playerName);

                        assert player != null;
                        receivedProfitMessage(player); // Mensaje
                    }

                } else {
                    // Si enviar mensaje está activado y el jugador esta conectado
                    if (notBalanceProfitMessage && player != null) minStorageProfitMessage(player); // Message
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
        }
    }

    public void receivedProfitMessage(Player player) {
        plugin.placeholders.put("%keepinbankprofit%", String.valueOf(amountRoundedProfit));
        plugin.placeholders.put("%profitpercentage%", String.valueOf(profitPercentage));

        for (String message : languageManager.getAllMessage("bank.profit.received")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    public void minStorageProfitMessage(Player player) {
        plugin.placeholders.put("%minbankbalancetoreceiveprofit%", String.valueOf(minBankBalanceToApplyProfit));

        for (String message : languageManager.getAllMessage("bank.profit.min-storage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    public void maxStorageProfitMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.profit.max-storage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
