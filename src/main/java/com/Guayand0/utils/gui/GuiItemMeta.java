package com.Guayand0.utils.gui;

import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiItemMeta {

    private final MessageUtils MU = new MessageUtils();

    private static final Map<String, Enchantment> ENCHANT_CACHE = new HashMap<>();


    public Material getMaterial(ConfigurationSection section) {

        String materialName = section.getString("item");
        if (materialName == null) return null;

        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void applyName(ConfigurationSection section, ItemMeta meta, Map<String,String> ph) {

        String name = section.getString("name");
        if (name == null) return;

        meta.setDisplayName(MU.getColoredReplacePluginPlaceholdersText(name, ph));
    }

    public void applyLore(ConfigurationSection section, ItemMeta meta, Map<String,String> ph) {

        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) return;

        List<String> finalLore = new ArrayList<>(lore.size());

        for (String line : lore) {
            finalLore.add(MU.getColoredReplacePluginPlaceholdersText(line, ph));
        }

        meta.setLore(finalLore);
    }

    public void applyEnchants(ConfigurationSection section, ItemMeta meta) {

        List<String> enchants = section.getStringList("enchant");
        if (enchants.isEmpty()) return;

        for (String raw : enchants) {

            String[] parts = raw.split(":", 2);
            if (parts.length != 2) continue;

            int level;
            try {
                level = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            if (level <= 0) continue;

            Enchantment enchant = resolveEnchantment(parts[0].trim());
            if (enchant == null) continue;

            meta.addEnchant(enchant, level, true);
        }

        if (section.getBoolean("hide-enchant", false)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private Enchantment resolveEnchantment(String rawName) {

        if (rawName == null) return null;

        String normalized = rawName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;

        // ⚡ buscar en cache
        Enchantment cached = ENCHANT_CACHE.get(normalized);
        if (cached != null) return cached;

        Enchantment enchant;

        // intentar por nombre Bukkit
        enchant = Enchantment.getByName(normalized.toUpperCase(Locale.ROOT));

        // intentar por key namespaced
        if (enchant == null) {
            enchant = Enchantment.getByKey(NamespacedKey.minecraft(normalized));
        }

        // guardar resultado (aunque sea null evitamos repetir búsqueda)
        ENCHANT_CACHE.put(normalized, enchant);

        return enchant;
    }
}
