package com.Guayand0.commands.subcommands;

import com.Guayand0.MineBank;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SubCommandData implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private String playerBankName;
    private int playerBankBalance;
    private int playerBankLevel;
    private int bankMaxBalanceByLevel;
    private int bankMaxLevel;
    private String targetPlayerName;

    public SubCommandData(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        try {

            JsonObject bank;

            if (args.length > 1) {
                targetPlayerName = args[1];
                bank = BU.getBankDataOfPlayerName(plugin, targetPlayerName);
            } else {
                targetPlayerName = null;
                bank = BU.getBankDataOfPlayerName(plugin, playerName);
            }

            playerBankName = BU.getPlayerBankName(bank);
            playerBankBalance = BU.getPlayerBankBalance(bank);
            playerBankLevel = BU.getPlayerBankLevel(bank);

            if (targetPlayerName != null) {

                bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, targetPlayerName);
                bankMaxLevel = BU.getBankMaxLevel(plugin, targetPlayerName);

                targetBankData(player); // Mensaje
            } else {

                bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
                bankMaxLevel = BU.getBankMaxLevel(plugin, playerName);

                playerBankData(player); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void playerBankData(Player player) {
        plugin.placeholders.put("%playerbankname%", playerBankName);
        plugin.placeholders.put("%playerbanklevel%", String.valueOf(playerBankLevel));
        plugin.placeholders.put("%playerbankmaxlevel%", String.valueOf(bankMaxLevel));
        plugin.placeholders.put("%playerbankbalance%", String.valueOf(playerBankBalance));
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalanceByLevel));

        for (String message : languageManager.getAllMessage("bank.data.player-bank-data")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void targetBankData(Player player) {
        plugin.placeholders.put("%targetbankname%", playerBankName);
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%targetbanklevel%", String.valueOf(playerBankLevel));
        plugin.placeholders.put("%targetbankmaxlevel%", String.valueOf(bankMaxLevel));
        plugin.placeholders.put("%targetbankbalance%", String.valueOf(playerBankBalance));
        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(bankMaxBalanceByLevel));

        for (String message : languageManager.getAllMessage("bank.data.target-bank-data")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
