package com.Guayand0.commands.subcommands;

import com.Guayand0.Data.BankData;
import com.Guayand0.Data.BankManager;
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

public class SubCommandData implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();
    private final BankManager BM = new BankManager();

    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private int bankMaxLevel = -1;
    private int bankLevelUpgradeCost = -1;
    private String playerName;
    private String targetPlayerName;

    public SubCommandData(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        playerName = player.getName();

        try {
            BankData bankData;

            // Jugador del que se van a obtener los datos del banco
            if (args.length > 1) {
                targetPlayerName = args[1];

                // Si targetPlayerName está en la lista del banco
                if (BU.getPlayerNameOfBank(plugin).contains(targetPlayerName)) {
                    bankData = BM.getBankData(plugin, targetPlayerName);
                } else {
                    // Si no está en la lista, obtener los datos del banco para el jugador actual
                    bankData = BM.getBankData(plugin, playerName);
                }
            } else {
                targetPlayerName = null;
                bankData = BM.getBankData(plugin, playerName);
            }

            // Obtener datos del banco del jugador y maximos de nivel y balance
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

            if (targetPlayerName != null) {
                if (BU.getPlayerNameOfBank(plugin).contains(targetPlayerName)) {
                    targetBankDataMessage(player); // Mensaje
                } else {
                    playerBankDataMessage(player); // Mensaje
                }
            } else {
                playerBankDataMessage(player); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void playerBankDataMessage(Player player) {
        plugin.placeholders.put("%playerbankname%", bankName);
        plugin.placeholders.put("%playername%", playerName);
        plugin.placeholders.put("%playerbanklevel%", String.valueOf(bankLevel));
        plugin.placeholders.put("%playerbankbalance%", String.valueOf(bankBalance));
        plugin.placeholders.put("%playerbankofflineprofitaccrued%", String.valueOf(offlineProfitAccrued));
        plugin.placeholders.put("%playerbankofflineprofittimes%", String.valueOf(offlineProfitTimes));

        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalance));
        plugin.placeholders.put("%playerbanklevelupgradecost%", String.valueOf(bankLevelUpgradeCost));

        plugin.placeholders.put("%playerbankmaxlevel%", String.valueOf(bankMaxLevel));

        for (String message : languageManager.getAllMessage("bank.data.player-bank-data")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void targetBankDataMessage(Player player) {
        plugin.placeholders.put("%targetbankname%", bankName);
        plugin.placeholders.put("%targetplayername%", targetPlayerName);
        plugin.placeholders.put("%targetbanklevel%", String.valueOf(bankLevel));
        plugin.placeholders.put("%targetbankbalance%", String.valueOf(bankBalance));
        plugin.placeholders.put("%targetbankofflineprofitaccrued%", String.valueOf(offlineProfitAccrued));
        plugin.placeholders.put("%targetbankofflineprofittimes%", String.valueOf(offlineProfitTimes));

        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(bankMaxBalance));
        plugin.placeholders.put("%targetbanklevelupgradecost%", String.valueOf(bankLevelUpgradeCost));

        plugin.placeholders.put("%targetbankmaxlevel%", String.valueOf(bankMaxLevel));

        for (String message : languageManager.getAllMessage("bank.data.target-bank-data")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
