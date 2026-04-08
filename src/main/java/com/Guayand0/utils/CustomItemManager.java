package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomItemManager {

    private final MineBank plugin;
    private final MessageUtils MU = new MessageUtils();
    public CustomItemManager(MineBank plugin) {
        this.plugin = plugin;
    }

    public void saveItem(String customItemId, ItemStack source) throws IOException {
        ensureItemDirectory();

        ItemStack stored = source.clone();
        stored.setAmount(1);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", stored);
        yaml.save(resolveItemFile(customItemId));
    }

    public ItemStack loadItem(String customItemId) {
        File file = resolveExistingItemFile(customItemId);
        if (file == null) {
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ItemStack item = yaml.getItemStack("item");
        return item != null ? item.clone() : null;
    }

    public ItemStack buildGuiItem(ConfigurationSection section, Map<String, String> placeholders) {
        String customItemId = section.getString("custom-item");
        if (customItemId == null || customItemId.trim().isEmpty()) {
            return null;
        }

        ItemStack item = loadItem(customItemId);
        if (item == null || item.getType() == Material.AIR) {
            return buildMissingItem(customItemId, placeholders);
        }

        item = item.clone();

        int amount = Math.max(1, section.getInt("amount", item.getAmount() <= 0 ? 1 : item.getAmount()));
        item.setAmount(Math.min(amount, item.getMaxStackSize()));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String name = section.getString("name");
        if (name != null) {
            meta.setDisplayName(MU.getColoredReplacePluginPlaceholdersText(name, placeholders));
        }

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            List<String> coloredLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                coloredLore.add(MU.getColoredReplacePluginPlaceholdersText(line, placeholders));
            }
            meta.setLore(coloredLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public File resolveItemFile(String customItemId) {
        return new File(resolveItemDirectory(), customItemId + ".yml");
    }

    public File resolveExistingItemFile(String customItemId) {
        File dir = resolveItemDirectory();
        File[] files = dir.listFiles((parent, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (stripExtension(file.getName()).equalsIgnoreCase(customItemId)) {
                return file;
            }
        }
        return null;
    }

    public List<String> getCustomItemIds() {
        File dir = resolveItemDirectory();
        File[] files = dir.listFiles((parent, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<String> ids = new ArrayList<>();
        for (File file : files) {
            ids.add(stripExtension(file.getName()));
        }
        Collections.sort(ids);
        return ids;
    }

    public String getCanonicalItemId(File file) {
        return stripExtension(file.getName());
    }

    public File resolveItemDirectory() {
        return new File(plugin.getDataFolder(), "item");
    }

    public void ensureItemDirectory() {
        File dir = resolveItemDirectory();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private ItemStack buildMissingItem(String customItemId, Map<String, String> placeholders) {
        ItemStack barrier = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = barrier.getItemMeta();
        if (meta == null) {
            return barrier;
        }

        Map<String, String> ph = placeholders != null ? placeholders : Collections.emptyMap();
        Map<String, String> merged = new java.util.HashMap<>(plugin.placeholders);
        merged.putAll(ph);
        merged.put("%customItemID%", customItemId);

        List<String> titleMessages = plugin.getLanguageManager().getAllMessage("bank.item.gui.custom-item-not-found-name");
        if (!titleMessages.isEmpty()) {
            meta.setDisplayName(MU.getColoredReplacePluginPlaceholdersText(titleMessages.get(0), merged));
        }

        List<String> loreMessages = plugin.getLanguageManager().getAllMessage("bank.item.gui.custom-item-not-found-lore");
        if (!loreMessages.isEmpty()) {
            List<String> lore = new ArrayList<>(loreMessages.size());
            for (String line : loreMessages) {
                lore.add(MU.getColoredReplacePluginPlaceholdersText(line, merged));
            }
            meta.setLore(lore);
        }

        barrier.setItemMeta(meta);
        return barrier;
    }
}
