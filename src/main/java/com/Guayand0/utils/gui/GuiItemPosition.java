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

        ItemStack item = buildItem(player, "gui." + guiId + ".position-slot.default");
        if (item == null) return;

        fillEmptySlots(inventory, item);
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

        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
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

    private void fillEmptySlots(Inventory inventory, ItemStack item) {

        for (int i = 0; i < inventory.getSize(); i++) {

            ItemStack current = inventory.getItem(i);

            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(i, item);
            }
        }
    }
}
