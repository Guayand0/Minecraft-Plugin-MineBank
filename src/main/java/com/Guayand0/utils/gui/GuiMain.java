package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.InventoryUtils;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;

public class GuiMain {

    private final MineBank plugin;

    private FileConfiguration languageInventoryManager;

    private final MessageUtils MU = new MessageUtils();
    private final InventoryUtils IU = new InventoryUtils();

    public GuiMain(MineBank plugin) {
        this.plugin = plugin;
    }

    /*public boolean guiExists(String guiId) {
        loadGuiConfig();
        ConfigurationSection section = languageInventoryManager.getConfigurationSection("gui");
        return section != null && section.contains(guiId);
    }

    public String getGuiTitle(String guiId) {
        loadGuiConfig();
        String path = "gui." + guiId + ".name";
        String name = languageInventoryManager.getString(path, guiId);
        return MU.getColoredReplacePluginPlaceholdersText(name, plugin.placeholders);
    }

    public int getGuiSize(String guiId) {
        loadGuiConfig();
        String path = "gui." + guiId + ".size";
        return languageInventoryManager.getInt(path, 6) * 9;
    }

    public void loadGuiConfig() {
        if (languageInventoryManager == null) {
            languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
        }
    }

    public void reloadGuiConfig() {
        languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
    }*/
    public boolean guiExists(String guiId) {
        loadGuiConfig(guiId);
        return languageInventoryManager.getConfigurationSection("gui." + guiId) != null;
    }

    public String getGuiTitle(String guiId) {
        loadGuiConfig(guiId);
        String path = "gui." + guiId + ".name";
        String name = languageInventoryManager.getString(path, guiId);
        return MU.getColoredReplacePluginPlaceholdersText(name, plugin.placeholders);
    }

    public int getGuiSize(String guiId) {
        loadGuiConfig(guiId);
        String path = "gui." + guiId + ".size";
        return languageInventoryManager.getInt(path, 6) * 9;
    }

    private void loadGuiConfig(String guiId) {
        languageInventoryManager = IU.getGuiConfig(plugin, guiId);
    }

    public void reloadGuiConfig() {
        languageInventoryManager = null;
    }

}
