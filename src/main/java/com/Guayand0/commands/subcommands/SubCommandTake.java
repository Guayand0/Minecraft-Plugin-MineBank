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

import java.util.List;

public class SubCommandTake implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private final Economy economy;

    private int amount = -1;
    private int amountReceived = -1;
    private double interestPercentage = -1;

    private int targetAmountToRemove = -1;
    private String targetPlayerName;

    public SubCommandTake(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        // Si el comando tiene menos de 2 argumentos
        if (args.length < 2) {
            bankTakeUsage(player); // Mensaje
            return true;
        }

        try {

            List<String> playerNames = BU.getPlayerNameOfBank(plugin);

            if (playerNames.contains(args[1])) {

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    noPerm(player); // Mensaje
                    return true;
                }

                // Caso: /bank take <player> <amount>
                if (args.length < 3) {
                    bankTargetWithdrawFailure(player); // Mensaje
                    return true;
                }

                targetPlayerName = args[1];
                String playerAmountString = args[2];

                try {
                    targetAmountToRemove = Integer.parseInt(playerAmountString);
                } catch (NumberFormatException e) {
                    bankTargetWithdrawFailure(player); // Mensaje
                    return true;
                }

                // Obtener el banco del jugador objetivo
                JsonObject targetBank = BU.getBankDataOfPlayerName(plugin, targetPlayerName);

                int targetPlayerBankBalance = BU.getPlayerBankBalance(targetBank);

                // Verificar que la cantidad a recoger sea válida
                if (targetAmountToRemove <= 0) {
                    bankTargetWithdrawFailure(player); // Mensaje
                    return true;
                }

                if (targetPlayerBankBalance < targetAmountToRemove) {
                    targetNotEnoughtBankBalance(player); // Mensaje
                    return true;
                }

                BU.setPlayerBankBalance(targetBank, targetPlayerBankBalance - targetAmountToRemove);
                fileManager.updatePlayerInfo(targetBank, targetPlayerName);
                bankTargetWithdrawSuccess(player); // Mensaje

            } else {

                // Obtener datos del jugador que ejecuta el comando
                String playerName = player.getName();

                // Obtener el banco del jugador
                JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

                int playerBankBalance = BU.getPlayerBankBalance(bank);
                int playerBankLevel = BU.getPlayerBankLevel(bank);
                int bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
                int accruedInterestData = BU.getAccruedInterestData(plugin);
                int minBankBalanceToApplyInterest = BU.getInterestMinBankBalanceToApply(plugin);
                double withdrawInterestPercentage = BU.getInterestWithdrawPercentage(plugin);
                boolean interestMultiplyByBankLevel = BU.getInterestMultiplyByBankLevel(plugin);

                // Caso: /bank take <amount>
                String amountString = args[1];

                // Si en el banco tengo 500, recoge 250
                if (amountString.equalsIgnoreCase("half-balance")) {
                    amount = playerBankBalance / 2;

                    // Si el maximo de almacenamiento del banco es 500, recoge toda lo que tengo
                } else if (amountString.equalsIgnoreCase("all")) {
                    amount = playerBankBalance;

                    // Si el maximo de almacenamiento del banco es 500, recoge 250
                } else if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalanceByLevel / 2;

                    // Si no se usa ninguno de esos se obtiene un valor y se comprueba que sea número valido
                } else {
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankWithdrawFailure(player); // Mensaje
                        return true;
                    }
                }

                // Verificar que la cantidad a recoger sea válida
                if (amount <= 0) {
                    bankWithdrawFailure(player); // Mensaje
                    return true;
                }

                // Verificar si el banco tiene suficiente dinero
                if (playerBankBalance < amount) {
                    playerNotEnoughtBankBalance(player); // Mensaje
                    return true;
                }

                // Calcular el total a recibir con intereses
                double interestAmount;
                if (interestMultiplyByBankLevel) {
                    interestAmount = amount * ((withdrawInterestPercentage * playerBankLevel) / 100.0);
                    interestPercentage = Math.round((withdrawInterestPercentage * playerBankLevel) * 100.0) / 100.0;
                } else {
                    interestAmount = amount * (withdrawInterestPercentage / 100.0);
                    interestPercentage = Math.round(withdrawInterestPercentage * 100.0) / 100.0;
                }

                amountReceived = amount - (int) Math.round(interestAmount);

                // Comprueba el minimo para tener intereses
                if(amount <= minBankBalanceToApplyInterest || minBankBalanceToApplyInterest <= -1) {
                    amountReceived = amount;
                    interestPercentage = 0;
                }

                // Verificar si el total a recibir es válido
                if (amount > bankMaxBalanceByLevel || amountReceived > bankMaxBalanceByLevel || amountReceived <= 0) {
                    bankWithdrawExceeds(player); // Mensaje
                    return true;
                }

                // Calcular la cantidad de intereses que se queda el banco
                int interestsAmount = amount - amountReceived;

                // Establecer la cantidad de intereses acumulados
                fileManager.updateInterestsData(accruedInterestData + interestsAmount);

                // Devolver el dinero al jugador
                economy.depositPlayer(player, amountReceived);

                // Establecer nuevo balance del banco
                BU.setPlayerBankBalance(bank, playerBankBalance - amount);

                // Actualizar solo datos del banco
                fileManager.updatePlayerInfo(bank, player.getName());

                bankWithdrawSuccess(player);  // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankTakeUsage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawSuccess(Player player) {
        plugin.placeholders.put("%amountreceived%", String.valueOf(amountReceived));
        plugin.placeholders.put("%amountdeducted%", String.valueOf(amount));
        plugin.placeholders.put("%interestpercentage%", String.valueOf(interestPercentage));

        for (String message : languageManager.getAllMessage("bank.take.withdraw-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.withdraw-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawExceeds(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.withdraw-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerNotEnoughtBankBalance(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetWithdrawSuccess(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(targetAmountToRemove));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.take.target-withdraw-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetWithdrawFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.target-withdraw-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void targetNotEnoughtBankBalance(Player player) {
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.take.target-not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPerm(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
