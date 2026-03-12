package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.utils.gui.GuiUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class OnInventoryClose implements Listener {

    private final MineBank plugin;

    public OnInventoryClose(MineBank plugin) {
        this.plugin = plugin;
    }

    // Cerrar el inventario
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        // Eliminar el jugador de la lista
        GuiUtils.openedPlayersGUI.remove(player.getUniqueId());
    }
}
