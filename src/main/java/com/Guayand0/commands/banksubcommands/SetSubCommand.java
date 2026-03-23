package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionService;
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

public class SetSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;
    private final TransactionService transactionService;

    private final PlayerUtils PU = new PlayerUtils();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final GetValues GV = new GetValues();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    public SetSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
        this.transactionService = plugin.getTransactionService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            try {
                ph = plugin.buildPlayerPlaceholders(null);

                // args[0]=set, args[1]=target, args[2]=type, args[3]=amount
                if (args.length < 4) {
                    sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                    return true;
                }

                String targetPlayerName = args[1];
                String type = args[2];
                String amountString = args[3];

                ph.put("%targetplayername%", targetPlayerName);

                UUID targetUUID = PU.getUUIDFromName(targetPlayerName);
                if (targetUUID == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                PlayerData targetData = dataStorage.loadPlayerData(targetUUID);
                if (targetData == null || targetData.getBank() == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                String bankName = targetData.getBank().getName();
                int bankLevel = targetData.getBank().getLevel();

                Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
                if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                    sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                    return true;
                }

                BankData bankData = bankDataMap.get(bankName);
                int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
                int bankMaxLevel = bankData.getLevels().size();
                int amount;

                if (type.equalsIgnoreCase("balance")) {

                    amount = resolveAmount(amountString, bankMaxBalance);
                    if (amount < 0) {
                        sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                        return true;
                    }

                    if (amount > bankMaxBalance) {
                        ph.put("%targetbankmaxbalance%", BSP.format(plugin, String.valueOf(bankMaxBalance)));
                        sendMessage.send(sender, "bank.set.max-balance", ph); // Mensaje
                        return true;
                    }

                    targetData.getBank().setBalance(amount);
                    dataStorage.savePlayerData(targetUUID, targetData);
                    transactionService.register(targetUUID, "set", amount, "plugin", "console");

                    ph.put("%amount%", BSP.format(plugin, String.valueOf(amount)));
                    sendMessage.send(sender, "bank.set.set-balance-success", ph); // Mensaje

                } else if (type.equalsIgnoreCase("level")) {

                    amount = resolveAmount(amountString, bankMaxLevel);
                    if (amount <= 0) {
                        sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                        return true;
                    }

                    if (amount > bankMaxLevel) {
                        ph.put("%targetbankmaxlevel%", String.valueOf(bankMaxLevel));
                        sendMessage.send(sender, "bank.set.max-level", ph); // Mensaje
                        return true;
                    }

                    targetData.getBank().setLevel(amount);
                    dataStorage.savePlayerData(targetUUID, targetData);

                    ph.put("%amount%", String.valueOf(amount));
                    sendMessage.send(sender, "bank.set.set-level-success", ph); // Mensaje

                } else {
                    sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
            }
            return true;
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = "bank.set.usage-admin";

        if (!player.hasPermission(plugin.pluginName + ".admin")) {
            sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
            return true;
        }

        if (args.length < 4) {
            sendMessage.send(sender, usageKey, ph); // Mensaje
            return true;
        }

        try {
            String targetPlayerName = args[1];
            String type = args[2];
            String amountString = args[3];

            ph.put("%targetplayername%", targetPlayerName);

            // Resolver UUID del jugador objetivo
            UUID targetUUID = PU.getUUIDFromName(targetPlayerName);
            if (targetUUID == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            // Cargar datos del jugador objetivo
            PlayerData targetData = dataStorage.loadPlayerData(targetUUID);
            if (targetData == null || targetData.getBank() == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = targetData.getBank().getName();
            int bankLevel = targetData.getBank().getLevel();

            // Cargar datos del banco
            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                return true;
            }

            BankData bankData = bankDataMap.get(bankName);
            int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
            int bankMaxLevel = bankData.getLevels().size();
            int amount;

            // ================= BALANCE =================
            if (type.equalsIgnoreCase("balance")) {

                amount = resolveAmount(amountString, bankMaxBalance);
                if (amount < 0) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                if (amount > bankMaxBalance) {
                    ph.put("%targetbankmaxbalance%", BSP.format(plugin, String.valueOf(bankMaxBalance)));
                    sendMessage.send(sender, "bank.set.max-balance", ph); // Mensaje
                    return true;
                }

                targetData.getBank().setBalance(amount);
                dataStorage.savePlayerData(targetUUID, targetData);
                transactionService.register(targetUUID, "set", amount, "plugin", "admin");

                ph.put("%amount%", BSP.format(plugin, String.valueOf(amount)));
                sendMessage.send(sender, "bank.set.set-balance-success", ph); // Mensaje

                // ================= LEVEL =================
            } else if (type.equalsIgnoreCase("level")) {

                amount = resolveAmount(amountString, bankMaxLevel);
                if (amount <= 0) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                if (amount > bankMaxLevel) {
                    ph.put("%targetbankmaxlevel%", String.valueOf(bankMaxLevel));
                    sendMessage.send(sender, "bank.set.max-level", ph); // Mensaje
                    return true;
                }

                targetData.getBank().setLevel(amount);
                dataStorage.savePlayerData(targetUUID, targetData);

                ph.put("%amount%", String.valueOf(amount));
                sendMessage.send(sender, "bank.set.set-level-success", ph); // Mensaje

            } else {
                sendMessage.send(sender, usageKey, ph); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }

    private int resolveAmount(String input, int max) {
        if (input.equalsIgnoreCase("mid-max")) return max / 2;
        if (input.equalsIgnoreCase("max")) return max;

        try {
            int value = Integer.parseInt(input);
            return value >= 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
