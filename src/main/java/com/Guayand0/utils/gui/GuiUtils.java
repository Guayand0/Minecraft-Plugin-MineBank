package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class GuiUtils {

    private final MineBank plugin;

    public static Set<UUID> openedPlayersGUI = new HashSet<>();

    private final InventoryUtils IU = new InventoryUtils();

    public GuiUtils(MineBank plugin) {
        this.plugin = plugin;
    }

    public Inventory createGUI(String guiId) {
        GuiMain GUIM = new GuiMain(plugin);
        return Bukkit.createInventory(new GuiHolder(guiId), GUIM.getGuiSize(guiId), GUIM.getGuiTitle(guiId));
    }

    public void openGUI(Player player, String guiId) {
        GuiItemPosition GUIIP = new GuiItemPosition(plugin);
        Inventory inventory = createGUI(guiId); // Cada GUI tiene su inventario local

        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, guiId);
        ConfigurationSection slots = languageInventoryManager.getConfigurationSection("gui." + guiId + ".position-slot");
        if (slots != null) {

            // 1. Cargar slots numéricos
            for (String key : slots.getKeys(false)) {
                if (!key.equalsIgnoreCase("default")) {
                    GUIIP.setSlottedItems(player, inventory, "gui." + guiId + ".position-slot", key);
                }
            }

            // 2. Cargar slots default
            GUIIP.setDefaultItems(player, inventory, guiId);
        }

        // 3. Abrir GUI
        player.openInventory(inventory);
        openedPlayersGUI.add(player.getUniqueId());
    }

    public void updateGUI(Player player, String guiId) {
        GuiItemUpdater GUIIU = new GuiItemUpdater(plugin);
        GuiItemPosition GUIIP = new GuiItemPosition(plugin);
        Inventory inv = GUIIU.getPlayerTopInventory(player);
        if (inv == null) return;

        ConfigurationSection slots = GUIIU.getGuiSlots(guiId);
        if (slots == null) return;

        GUIIU.updateSlottedItems(player, inv, guiId, slots);
        GUIIP.setDefaultItems(player, inv, guiId);
    }

    public void reloadGUI() {
        GUIReloader GUIR = new GUIReloader(plugin);
        openedPlayersGUI.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        for (UUID uuid : openedPlayersGUI) {

            Player player = Bukkit.getPlayer(uuid);
            if (!GUIR.hasMineBankGUIOpen(player)) continue;

            GUIR.reloadPlayerGUI(player);
        }
    }

    /*Map<String, String> titlePlaceholders = null;
            if ("transactions".equalsIgnoreCase(guiId)) {
        TransactionGUI transactionGUI = plugin.getTransactionGUI();
        if (transactionGUI != null) {
            String viewedTargetName = transactionGUI.getViewedTargetName(player);
            if (viewedTargetName != null && !viewedTargetName.isEmpty()) {
                titlePlaceholders = new HashMap<>();
                titlePlaceholders.put("%playerName%", viewedTargetName);
            }
        }
    }*/
}