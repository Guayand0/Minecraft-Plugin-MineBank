package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.Guayand0.zlib.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class LevelUpSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final PlayerUtils PU = new PlayerUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    public LevelUpSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            try {
                ph = plugin.buildPlayerPlaceholders(null);

                // args[0]=levelup, args[1]=target
                if (args.length < 2) {
                    sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                    return true;
                }

                String targetName = args[1];
                ph.put("%targetplayername%", targetName);
                UUID playerUUID = PU.getUUIDFromName(targetName);

                if (playerUUID == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
                if (playerData == null || playerData.getBank() == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                String bankName = playerData.getBank().getName();
                int bankLevel = playerData.getBank().getLevel();

                Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
                if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                    sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                    return true;
                }

                BankData bankData = bankDataMap.get(bankName);
                int bankMaxLevel = bankData.getLevels().size();

                if (bankLevel >= bankMaxLevel) {
                    sendMessage.send(sender, "bank.levelup.target-already-max-level", ph); // Mensaje
                    return true;
                }

                playerData.getBank().setLevel(bankLevel + 1);
                dataStorage.savePlayerData(playerUUID, playerData);

                ph.put("%targetBankLevel%", String.valueOf(bankLevel + 1));
                sendMessage.send(sender, "bank.levelup.target-levelup-success", ph); // Mensaje
            } catch (Exception e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
            }
            return true;
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = player.hasPermission(plugin.pluginName + ".admin") ? "bank.levelup.usage-admin" : "bank.levelup.usage";

        try {

            UUID playerUUID;
            PlayerData playerData;
            String targetName = "N/A";

            if (args.length >= 2) {
                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    sendMessage.send(sender, usageKey, ph); // Mensaje
                    return true;
                }

                targetName = args[1];
                playerUUID = PU.getUUIDFromName(targetName);

            } else {
                playerUUID = player.getUniqueId();
            }

            if (playerUUID == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            playerData = dataStorage.loadPlayerData(playerUUID);
            if (playerData == null || playerData.getBank() == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                return true;
            }

            BankData bankData = bankDataMap.get(bankName);
            int bankMaxLevel = bankData.getLevels().size();
            int upgradeCost = bankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();

            if (args.length >= 2) {
                ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
                ph.put("%targetplayername%", targetName);

                if (bankLevel >= bankMaxLevel) {
                    sendMessage.send(sender, "bank.levelup.target-already-max-level", ph); // Mensaje
                    return true;
                }

                playerData.getBank().setLevel(bankLevel + 1);
                dataStorage.savePlayerData(playerUUID, playerData);

                ph.put("%targetBankLevel%", String.valueOf(bankLevel + 1));
                sendMessage.send(sender,"bank.levelup.target-levelup-success", ph); // Mensaje

            } else {
                ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

                if (bankLevel >= bankMaxLevel) {
                    sendMessage.send(sender, "bank.levelup.already-max-level", ph); // Mensaje
                    return true;
                }

                // Datos del siguiente nivel
                BankData.Level nextLevel = bankData.getLevels().get(String.valueOf(bankLevel + 1));
                if (nextLevel == null) return true;

                int newMaxBalance = nextLevel.getMax_balance();

                // No tiene suficiente balance en el banco
                if (bankBalance < upgradeCost) {
                    ph.put("%playerbanknextlevelcost%", BSP.format(plugin, String.valueOf(upgradeCost)));
                    sendMessage.send(sender, "bank.levelup.not-enough-bank-balance", ph); // Mensaje
                    return true;
                }

                // Aplicar level up
                bankBalance -= upgradeCost;
                int newBankLevel = bankLevel + 1;

                playerData.getBank().setLevel(newBankLevel);
                playerData.getBank().setBalance(bankBalance);
                dataStorage.savePlayerData(player.getUniqueId(), playerData);

                ph.put("%newPlayerBankLevel%", String.valueOf(newBankLevel));
                ph.put("%newPlayerBankMaxBalance%", BSP.format(plugin, String.valueOf(newMaxBalance)));
                sendMessage.send(sender, "bank.levelup.levelup-success", ph); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
