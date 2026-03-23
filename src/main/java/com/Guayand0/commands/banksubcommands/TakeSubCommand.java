package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionService;
import com.Guayand0.managers.EventManager;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.Guayand0.zlib.PlayerUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TakeSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;
    private final TransactionService transactionService;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();
    private final PlayerUtils PU = new PlayerUtils();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();
    private final Map<UUID, Long> lastTakeTransactionTimes = new ConcurrentHashMap<>();

    private final Economy economy;

    public TakeSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
        this.transactionService = plugin.getTransactionService();

        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            try {
                ph = plugin.buildPlayerPlaceholders(null);

                // args[0]=take, args[1]=player, args[2]=target, args[3]=amount
                if (args.length < 4 || !args[1].equalsIgnoreCase("player")) {
                    sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                    return true;
                }

                String targetName = args[2];
                UUID uuid = PU.getUUIDFromName(targetName);
                String amountArg = args[3];

                if (uuid == null) {
                    ph.put("%targetPlayerName%", targetName);
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                ph = plugin.buildPlayerPlaceholders(uuid);

                PlayerData playerData = dataStorage.loadPlayerData(uuid);
                if (playerData == null) {
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

                int amountTaken;
                try {
                    amountTaken = Integer.parseInt(amountArg);
                } catch (NumberFormatException e) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }
                if (amountTaken <= 0) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                ph.put("%targetPlayerName%", targetName);

                if (bankBalance < amountTaken) {
                    sendMessage.send(sender, "bank.take.target-not-enough-bank-balance", ph); // Mensaje
                    return true;
                }

                int newBalance = bankBalance - amountTaken;
                playerData.getBank().setBalance(newBalance);
                dataStorage.savePlayerData(uuid, playerData);
                transactionService.register(uuid, "withdraw", amountTaken, "plugin", "console");

                ph.put("%amount%", BSP.format(plugin, String.valueOf(amountTaken)));
                sendMessage.send(sender, "bank.take.target-withdraw-success", ph); // Mensaje
            } catch (Exception e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
            }
            return true;
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = player.hasPermission(plugin.pluginName + ".admin") ? "bank.take.usage-admin" : "bank.take.usage";

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            sendMessage.send(sender, usageKey, ph); // Mensaje
            return true;
        }

        try {
            UUID uuid;
            String amountArg;
            String targetName = "N/A";

            if (args[1].equalsIgnoreCase("player")) {

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    sendMessage.send(sender, usageKey, ph); // Mensaje
                    return true;
                }

                // args[0]=take, args[1]=player, args[2]=target, args[3]=amount
                if (args.length < 4) {
                    sendMessage.send(sender, usageKey, ph); // Mensaje
                    return true;
                }

                targetName = args[2];
                uuid = PU.getUUIDFromName(targetName);
                amountArg = args[3];
            } else {
                uuid = player.getUniqueId();
                amountArg = args[1];
            }

            if (uuid == null) {
                ph.put("%targetPlayerName%", targetName);
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }
            ph = plugin.buildPlayerPlaceholders(uuid);

            // Cargar datos del jugador
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            // Datos del banco
            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                return true;
            }

            BankData bankData = bankDataMap.get(bankName);

            // Determinar cantidad a depositar
            int amountTaken;

            if (args[1].equalsIgnoreCase("player")) {

                try {
                    amountTaken = Integer.parseInt(amountArg);
                } catch (NumberFormatException e) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }
                if (amountTaken <= 0) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                ph.put("%targetPlayerName%", targetName);

                if (bankBalance < amountTaken) {
                    sendMessage.send(sender, "bank.take.target-not-enough-bank-balance", ph); // Mensaje
                    return true;
                }

                // Actualizar balance del banco
                int newBalance = bankBalance - amountTaken;
                playerData.getBank().setBalance(newBalance);
                dataStorage.savePlayerData(uuid, playerData);
                transactionService.register(uuid, "withdraw", amountTaken, "plugin", "admin");

                // Mensaje al ejecutor
                ph.put("%amount%", BSP.format(plugin, String.valueOf(amountTaken)));
                sendMessage.send(sender, "bank.take.target-withdraw-success", ph); // Mensaje

                /*// Mensaje al jugador objetivo si está online y activado en config

                withdraw-taked: '%chatPlugin% &eYou have lost &6%amount% &efrom your bank'

                Player targetPlayer = Bukkit.getPlayer(uuid);
                boolean alertPlayer = GV.getBoolean(plugin, "bank.admin-modify-bank-data-alert", false);
                if (targetPlayer != null && targetPlayer.isOnline() && alertPlayer) {
                    Map<String,String> phTarget = plugin.buildPlayerPlaceholders(uuid);
                    phTarget.put("%amount%", BSP.format(plugin, String.valueOf(amountTaken)));
                    sendMessage.send(targetPlayer, "bank.take.withdraw-taked", phTarget); // Mensaje
                }*/

            } else {

                int accruedInterestData = dataStorage.loadAccruedInterestData();
                int minBankBalanceToApplyInterest = GV.getInt(plugin, "bank.interest.min-bank-balance-to-apply", -1);
                double withdrawInterestPercentage = GV.getDouble(plugin, "bank.interest.withdraw-percentage", 0);
                boolean interestMultiplyByBankLevel = GV.getBoolean(plugin, "bank.interest.multiply-by-bank-level", false);
                double taxMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.TAX);

                double interestPercentage = 0;
                double interestAmount = 0;
                boolean isAllWithdraw = false;

                if (amountArg.equalsIgnoreCase("half-balance")) {
                    amountTaken = bankBalance / 2;
                } else if (amountArg.equalsIgnoreCase("all")) {

                    isAllWithdraw = true;

                    double interestPct = 0;
                    int interestInt = 0;

                    // Aplicar intereses SOLO si supera el mínimo
                    if (minBankBalanceToApplyInterest > -1 && bankBalance > minBankBalanceToApplyInterest) {
                        interestPct = interestMultiplyByBankLevel ? withdrawInterestPercentage * bankLevel : withdrawInterestPercentage;
                        interestPct = interestPct * taxMultiplier;
                        interestInt = (int) Math.floor(bankBalance * (interestPct / 100.0));
                    }

                    interestPercentage = interestPct;
                    interestAmount = interestInt;

                    amountTaken = bankBalance - interestInt;
                    if (amountTaken < 0) amountTaken = 0;

                } else if (amountArg.equalsIgnoreCase("mid-max")) {
                    amountTaken = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance() / 2;

                } else {
                    try {
                        amountTaken = Integer.parseInt(amountArg);
                    } catch (NumberFormatException e) {
                        sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                        return true;
                    }
                }

                if(amountTaken <= 0) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                int minTakeAmount = Math.max(0, GV.getInt(plugin, "bank.transactions.take.min-amount", 500));
                if (minTakeAmount > 0 && amountTaken < minTakeAmount) {
                    ph.put("%minTransactionAmount%", BSP.format(plugin, String.valueOf(minTakeAmount)));
                    sendMessage.send(sender, "bank.transaction.min-take-amount", ph);
                    return true;
                }

                long cooldownRemainingMs = getTakeTransactionRemainingMillis(uuid);
                if (cooldownRemainingMs > 0L) {
                    ph.put("%transactionCooldownRemaining%", String.valueOf(Math.max(1L, (long) Math.ceil(cooldownRemainingMs / 1000.0))));
                    sendMessage.send(sender, "bank.transaction.cooldown", ph);
                    return true;
                }

                if (bankBalance < amountTaken) {
                    sendMessage.send(sender, "bank.take.not-enough-bank-balance", ph); // Mensaje
                    return true;
                }

                if (!isAllWithdraw && minBankBalanceToApplyInterest > -1 && amountTaken > minBankBalanceToApplyInterest) {
                    if (interestMultiplyByBankLevel) {
                        interestPercentage = withdrawInterestPercentage * bankLevel;
                    } else {
                        interestPercentage = withdrawInterestPercentage;
                    }
                    interestPercentage = interestPercentage * taxMultiplier;

                    interestAmount = amountTaken * (interestPercentage / 100.0);
                }

                int interestsAmount = (int) Math.round(interestAmount);

                int totalBankDeduction;
                if (isAllWithdraw) {
                    totalBankDeduction = bankBalance;
                } else {
                    totalBankDeduction = amountTaken + interestsAmount;
                }

                if (bankBalance < totalBankDeduction) {
                    sendMessage.send(sender, "bank.take.withdraw-exceeds", ph); // Mensaje
                    return true;
                }

                int newBalance = bankBalance - totalBankDeduction;
                playerData.getBank().setBalance(newBalance);
                dataStorage.savePlayerData(player.getUniqueId(), playerData);
                dataStorage.saveAccruedInterestData(accruedInterestData + interestsAmount);
                transactionService.register(player.getUniqueId(), "withdraw", amountTaken, "plugin", "self");
                markTakeTransactionNow(uuid);

                economy.depositPlayer(player, amountTaken);

                ph.put("%amountReceived%", BSP.format(plugin, String.valueOf(amountTaken)));
                ph.put("%amountDeducted%", BSP.format(plugin, String.valueOf(totalBankDeduction)));
                ph.put("%interestPercentage%", String.valueOf(interestPercentage));

                sendMessage.send(sender, "bank.take.withdraw-success", ph); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }

    private long getTakeTransactionRemainingMillis(UUID uuid) {
        if (uuid == null) return 0L;
        int cooldownSeconds = Math.max(0, GV.getInt(plugin, "bank.transactions.take.cooldown-seconds", 10));
        if (cooldownSeconds <= 0) return 0L;
        Long lastTime = lastTakeTransactionTimes.get(uuid);
        if (lastTime == null) return 0L;
        long remaining = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastTime);
        return Math.max(0L, remaining);
    }

    private void markTakeTransactionNow(UUID uuid) {
        if (uuid != null) {
            lastTakeTransactionTimes.put(uuid, System.currentTimeMillis());
        }
    }
}
