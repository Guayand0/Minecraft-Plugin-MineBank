package com.Guayand0.commands.subcommands;

import com.Guayand0.Data.BankData;
import com.Guayand0.Data.BankManager;
import com.Guayand0.Data.Player.JSON.SetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
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

public class SubCommandSet implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankManager BM = new BankManager();
    private final SetPlayerBankData SPBD = new SetPlayerBankData();

    int amount = -1;
    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private int bankMaxLevel = -1;
    private String targetPlayerName;

    public SubCommandSet(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        // Si el comando tiene menos de 4 argumentos
        if (args.length < 4) {
            bankSetUsageMessage(player); // Mensaje
            return true;
        }

        try {

            targetPlayerName = args[1];
            String bankLevelOrBalance = args[2];
            String amountString = args[3];

            // Obtener datos del banco del jugador y maximos de nivel y balance
            BankData bankData = BM.getBankData(plugin, playerName);
            if (bankData != null) {
                bankName = bankData.getBankName();
                bankLevel = bankData.getBankLevel();
                bankBalance = bankData.getBankBalance();
                offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                offlineProfitTimes = bankData.getOfflineProfitTimes();
                bankMaxLevel = bankData.getBankMaxLevel();
                bankMaxBalance = bankData.getBankMaxBalance();
            }

            if (bankLevelOrBalance.equalsIgnoreCase("balance")) {

                // Mitad del maximo de almacenamiento de banco
                if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalance / 2;

                    // Total del maximo de almacenamiento de banco
                } else if (amountString.equalsIgnoreCase("max")) {
                    amount = bankMaxBalance;

                    // Si no se usa ninguno de esos se obtiene un valor y se comprueba que sea número válido
                } else {

                    // Validar que <amount> sea un número
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankSetAmountFailureMessage(player); // Mensaje
                        return true;
                    }

                    if (amount > bankMaxBalance) {
                        bankSetMaxBalanceMessage(player); // Mensaje
                        return true;
                    }

                    if (amount < 0) {
                        bankSetAmountFailureMessage(player); // Mensaje
                        return true;
                    }
                }

                bankBalance = amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, targetPlayerName, newBankData);
                if (success) {
                    bankSetBalaceSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }

            } else if (bankLevelOrBalance.equalsIgnoreCase("level")) {

                // Mitad del maximo de nivel de banco
                if (amountString.equalsIgnoreCase("mid-max")) {
                    amount = bankMaxLevel / 2;

                    // Total del maximo de nivel de banco
                } else if (amountString.equalsIgnoreCase("max")) {
                    amount = bankMaxLevel;

                    // Si no se usa ninguno de esos se obtiene un valor y se comprueba que sea número válido
                } else {

                    // Validar que <amount> sea un número
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankSetAmountFailureMessage(player); // Mensaje
                        return true;
                    }

                    if (amount > bankMaxLevel) {
                        bankSetMaxLevelMessage(player); // Mensaje
                        return true;
                    }

                    if (amount < 0) {
                        bankSetAmountFailureMessage(player); // Mensaje
                        return true;
                    }
                }

                bankLevel = amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, targetPlayerName, newBankData);
                if (success) {
                    bankSetLevelSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }

            } else {
                bankSetUsageMessage(player);  // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankSetUsageMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.set-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetBalaceSuccessMessage(Player player) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%amount%", String.valueOf(amount));

        for (String message : languageManager.getAllMessage("bank.set.set-balance-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetLevelSuccessMessage(Player player) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%amount%", String.valueOf(amount));

        for (String message : languageManager.getAllMessage("bank.set.set-level-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetMaxBalanceMessage(Player player) {
        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(bankMaxBalance));

        for (String message : languageManager.getAllMessage("bank.set.max-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetMaxLevelMessage(Player player) {
        plugin.placeholders.put("%targetbankmaxlevel%", String.valueOf(bankMaxLevel));

        for (String message : languageManager.getAllMessage("bank.set.max-level")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankSetAmountFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.set.set-amount-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetPlayerNotFoundMessage(Player player, String targetPlayerName) {
        plugin.placeholders.put("%targetplayername%", targetPlayerName);

        for (String message : languageManager.getAllMessage("bank.set.target-player-not-found")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
