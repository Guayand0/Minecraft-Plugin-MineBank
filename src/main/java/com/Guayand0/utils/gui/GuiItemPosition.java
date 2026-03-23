package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class GuiItemPosition {

    private final MineBank plugin;

    private final GuiItemMeta GUIIM = new GuiItemMeta();
    private final InventoryUtils IU = new InventoryUtils();

    public GuiItemPosition(MineBank plugin) {
        this.plugin = plugin;
    }

    public void setDefaultItems(Player player, Inventory inventory, String guiId) {

        String defaultPath = "gui." + guiId + ".position-slot.default";
        ItemStack item = buildItem(player, defaultPath);
        if (item == null) return;

        int maxSlots = resolveGuiSize(guiId, inventory);

        if (!fillListedEmptySlots(inventory, item, guiId, defaultPath, maxSlots)) {
            fillEmptySlots(inventory, item);
        }
    }

    public void setSlottedItems(Player player, Inventory inventory, String path, String slot) {

        Integer slotNumber = parseSlot(slot, inventory);
        if (slotNumber == null) return;

        ItemStack item = buildItem(player, path + "." + slot);
        if (item == null) return;

        inventory.setItem(slotNumber, item);
    }

    private ItemStack buildItem(Player player, String route) {
        return createItem(route, plugin.buildPlayerPlaceholders(player.getUniqueId()));
    }

    public ItemStack createItem(String route, Map<String,String> ph) {

        String guiId = extractGuiId(route);
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, guiId);
        ConfigurationSection section = languageInventoryManager.getConfigurationSection(route);
        if (section == null) return null;

        Material material = GUIIM.getMaterial(section);
        if (material == null) return null;

        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        GUIIM.applyName(section, meta, ph);
        GUIIM.applyLore(section, meta, ph);
        GUIIM.applyEnchants(section, meta);

        item.setItemMeta(meta);

        GuiHeadTexture GUIHT = new GuiHeadTexture(plugin);
        GUIHT.addHeadTexture(material, section, item);

        return item;
    }

    private Integer parseSlot(String slot, Inventory inventory) {

        try {
            int slotNumber = Integer.parseInt(slot);

            if (slotNumber < 0 || slotNumber >= inventory.getSize()) {
                return null;
            }

            return slotNumber;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractGuiId(String route) {
        if (route == null) return "main";
        String[] parts = route.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "main";
    }

    private void fillEmptySlots(Inventory inventory, ItemStack item) {

        for (int i = 0; i < inventory.getSize(); i++) {

            ItemStack current = inventory.getItem(i);

            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(i, item);
            }
        }
    }

    private int resolveGuiSize(String guiId, Inventory inventory) {
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, guiId);
        int rows = languageInventoryManager.getInt("gui." + guiId + ".size", 0);
        int size = rows * 9;
        if (size <= 0) {
            return inventory.getSize();
        }
        return size;
    }

    private boolean fillListedEmptySlots(Inventory inventory, ItemStack item, String guiId, String defaultPath, int maxSlots) {
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, guiId);
        ConfigurationSection section = languageInventoryManager.getConfigurationSection(defaultPath);
        if (section == null) return false;

        String listRaw = section.getString("list");
        if (listRaw == null || listRaw.trim().isEmpty()) return false;

        String[] parts = listRaw.split(",");
        boolean anyValid = false;

        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) continue;

            int slotNumber;
            try {
                slotNumber = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                continue;
            }

            if (slotNumber < 0 || slotNumber >= maxSlots) {
                continue;
            }
            if (slotNumber >= inventory.getSize()) {
                continue;
            }

            ItemStack current = inventory.getItem(slotNumber);
            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(slotNumber, item);
            }

            anyValid = true;
        }

        return anyValid;
    }
}
