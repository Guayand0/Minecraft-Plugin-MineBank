package com.Guayand0.commands.subcommands;

import com.Guayand0.data.BankData;
import com.Guayand0.data.player.JSON.JSONGetPlayerData;
import com.Guayand0.data.player.JSON.JSONSetPlayerBankData;
import com.Guayand0.data.player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandLevelUp implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final JSONGetPlayerData BM = new JSONGetPlayerData();
    private final JSONSetPlayerBankData SPBD = new JSONSetPlayerBankData();

    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private int bankMaxLevel = -1;
    private int bankLevelUpgradeCost = -1;

    public SubCommandLevelUp(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        try {
            // Obtener datos del banco del jugador y maximos de nivel y balance
            BankData bankData = BM.getPlayerBankData(plugin, playerName);
            if (bankData != null) {
                bankName = bankData.getBankName();
                bankLevel = bankData.getBankLevel();
                bankBalance = bankData.getBankBalance();
                offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                offlineProfitTimes = bankData.getOfflineProfitTimes();
                bankMaxLevel = bankData.getBankMaxLevel();
                bankMaxBalance = bankData.getBankMaxBalance();
                bankLevelUpgradeCost = bankData.getBankLevelUpgradeCost();
            }

            // Si el jugador ya está en el último nivel
            if (bankLevel == bankMaxLevel) {
                playerBankAlreadyMaxLevelMessage(player); // Mensaje
                return true;
            }

            // Si el jugador no tiene suficiente dinero
            if (bankBalance < bankLevelUpgradeCost) {
                playerBankNextLevelNotBalanceMessage(player); // Mensaje
                return true;
            }

            bankBalance = bankBalance - bankLevelUpgradeCost;
            bankLevel = bankLevel + 1;
            // Establecer nuevos valores de banco
            PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

            boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
            if (success) {
                // Obtener el nuevo maximo de balance
                bankData = BM.getPlayerBankData(plugin, playerName);
                if (bankData != null) {
                    bankMaxBalance = bankData.getBankMaxBalance();
                }

                levelupSuccessMessage(player); // Mensaje
            } else {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void playerBankNextLevelNotBalanceMessage(Player player) {
        plugin.placeholders.put("%playerbanknextlevelcost%", String.valueOf(bankLevelUpgradeCost));

        for (String message : languageManager.getAllMessage("bank.levelup.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerBankAlreadyMaxLevelMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.levelup.already-max-level")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void levelupSuccessMessage(Player player) {
        plugin.placeholders.put("%playerbanklevel%", String.valueOf(bankLevel));
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalance));

        for (String message : languageManager.getAllMessage("bank.levelup.levelup-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
