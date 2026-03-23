package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class OnPlayerJoin implements Listener {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public OnPlayerJoin(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getWebTokenStore().markConnected(player.getUniqueId());

        try {
            PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());

            if (playerData == null) {
                playerData = PlayerData.defaultData(plugin);
                dataStorage.savePlayerData(player.getUniqueId(), playerData);
            }

            boolean bankUseAllowed = GV.getBoolean(plugin, "config.bank-allowed", true);
            int offlineProfitAccrued = playerData.getBank().getOffline().getAccrued_profit();
            // Si el banco esta activado y el jugador tiene beneficios acumulados
            if (bankUseAllowed && offlineProfitAccrued > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
                        sendMessage.send((CommandSender) player, "bank.profit.offline-accumulated", ph); // Mensaje
                    }
                }.runTaskLater(plugin, 10); // Ejecuta la tarea despues de 10 ticks (0.5 segundos)
            }

            recalculateBankLevel(player);

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
        }
    }

    private void recalculateBankLevel(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PlayerData data = dataStorage.loadPlayerData(player.getUniqueId());
                    if (data == null) return;

                    String bankName = data.getBank().getName();
                    int currentLevel = data.getBank().getLevel();

                    // Cargar datos del banco
                    Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
                    BankData bankData = bankDataMap.get(bankName);
                    if (bankData == null) return;

                    int maxLevel = bankData.getLevels().size();

                    // Ajustar nivel si es mayor al permitido
                    if (currentLevel > maxLevel) {
                        data.getBank().setLevel(maxLevel);
                        dataStorage.savePlayerData(player.getUniqueId(), data);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                }
            }
        }.runTaskLater(plugin, 10); // Ejecuta la tarea despues de 10 ticks (0.5 segundos)
    }
}
