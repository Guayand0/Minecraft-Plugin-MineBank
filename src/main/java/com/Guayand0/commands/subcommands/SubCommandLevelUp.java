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

public class SubCommandLevelUp implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private int bankMaxBalanceByLevel = -1;
    private int bankUpgradeLevelCost = -1;
    private int playerBankLevel = -1;

    public SubCommandLevelUp(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        try {

            String playerName = player.getName();

            // Obtener solo el banco del jugador una vez
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            int playerBankBalance = BU.getPlayerBankBalance(bank);
            playerBankLevel = BU.getPlayerBankLevel(bank);
            bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
            bankUpgradeLevelCost = BU.getBankUpgradeCostByLevel(plugin, playerName);
            int bankMaxLevel = BU.getBankMaxLevel(plugin, playerName);

            // Si el jugador ya está en el último nivel
            if (playerBankLevel == bankMaxLevel) {
                playerBankAlreadyMaxLevel(player);
                return true;
            }

            // Si el jugador no tiene suficiente dinero
            if (playerBankBalance < bankUpgradeLevelCost) {
                playerBankNextLevelNotBalance(player); // Mensaje
                return true;
            }

            // Retirar el dinero del banco
            BU.setPlayerBankBalance(bank, playerBankBalance - bankUpgradeLevelCost);

            // Establecer nuevo nivel del banco
            BU.setPlayerBankLevel(bank, playerBankLevel + 1);

            // Actualizar solo datos del banco
            fileManager.updatePlayerInfo(bank, player.getName());

            levelupSuccess(player); // Mensaje

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }


    private void playerBankNextLevelNotBalance(Player player) {
        plugin.placeholders.put("%playerbanknextlevelcost%", String.valueOf(bankUpgradeLevelCost));

        for (String message : languageManager.getAllMessage("bank.levelup.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerBankAlreadyMaxLevel(Player player) {
        for (String message : languageManager.getAllMessage("bank.levelup.already-max-level")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void levelupSuccess(Player player) {
        plugin.placeholders.put("%playerbanklevel%", String.valueOf(playerBankLevel));
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalanceByLevel));

        for (String message : languageManager.getAllMessage("bank.levelup.levelup-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
