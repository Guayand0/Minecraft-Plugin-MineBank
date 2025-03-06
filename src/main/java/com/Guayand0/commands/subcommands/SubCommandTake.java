package com.Guayand0.commands.subcommands;

import com.Guayand0.data.BankData;
import com.Guayand0.data.interest.JSON.JSONGetInterestData;
import com.Guayand0.data.player.JSON.JSONGetPlayerData;
import com.Guayand0.data.config.GetConfigData;
import com.Guayand0.data.player.JSON.JSONGetPlayerNames;
import com.Guayand0.data.player.JSON.JSONSetPlayerBankData;
import com.Guayand0.data.player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
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
    private final JSONGetPlayerData BM = new JSONGetPlayerData();
    private final JSONSetPlayerBankData SPBD = new JSONSetPlayerBankData();
    private final JSONGetPlayerNames GPN = new JSONGetPlayerNames();
    private final GetConfigData GCD = new GetConfigData();
    private final JSONGetInterestData GID = new JSONGetInterestData();

    private final Economy economy;

    private int amount = -1;
    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private String targetPlayerName;
    private int amountReceived = -1;
    private double interestPercentage = -1;

    public SubCommandTake(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        // Si el comando tiene menos de 2 argumentos
        if (args.length < 2) {
            bankTakeUsageMessage(player); // Mensaje
            return true;
        }

        try {
            BankData bankData;

            List<String> playerNames = GPN.getAllRegisteredPlayerName(plugin);
            targetPlayerName = args[1];

            if (playerNames.contains(args[1])) {

                // Obtener datos del banco del jugador y maximos de nivel y balance
                bankData = BM.getPlayerBankData(plugin, targetPlayerName);
                if (bankData != null) {
                    bankName = bankData.getBankName();
                    bankLevel = bankData.getBankLevel();
                    bankBalance = bankData.getBankBalance();
                    offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                    offlineProfitTimes = bankData.getOfflineProfitTimes();
                    bankMaxBalance = bankData.getBankMaxBalance();
                }

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    noPermMessage(player); // Mensaje
                    return true;
                }

                // Caso: /bank take <player> <amount>
                if (args.length < 3) {
                    bankTargetWithdrawFailureMessage(player); // Mensaje
                    return true;
                }

                // Validar que <amount> sea un número
                try {
                    String playerAmountString = args[2];
                    amount = Integer.parseInt(playerAmountString);
                } catch (NumberFormatException e) {
                    bankTargetWithdrawFailureMessage(player); // Mensaje
                    return true;
                }

                // Verificar que la cantidad a recoger sea válida
                if (amount <= 0) {
                    bankTargetWithdrawFailureMessage(player); // Mensaje
                    return true;
                }

                if (bankBalance < amount) {
                    targetNotEnoughtBankBalanceMessage(player); // Mensaje
                    return true;
                }

                bankBalance = bankBalance - amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, targetPlayerName, newBankData);
                if (success) {
                    bankTargetWithdrawSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }

            } else {

                // Obtener datos del banco del jugador y maximos de nivel y balance
                bankData = BM.getPlayerBankData(plugin, playerName);
                if (bankData != null) {
                    bankName = bankData.getBankName();
                    bankLevel = bankData.getBankLevel();
                    bankBalance = bankData.getBankBalance();
                    offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                    offlineProfitTimes = bankData.getOfflineProfitTimes();
                    bankMaxBalance = bankData.getBankMaxBalance();
                }

                int accruedInterestData = GID.getAccruedInterestData(plugin);
                int minBankBalanceToApplyInterest = GCD.getInterestMinBankBalanceToApply(plugin);
                double withdrawInterestPercentage = GCD.getInterestWithdrawPercentage(plugin);
                boolean interestMultiplyByBankLevel = GCD.getInterestMultiplyByBankLevel(plugin);

                // Caso: /bank take {half-balance/all/mid-max} (sin <player>)
                String amountString = args[1];

                // Si en el banco tengo 500, recoge 250
                if (amountString.equalsIgnoreCase("half-balance")) {
                    amount = bankBalance / 2;

                    // Si el maximo de almacenamiento del banco es 500, recoge toda lo que tengo
                } else if (amountString.equalsIgnoreCase("all")) {
                    amount = bankBalance;

                    // Si el maximo de almacenamiento del banco es 500, recoge 250
                } else if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalance / 2;

                    // Caso: /bank add <amount>
                } else {

                    // Validar que <amount> sea un número
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankWithdrawFailureMessage(player); // Mensaje
                        return true;
                    }
                }

                // Verificar que la cantidad a recoger sea válida
                if (amount <= 0) {
                    bankWithdrawFailureMessage(player); // Mensaje
                    return true;
                }

                // Verificar si el banco tiene suficiente dinero
                if (bankBalance < amount) {
                    playerNotEnoughtBankBalanceMessage(player); // Mensaje
                    return true;
                }

                // Calcular el total a recibir con intereses
                double interestAmount;
                if (interestMultiplyByBankLevel) {
                    interestAmount = amount * ((withdrawInterestPercentage * bankLevel) / 100.0);
                    interestPercentage = Math.round((withdrawInterestPercentage * bankLevel) * 100.0) / 100.0;
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
                if (amount > bankMaxBalance || amountReceived > bankMaxBalance || amountReceived <= 0) {
                    bankWithdrawExceedsMessage(player); // Mensaje
                    return true;
                }

                // Calcular la cantidad de intereses que se queda el banco
                int interestsAmount = amount - amountReceived;

                // Establecer la cantidad de intereses acumulados
                fileManager.updateInterestsData(accruedInterestData + interestsAmount);

                // Devolver el dinero al jugador
                economy.depositPlayer(player, amountReceived);

                bankBalance = bankBalance - amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                if (success) {
                    bankWithdrawSuccessMessage(player);  // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankTakeUsageMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawSuccessMessage(Player player) {
        plugin.placeholders.put("%amountreceived%", String.valueOf(amountReceived));
        plugin.placeholders.put("%amountdeducted%", String.valueOf(amount));
        plugin.placeholders.put("%interestpercentage%", String.valueOf(interestPercentage));

        for (String message : languageManager.getAllMessage("bank.take.withdraw-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.withdraw-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankWithdrawExceedsMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.withdraw-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerNotEnoughtBankBalanceMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetWithdrawSuccessMessage(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(amount));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.take.target-withdraw-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetWithdrawFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.take.target-withdraw-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void targetNotEnoughtBankBalanceMessage(Player player) {
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.take.target-not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPermMessage(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
