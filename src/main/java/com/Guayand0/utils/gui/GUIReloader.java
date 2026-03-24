package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class GUIReloader {

    private final MineBank plugin;

    public GUIReloader(MineBank plugin) {
        this.plugin = plugin;
    }

    public boolean hasMineBankGUIOpen(Player player) {

        if (player == null) return false;

        InventoryView view = player.getOpenInventory();
        if (view == null) return false;

        return view.getTopInventory().getHolder() instanceof GuiHolder;
    }

    public void reloadPlayerGUI(Player player) {

        InventoryView view = player.getOpenInventory();
        GuiHolder holder = (GuiHolder) view.getTopInventory().getHolder();

        String guiId = holder.getGuiId();

        Inventory newInv = buildInventory(player, guiId);

        player.closeInventory();

        plugin.getSchedulerCompat().runAtPlayerLater(player, () -> {
            player.openInventory(newInv);
            GuiUtils.openedPlayersGUI.add(player.getUniqueId());
        }, 1L);
    }

    private Inventory buildInventory(Player player, String guiId) {
        GuiUtils GUIU = new GuiUtils(plugin);
        GuiItemPosition GUIIP = new GuiItemPosition(plugin);
        Inventory inventory = GUIU.createGUI(guiId);

        GuiItemUpdater GUIIU = new GuiItemUpdater(plugin);
        ConfigurationSection slots = GUIIU.getGuiSlots(guiId);

        if (slots != null) {

            for (String key : slots.getKeys(false)) {
                if (!key.equalsIgnoreCase("default")) {
                    GUIIP.setSlottedItems(player, inventory, "gui." + guiId + ".position-slot", key);
                }
            }

            GUIIP.setDefaultItems(player, inventory, guiId);
        }

        return inventory;
    }
}
