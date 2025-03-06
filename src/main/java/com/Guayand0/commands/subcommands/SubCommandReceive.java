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

public class SubCommandReceive implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final JSONGetPlayerData BM = new JSONGetPlayerData();
    private final JSONSetPlayerBankData SPBD = new JSONSetPlayerBankData();

    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int bankMaxBalance = -1;

    public SubCommandReceive(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        // Si el comando tiene menos de 2 argumentos
        if (args.length < 2) {
            bankReceiveUsageMessage(player); // Mensaje
            return true;
        }

        try {
            // Obtener datos del banco del jugador y maximos de nivel y balance
            BankData bankData = BM.getPlayerBankData(plugin, playerName);
            if (bankData != null) {
                bankName = bankData.getBankName();
                bankLevel = bankData.getBankLevel();
                bankBalance = bankData.getBankBalance();
                offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                bankMaxBalance = bankData.getBankMaxBalance();
            }

            String arg = args[1];

            if (arg.equalsIgnoreCase("profit")) {

                if (offlineProfitAccrued <= 0) {
                    bankReceiveOfflineNotProfitMessage(player); // Mensaje
                    return true;
                }

                int playerBankSpace = bankMaxBalance - bankBalance;

                if (playerBankSpace < offlineProfitAccrued) {
                    bankReceiveExceedMessage(player); // Mensaje
                    return true;
                }

                bankBalance = bankBalance + offlineProfitAccrued;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, 0, 0);

                boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                if (success) {
                    bankReceiveOfflineSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }

            //} else if (arg.equalsIgnoreCase("lottery")) {

            } else {
                bankReceiveUsageMessage(player); // Mensaje
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankReceiveUsageMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.receive-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveOfflineSuccessMessage(Player player) {
        plugin.placeholders.put("%offlineprofitamount%", String.valueOf(offlineProfitAccrued));

        for (String message : languageManager.getAllMessage("bank.receive.offline-received-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveExceedMessage(Player player) {
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalance));

        for (String message : languageManager.getAllMessage("bank.receive.receive-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveOfflineNotProfitMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.receive.offline-not-profit")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
