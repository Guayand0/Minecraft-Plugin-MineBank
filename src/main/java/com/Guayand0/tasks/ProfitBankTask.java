package com.Guayand0.tasks;

import com.Guayand0.Data.BankData;
import com.Guayand0.Data.BankManager;
import com.Guayand0.Data.Player.JSON.SetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class ProfitBankTask extends BukkitRunnable {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();
    private final BankManager BM = new BankManager();
    private final SetPlayerBankData SPBD = new SetPlayerBankData();

    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private int amountRoundedProfit = -1;
    private double profitPercentage = -1;
    private int minBankBalanceToApplyProfit = -1;

    public ProfitBankTask(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
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

            // Obtener datos del banco del jugador y maximos de nivel y balance
            BankData bankData = BM.getBankData(plugin, playerName);
            if (bankData != null) {
                bankName = bankData.getBankName();
                bankLevel = bankData.getBankLevel();
                bankBalance = bankData.getBankBalance();
                offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                offlineProfitTimes = bankData.getOfflineProfitTimes();
                bankMaxBalance = bankData.getBankMaxBalance();
            }

            minBankBalanceToApplyProfit = BU.getProfitMinBankBalanceToReceive(plugin);
            double profitKeepInBankPercentage = BU.getProfitKeepInBankPercentage(plugin);
            boolean profitMultiplyByBankLevel = BU.getProfitMultiplyByBankLevel(plugin);
            boolean notBalanceProfitMessage = BU.getProfitNotEnoughBalanceToReveiveMessage(plugin);
            int timesProfitsOffline = BU.getTimesProfitsOffline(plugin);
            boolean isPlayerOnline = BU.isPlayerOnline(playerName);

            Player player = null;

            try {

                // Si el jugador no tiene dinero o si tiene exceso de dinero
                if (bankBalance <= 0 || bankBalance > bankMaxBalance) return;

                // Si el jugador esta online establecerlo como player
                if (isPlayerOnline) player = Bukkit.getPlayerExact(playerName);

                // Si el jugador tiene suficiente dinero para ganar beneficio
                if (bankBalance >= minBankBalanceToApplyProfit) {

                    // Si multiplicar beneficio por nivel está activado
                    if (profitMultiplyByBankLevel) profitPercentage = profitKeepInBankPercentage * bankLevel;
                    else profitPercentage = profitKeepInBankPercentage;

                    double profit = Math.floor(bankBalance * profitPercentage / 100.0);

                    amountRoundedProfit = (int) profit;

                    // Si el jugador no esta online añadirle el dinero del beneficio a "offline_accrued_profit"
                    if (!isPlayerOnline) {

                        // Si la opcion esta desactivada o el jugador ha llegado al maximo de la opcion
                        if (timesProfitsOffline <= 0 || offlineProfitTimes >= timesProfitsOffline) return;

                        offlineProfitAccrued = offlineProfitAccrued + amountRoundedProfit;
                        offlineProfitTimes = offlineProfitTimes + 1;
                        // Establecer nuevos valores de banco
                        PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                        boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                        if (!success) {
                            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                        }

                        return;
                    }

                    // Si el jugador supera el maximo de almacenamiento de su nuvel de banco
                    if (bankBalance + amountRoundedProfit > bankMaxBalance) {
                        maxStorageProfitMessage(player); // Message
                        return;
                    }

                    bankBalance = bankBalance + amountRoundedProfit;
                    // Establecer nuevos valores de banco
                    PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                    boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                    if (success) {
                        receivedProfitMessage(player); // Mensaje
                    } else {
                        Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
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
