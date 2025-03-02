package com.Guayand0.commands.subcommands;

import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandReceive implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private int bankMaxBalanceByLevel = -1;
    private int originalPlayerOfflineAccruedProfit = -1;

    public SubCommandReceive(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        // Si el comando tiene menos de 2 argumentos
        if (args.length < 2) {
            bankReceiveUsage(player); // Mensaje
            return true;
        }

        try {

            String playerName = player.getName();

            // Obtener solo el banco del jugador una vez
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            int playerBankBalance = BU.getPlayerBankBalance(bank);
            bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
            int playerOfflineAccruedProfit = BU.getPlayerOfflineAccruedProfit(bank);
            int bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);

            String type = args[1];

            if (type.equalsIgnoreCase("profit")) {

                if (playerOfflineAccruedProfit <= 0) {
                    bankReceiveOfflineNotProfit(player); // Mensaje
                    return true;
                }

                int playerBankSpace = bankMaxBalanceByLevel - playerBankBalance;
                originalPlayerOfflineAccruedProfit = playerOfflineAccruedProfit;

                if (playerBankSpace >= playerOfflineAccruedProfit) {

                    // Restablecer la cantidad de beneficios acumulados
                    BU.setPlayerOfflineAccruedProfit(bank, 0);
                    BU.setPlayerOfflineProfitTimes(bank, 0);

                    // Establecer nuevo balance del banco
                    BU.setPlayerBankBalance(bank, playerBankBalance + playerOfflineAccruedProfit);

                    // Actualizar solo datos del banco
                    fileManager.updatePlayerInfo(bank, playerName);

                    bankReceiveOfflineSuccess(player); // Mensaje

                } else {
                    bankReceiveExceed(player); // Mensaje
                }
            //} else if (type.equalsIgnoreCase("lottery")) {

            } else {
                bankReceiveUsage(player); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankReceiveUsage(Player player) {
        for (String message : languageManager.getAllMessage("bank.receive-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveOfflineSuccess(Player player) {
        plugin.placeholders.put("%offlineprofitamount%", String.valueOf(originalPlayerOfflineAccruedProfit));

        for (String message : languageManager.getAllMessage("bank.receive.offline-received-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveExceed(Player player) {
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalanceByLevel));

        for (String message : languageManager.getAllMessage("bank.receive.receive-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankReceiveOfflineNotProfit(Player player) {
        for (String message : languageManager.getAllMessage("bank.receive.offline-not-profit")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
