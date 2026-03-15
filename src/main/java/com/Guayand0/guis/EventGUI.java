package com.Guayand0.guis;

import com.Guayand0.MineBank;
import com.Guayand0.managers.EventManager;
import com.Guayand0.utils.gui.GuiUtils;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EventGUI {

    private static final int LAST_CONTENT_SLOT = 44;

    private final MineBank plugin;
    private final MessageUtils MU = new MessageUtils();

    public EventGUI(MineBank plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiUtils GUIU = new GuiUtils(plugin);
        GUIU.openGUI(player, "events");
        renderEvents(player);
    }

    public void update(Player player) {
        GuiUtils GUIU = new GuiUtils(plugin);
        GUIU.updateGUI(player, "events");
        renderEvents(player);
    }

    private void renderEvents(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        List<EventManager.EventInfo> events = plugin.getEventManager().getActiveEvents();

        int slot = 0;
        for (EventManager.EventInfo event : events) {
            if (slot > LAST_CONTENT_SLOT) break;
            top.setItem(slot, createEventItem(event));
            slot++;
        }
    }

    private ItemStack createEventItem(EventManager.EventInfo event) {
        boolean isProfit = event.getType() == EventManager.EventType.PROFIT;
        Material material = isProfit ? Material.EMERALD : Material.REDSTONE;

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String multiplier = formatMultiplier(event.getMultiplier());
        String remaining = formatDuration(event.getRemainingMillis());

        String titleKey = isProfit ? "bank.events.gui-items.profit-title" : "bank.events.gui-items.tax-title";
        String title = getMessage(titleKey, (isProfit ? "Profit x" : "Tax x") + multiplier);
        String loreLine = getMessage("bank.events.gui-items.remaining", "Remaining: " + remaining);

        Map<String, String> ph = new HashMap<>();
        ph.put("%multiplier%", multiplier);
        ph.put("%remaining%", remaining);
        title = MU.replacePlaceholdersText(title, ph);
        loreLine = MU.replacePlaceholdersText(loreLine, ph);

        meta.setDisplayName(MU.getColoredText(title));
        List<String> lore = new ArrayList<>();
        lore.add(MU.getColoredText(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getMessage(String path, String fallback) {
        List<String> list = plugin.getLanguageManager().getAllMessage(path);
        if (list == null || list.isEmpty()) return fallback;
        return list.get(0);
    }

    private String formatMultiplier(double value) {
        DecimalFormat df = new DecimalFormat("0.########");
        return df.format(value);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long weeks = totalSeconds / (7L * 24L * 3600L);
        totalSeconds %= (7L * 24L * 3600L);
        long days = totalSeconds / (24L * 3600L);
        totalSeconds %= (24L * 3600L);
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        List<String> parts = new ArrayList<>();
        if (weeks > 0) parts.add(weeks + "w");
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0) parts.add(minutes + "m");
        parts.add(seconds + "s");

        return String.join(" ", parts);
    }
}
