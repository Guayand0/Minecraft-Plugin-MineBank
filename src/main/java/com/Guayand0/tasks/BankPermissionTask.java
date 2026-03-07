package com.Guayand0.tasks;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankPermissionTask extends BukkitRunnable {

    private final MineBank plugin;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();


    public BankPermissionTask(MineBank plugin) {
        this.plugin = plugin;
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public void run() {
        boolean bankEnabled = GV.getBoolean(plugin, "config.bank-allowed", true);
        if (!bankEnabled) return;

        // Para cada jugador conectado comprobar si su banco es el correcto
        for (Player player : Bukkit.getOnlinePlayers()) {
            handlePlayerPermission(player);
        }
    }

    private void handlePlayerPermission(Player player) {
        try {
            List<String> bankNames = dataStorage.getAllBankNames();
            boolean adminBetterBank = GV.getBoolean(plugin, "bank.admin-better-bank", false);

            String finalBank;
            // Si el jugador tiene permiso de admin y esta activado en la config
            if (player.hasPermission(plugin.pluginName + ".admin") && adminBetterBank) finalBank = bankNames.get(bankNames.size() - 1); // Obtener el último banco de la lista
            else finalBank = getBankFromPermissions(player); // Buscar un permiso de banco

            // Cargar datos del jugador objetivo
            UUID playerUUID = player.getUniqueId();
            PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            String bankName = playerData.getBank().getName();

            if (finalBank != null && !finalBank.equalsIgnoreCase(bankName)) {

                int oldLevel = playerData.getBank().getLevel();

                // niveles del nuevo banco
                Map<String, BankData> bankDataMap = dataStorage.loadBankData(finalBank);
                BankData bankData = bankDataMap.get(finalBank);
                int maxNewLevel = bankData.getLevels().size();

                // cambiar banco
                playerData.getBank().setName(finalBank);

                // ajustar nivel si supera el máximo del nuevo banco
                if (oldLevel > maxNewLevel) {
                    playerData.getBank().setLevel(maxNewLevel);
                }

                dataStorage.savePlayerData(player.getUniqueId(), playerData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
        }
    }

    // Obtener el banco correcto basado en los permisos del jugador
    private String getBankFromPermissions(Player player) {

        List<String> bankNames = dataStorage.getAllBankNames();

        // recorrer de MAYOR a MENOR prioridad
        for (int i = bankNames.size() - 1; i >= 0; i--) {

            String bankName = bankNames.get(i);
            String permission = "minebank.bank." + bankName.toLowerCase();

            if (player.hasPermission(permission)) {
                return bankName; // máxima prioridad encontrada
            }
        }

        // si no tiene ningún permiso, usar el banco base
        return bankNames.isEmpty() ? null : bankNames.get(0);
    }
}