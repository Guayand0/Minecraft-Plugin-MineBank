package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.inventory.MainGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerExit implements Listener {

    private final MineBank plugin;

    public OnPlayerExit(MineBank plugin) {
        this.plugin = plugin;
    }

    // Cerrarel inventario
    @EventHandler
    public void onPlayerExit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Eliminar el jugador de la lista
        MainGUI.openedPlayersGUI.remove(player.getUniqueId());
    }
}
