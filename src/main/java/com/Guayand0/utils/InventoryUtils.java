package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.events.BankInventoryEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;

public class InventoryUtils {

    /*public void openInventory(Player player, BankInventoryEvent bankInventoryEvent) {
        Inventory inventory = bankInventoryEvent.createInventory(player);
        player.openInventory(inventory);
    }*/

    public String getGuiLangFile(MineBank plugin) {
        String selectedLanguage = plugin.getConfig().getString("config.gui-language", "en");
        return selectedLanguage + ".yml";
    }

    public FileConfiguration getGuiConfig(MineBank plugin, String filePath) {
        File configFile = new File(plugin.getDataFolder(), filePath);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(filePath, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }
}
