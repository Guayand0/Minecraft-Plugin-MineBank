package com.Guayand0.commands.subcommands;

import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonObject;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class SubCommandAdd implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private final Economy economy;

    private int amount = -1;
    private int bankMaxBalanceByLevel = -1;

    private int targetAmountToAdd = -1;
    private int targetBankMaxBalance = -1;
    private String targetPlayerName;

    public SubCommandAdd(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            bankAddUsage(player); // Mensaje
            return true;
        }

        try {
            List<String> playerNames = BU.getPlayerNameOfBank(plugin);

            if (playerNames.contains(args[1])) {

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    noPerm(player); // Mensaje
                    return true;
                }

                // Caso: /bank add <player> <amount>
                if (args.length < 3) {
                    bankTargetDepositFailure(player); // Mensaje
                    return true;
                }

                targetPlayerName = args[1];
                String playerAmountString = args[2];

                // Validar que <amount> sea un número
                try {
                    targetAmountToAdd = Integer.parseInt(playerAmountString);
                } catch (NumberFormatException e) {
                    bankTargetDepositFailure(player); // Mensaje
                    return true;
                }

                // Obtener el banco del jugador objetivo
                JsonObject targetBank = BU.getBankDataOfPlayerName(plugin, targetPlayerName);

                int targetPlayerBankBalance = BU.getPlayerBankBalance(targetBank);
                targetBankMaxBalance = BU.getBankMaxBalanceByLevel(plugin, targetPlayerName);

                // Verificar que la cantidad a recoger sea válida
                if (targetAmountToAdd <= 0) {
                    bankTargetDepositFailure(player); // Mensaje
                    return true;
                }

                // Verificar que el nuevo balance no exceda el máximo
                if ((targetPlayerBankBalance + targetAmountToAdd) > targetBankMaxBalance) {
                    bankTargetBalanceExceeds(player); // Mensaje
                    return true;
                }

                // Añadir el monto al banco del jugador objetivo
                BU.setPlayerBankBalance(targetBank, targetPlayerBankBalance + targetAmountToAdd);
                fileManager.updatePlayerInfo(targetBank, targetPlayerName);
                bankTargetDepositSuccess(player); // Mensaje

            } else {
                // Caso: /bank add {half-balance/all/mid-max} (sin <player>)
                String amountString = args[1];

                // Obtener datos del jugador que ejecuta el comando
                String playerName = player.getName();

                // Obtener el banco del jugador
                JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

                int playerBankBalance = BU.getPlayerBankBalance(bank);
                bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
                int playerEconomyBalance = BU.getPlayerBalance(player, economy);

                if (args[1].equalsIgnoreCase("half-balance")) {
                    amount = playerBankBalance / 2;

                    // Si el máximo de almacenamiento del banco es 500, deposita toda lo que tengo o toda hasta llegar al máximo
                } else if (args[1].equalsIgnoreCase("all")) {
                    amount = Math.min(playerEconomyBalance, (bankMaxBalanceByLevel - playerBankBalance));

                    // Si el máximo de almacenamiento del banco es 500, deposita 250
                } else if (args[1].equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalanceByLevel / 2;

                    // Caso: /bank add <amount>
                } else {

                    // Validar que <amount> sea un número
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankDepositFailure(player); // Mensaje
                        return true;
                    }
                }

                // Verificar que el jugador tenga suficiente dinero
                if (playerEconomyBalance < amount) {
                    playerNotEnoughtBalance(player); // Mensaje
                    return true;
                }

                // Verificar que el nuevo balance no exceda el máximo
                if ((playerBankBalance + amount) > bankMaxBalanceByLevel) {
                    bankBalanceExceeds(player); // Mensaje
                    return true;
                }

                // Restar dinero de la economía del jugador
                economy.withdrawPlayer(player, amount);

                // Añadir el monto al banco del jugador
                BU.setPlayerBankBalance(bank, playerBankBalance + amount);
                fileManager.updatePlayerInfo(bank, playerName);
                bankDepositSuccess(player); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankAddUsage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankBalanceExceeds(Player player) {
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalanceByLevel));

        for (String message : languageManager.getAllMessage("bank.add.deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerNotEnoughtBalance(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositSuccess(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(amount));

        for (String message : languageManager.getAllMessage("bank.add.deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetBalanceExceeds(Player player) {
        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(targetBankMaxBalance));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositSuccess(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(targetAmountToAdd));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.target-deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPerm(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
