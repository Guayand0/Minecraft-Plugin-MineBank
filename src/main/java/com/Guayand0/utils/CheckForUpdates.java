package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.data.config.GetConfigData;
import com.Guayand0.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CheckForUpdates implements Listener {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final UpdateChecker UC = new UpdateChecker();
    private final MessageUtils MU = new MessageUtils();
    private final GetConfigData GCD = new GetConfigData();

    public CheckForUpdates(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @EventHandler
    public void checkUpdate(PlayerJoinEvent event) {
        try {

            Player player = event.getPlayer();
            if (!plugin.updateCheckerWork) plugin.comprobarActualizaciones();
            boolean updateCheckerAllowed = GCD.getUpdateCheckerAllowed(plugin);

            // Si el mensaje esta activado y hay nueva version
            if (updateCheckerAllowed && newVersionAvailable()) {

                // Si el jugador tiene permisos
                if (playerHavePermission(player)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            updateCheckerMessage(player); // Mensaje
                        }
                    }.runTask(plugin); // Ejecuta la tarea en el siguiente tick
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }
    }

    private boolean newVersionAvailable() {
        return UC.compareVersions(plugin.currentVersion, plugin.lastVersion) < 0;
    }

    private boolean playerHavePermission(Player player) {
        boolean hasAdminPermission = player.hasPermission(plugin.pluginName + ".admin");
        boolean hasUpdateCheckerPermission = player.hasPermission(plugin.pluginName + ".updatechecker");
        return hasAdminPermission || hasUpdateCheckerPermission;
    }

    private void updateCheckerMessage(Player player) {
        for (String message : languageManager.getAllMessage("messages.update-checker")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
