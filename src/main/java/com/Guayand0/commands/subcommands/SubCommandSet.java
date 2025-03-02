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

public class SubCommandSet implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    int amount = 0;

    public SubCommandSet(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        // Si el comando tiene menos de 2 argumentos
        if (args.length < 4) {
            bankSetUsage(player); // Mensaje
            return true;
        }

        try {

            String playerName = player.getName();

            // Obtener solo el banco del jugador una vez
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            int bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
            int bankMaxLevel = BU.getBankMaxLevel(plugin, playerName);
            
            String targetPlayerName = args[1];
            String bankLevelOrBalance = args[2];
            String amountString = args[3];

            if (bankLevelOrBalance.equalsIgnoreCase("bal") || bankLevelOrBalance.equalsIgnoreCase("balance")) {

                // Mitad del maximo de almacenamiento de banco
                if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalanceByLevel / 2;

                    // Total del maximo de almacenamiento de banco
                } else if (amountString.equalsIgnoreCase("max")) {
                    amount = bankMaxBalanceByLevel;

                    // Si no se usa ninguno de esos se obtiene un valor y se comprueba que sea número válido
                } else {

                    try {
                        amount = Integer.parseInt(amountString);

                        if (amount > bankMaxBalanceByLevel) {
                            bankSetMaxBalance(player, String.valueOf(bankMaxBalanceByLevel)); // Mensaje
                            return true;
                        }

                        if (amount < 0) {
                            bankSetAmountFailure(player); // Mensaje
                            return true;
                        }

                    } catch (NumberFormatException e) {
                        bankSetAmountFailure(player); // Mensaje
                        return true;
                    }
                }
                
                // Establecer nuevo balance del banco
                BU.setPlayerBankBalance(bank, amount);

                // Actualizar solo datos del banco
                fileManager.updatePlayerInfo(bank, targetPlayerName);

                bankSetBalaceSuccess(player, targetPlayerName, String.valueOf(amount)); // Mensaje

            } else if (bankLevelOrBalance.equalsIgnoreCase("level")) {

                // Mitad del maximo de nivel de banco
                if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxLevel / 2;

                    // Total del maximo de nivel de banco
                } else if (amountString.equalsIgnoreCase("max")) {
                    amount = bankMaxLevel;

                    // Si no se usa ninguno de esos se obtiene un valor y se comprueba que sea número válido
                } else {
                    try {
                        amount = Integer.parseInt(amountString);

                        if (amount > bankMaxLevel) {
                            bankSetMaxLevel(player, String.valueOf(bankMaxLevel)); // Mensaje
                            return true;
                        }

                        if (amount < 0) {
                            bankSetAmountFailure(player); // Mensaje
                            return true;
                        }

                    } catch (NumberFormatException e) {
                        bankSetAmountFailure(player); // Mensaje
                        return true;
                    }
                }

                // Establecer nuevo balance del banco
                BU.setPlayerBankLevel(bank, amount);

                // Actualizar solo datos del banco
                fileManager.updatePlayerInfo(bank, targetPlayerName);

                bankSetLevelSuccess(player, targetPlayerName, String.valueOf(amount)); // Mensaje

            } else {
                bankSetUsage(player);  // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankSetUsage(Player player) {
        for (String message : languageManager.getAllMessage("bank.set-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetBalaceSuccess(Player player, String targetPlayerName, String amountString) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%amount%", amountString);

        for (String message : languageManager.getAllMessage("bank.set.set-balance-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetLevelSuccess(Player player, String targetPlayerName, String amountString) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%amount%", amountString);

        for (String message : languageManager.getAllMessage("bank.set.set-level-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetMaxBalance(Player player, String maxBalanceString) {
        plugin.placeholders.put("%targetbankmaxbalance%", maxBalanceString);

        for (String message : languageManager.getAllMessage("bank.set.max-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetMaxLevel(Player player, String maxLevelString) {
        plugin.placeholders.put("%targetbankmaxlevel%", maxLevelString);

        for (String message : languageManager.getAllMessage("bank.set.max-level")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetAmountFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.set.set-amount-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetPlayerNotFound(Player player, String targetPlayerName) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);

        for (String message : languageManager.getAllMessage("bank.set.target-player-not-found")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
