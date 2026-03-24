package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CheckForUpdates implements Listener {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final UpdateChecker UC = new UpdateChecker();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public CheckForUpdates(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @EventHandler
    public void checkUpdate(PlayerJoinEvent event) {
        try {

            Player player = event.getPlayer();
            if (!plugin.updateCheckerWork) plugin.comprobarActualizaciones();
            boolean updateCheckerAllowed = GV.getBoolean(plugin, "config.update-checker", true);

            // Si el mensaje esta activado y hay nueva version
            if (updateCheckerAllowed && newVersionAvailable()) {

                // Si el jugador tiene permisos
                if (playerHavePermission(player)) {
                    plugin.getSchedulerCompat().runAtPlayer(player, () -> sendMessage.send(player, "config.update-checker", null)); // Mensaje
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
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

}
