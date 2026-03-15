package com.Guayand0.tasks;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.managers.EventManager;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfitBankTask extends BukkitRunnable {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final PlayerUtils PU = new PlayerUtils();
    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    public ProfitBankTask(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public void run() {

        boolean bankEnabled = GV.getBoolean(plugin, "config.bank-allowed", true);
        if (!bankEnabled) return;

        List<UUID> uuids = dataStorage.getAllPlayerUUIDs();

        for (UUID uuid : uuids) {
            handlePlayer(uuid);
        }
    }

    private void handlePlayer(UUID uuid) {
        try {
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) return;

            PlayerData.Bank bank = playerData.getBank();

            String bankName = bank.getName();
            int bankLevel = bank.getLevel();
            int bankBalance = bank.getBalance();

            // Cargar datos del banco
            Map<String, BankData> bankMap = dataStorage.loadBankData(bankName);
            if (bankMap == null || !bankMap.containsKey(bankName)) return;

            BankData bankData = bankMap.get(bankName);
            BankData.Level levelData = bankData.getLevels().get(String.valueOf(bankLevel));
            if (levelData == null) return;

            int bankMaxBalance = levelData.getMax_balance();

            int minBalance = GV.getInt(plugin, "bank.profit.min-bank-balance-to-receive", 0);
            double profitPercent = GV.getDouble(plugin, "bank.profit.keep-in-bank-percentage", 0);
            boolean multiplyByLevel = GV.getBoolean(plugin, "bank.profit.multiply-by-bank-level", false);
            int maxOfflineTimes = GV.getInt(plugin, "bank.profit.times-profits-offline", 0);

            if (bankBalance <= 0 || bankBalance > bankMaxBalance) return;
            if (bankBalance < minBalance) return;

            double finalPercent = multiplyByLevel ? profitPercent * bankLevel : profitPercent;
            double eventMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.PROFIT);
            finalPercent = finalPercent * eventMultiplier;
            int profit = (int) Math.floor(bankBalance * finalPercent / 100.0);
            if (profit <= 0) return;

            boolean online = PU.isPlayerOnline(PU.getNameFromUUID(uuid));
            Player player = online ? Bukkit.getPlayer(uuid) : null;

            // OFFLINE
            if (!online) {
                PlayerData.Offline offline = bank.getOffline();

                if (maxOfflineTimes > 0 && offline.getProfit_times() >= maxOfflineTimes) return;

                offline.setAccrued_profit(offline.getAccrued_profit() + profit);
                offline.setProfit_times(offline.getProfit_times() + 1);

                dataStorage.savePlayerData(uuid, playerData);
                return;
            }

            // ONLINE
            if (!player.hasPermission(plugin.pluginName + ".use")) {
                return;
            }

            if (bankBalance + profit > bankMaxBalance) {
                sendMessage.send(player, "bank.profit.max-storage", null); // Mensaje
                return;
            }

            bank.setBalance(bankBalance + profit);
            dataStorage.savePlayerData(uuid, playerData);

            Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
            ph.put("%keepinbankprofit%", BSP.format(plugin, String.valueOf(profit)));
            ph.put("%profitpercentage%", String.valueOf(finalPercent));
            sendMessage.send(player, "bank.profit.received", ph); // Mensaje
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
        }
    }
}
