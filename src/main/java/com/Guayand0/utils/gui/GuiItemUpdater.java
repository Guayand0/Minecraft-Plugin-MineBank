package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.InventoryUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiItemUpdater {

    private final MineBank plugin;

    private final InventoryUtils IU = new InventoryUtils();

    public GuiItemUpdater(MineBank plugin) {
        this.plugin = plugin;
    }

    public Inventory getPlayerTopInventory(Player player) {
        if (player == null) return null;
        if (player.getOpenInventory() == null) return null;
        return player.getOpenInventory().getTopInventory();
    }

    public ConfigurationSection getGuiSlots(String guiId) {
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, guiId);
        return languageInventoryManager.getConfigurationSection("gui." + guiId + ".position-slot");
    }

    public void updateSlottedItems(Player player, Inventory inv, String guiId, ConfigurationSection slots) {

        for (String key : slots.getKeys(false)) {

            if (key.equalsIgnoreCase("default")) continue;

            Integer slot = parseSlot(key, inv);
            if (slot == null) continue;

            GuiItemPosition GUIIP = new GuiItemPosition(plugin);
            ItemStack item = GUIIP.createItem("gui." + guiId + ".position-slot." + key, plugin.buildPlayerPlaceholders(player.getUniqueId()));

            if (item != null) {
                inv.setItem(slot, item);
            }
        }
    }

    private Integer parseSlot(String key, Inventory inv) {
        try {
            int slot = Integer.parseInt(key);
            if (slot < 0 || slot >= inv.getSize()) return null;
            return slot;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
