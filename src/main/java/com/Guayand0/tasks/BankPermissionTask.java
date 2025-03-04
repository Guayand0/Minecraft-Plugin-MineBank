package com.Guayand0.tasks;

import com.Guayand0.Data.Player.JSON.GetPlayerBankData;
import com.Guayand0.Data.Player.JSON.SetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class BankPermissionTask extends BukkitRunnable {

    private final MineBank plugin;

    private final BankUtils BU = new BankUtils();
    private final MessageUtils MU = new MessageUtils();
    private final GetPlayerBankData GPBD = new GetPlayerBankData();
    private final SetPlayerBankData SPBD = new SetPlayerBankData();

    public BankPermissionTask(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        startBankCheckTask();
    }

    // Comprobar si el banco de cada jugador coincide con el que deberia tener
    public void startBankCheckTask() {
        try {

            // Para cada jugador conectado comprobar si su banco es el correcto
            for (Player player : Bukkit.getOnlinePlayers()) {

                String playerName = player.getName();

                PlayerBankData playerBankData = GPBD.getPlayerBankData(plugin, playerName);
                String bankName = "NULL";
                int bankBalance = -1;
                int bankLevel = -1;
                int offlineProfitAccrued = -1;
                int offlineProfitTimes = -1;

                // Obtener datos del banco: nombre, nivel, dinero, beneficio offline
                if (playerBankData != null) {
                    bankName = playerBankData.getName();
                    bankBalance = playerBankData.getBalance();
                    bankLevel = playerBankData.getLevel();
                    offlineProfitAccrued = playerBankData.getOfflineAccruedProfit();
                    offlineProfitTimes = playerBankData.getOfflineProfitTimes();
                }

                List<String> bankNames = BU.getBankNames(plugin);
                boolean adminLastBank = BU.getBankAdminShouldHaveLastBank(plugin);

                // Banco que el jugador deberia tener
                String rightBank;

                // Si el jugador tiene permiso de admin y esta activado en la config
                if (player.hasPermission(plugin.pluginName + ".admin") && adminLastBank) rightBank = bankNames.get(bankNames.size() - 1); // Obtener el último banco de la lista
                else rightBank = getBankFromPermissions(player); // Buscar un permiso de banco

                // Si el banco no coincide con su permiso o condición, cambiar el banco
                if (rightBank != null && !rightBank.equals(bankName)) {

                    bankName = rightBank;
                    // Establecer nuevos valores de banco
                    PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                    boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                    if (!success) {
                        Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }
    }

    // Obtener el banco correcto basado en los permisos del jugador
    private String getBankFromPermissions(Player player) throws IOException {

        List<String> bankNames = BU.getBankNames(plugin);
        String selectedBank = null;

        // Iterar sobre los bancos en orden
        for (String bankName : bankNames) {
            String permissionName = "minebank.bank." + bankName.toLowerCase();
            // Si el jugador tiene el permiso para un banco específico
            if (player.hasPermission(permissionName)) selectedBank = bankName; // Actualizar el banco seleccionado
        }

        // Si no tiene ningún permiso de banco, asignar el primer banco
        return selectedBank != null ? selectedBank : bankNames.get(0);
    }
}
