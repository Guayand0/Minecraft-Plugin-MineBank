package com.Guayand0.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final String guiId;

    public GuiHolder(String guiId) {
        this.guiId = guiId;
    }

    public String getGuiId() {
        return guiId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}