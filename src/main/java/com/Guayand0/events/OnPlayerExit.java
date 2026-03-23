package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.utils.gui.GuiUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class OnPlayerExit implements Listener {

    private final MineBank plugin;

    public OnPlayerExit(MineBank plugin) {
        this.plugin = plugin;
    }

    // Cerrar el inventario
    @EventHandler
    public void onExit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Eliminar el jugador de la lista
        GuiUtils.openedPlayersGUI.remove(player.getUniqueId());
        long disconnectedAt = plugin.getWebTokenStore().markDisconnected(player.getUniqueId());
        long delayMinutes = Math.max(0L, plugin.getConfig().getLong("web.token.invalidate-after-disconnect-minutes", 5));
        if (disconnectedAt <= 0L) {
            return;
        }
        if (delayMinutes <= 0L) {
            plugin.getWebTokenStore().invalidateIfDisconnected(player.getUniqueId(), disconnectedAt);
            return;
        }
        long delayTicks = delayMinutes * 60L * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getWebTokenStore().invalidateIfDisconnected(player.getUniqueId(), disconnectedAt);
            }
        }.runTaskLater(plugin, delayTicks);
    }
}
