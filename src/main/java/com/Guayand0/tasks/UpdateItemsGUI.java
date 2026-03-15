package com.Guayand0.tasks;

import com.Guayand0.MineBank;
import com.Guayand0.utils.gui.GuiHolder;
import com.Guayand0.utils.gui.GuiUtils;
import com.Guayand0.zlib.GetValues;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateItemsGUI extends BukkitRunnable {

    private final MineBank plugin;

    private final GetValues GV = new GetValues();
    private final int nonEventIntervalTicks;
    private int nonEventElapsedTicks = 0;

    public UpdateItemsGUI(MineBank plugin) {
        this.plugin = plugin;
        int configured = GV.getInt(plugin, "gui.update-time", 40);
        this.nonEventIntervalTicks = Math.max(1, configured);
    }

    @Override
    public void run() {

        boolean updateNonEventGui = false;
        nonEventElapsedTicks += 20;
        if (nonEventElapsedTicks >= nonEventIntervalTicks) {
            updateNonEventGui = true;
            nonEventElapsedTicks = 0;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!GuiUtils.openedPlayersGUI.contains(player.getUniqueId())) continue;

            InventoryView view = player.getOpenInventory();
            Inventory top = view.getTopInventory();
            if (!(top.getHolder() instanceof GuiHolder)) continue;

            GuiHolder holder = (GuiHolder) top.getHolder();
            String guiId = holder.getGuiId();

            boolean isEventsGui = "events".equalsIgnoreCase(guiId);
            if (!isEventsGui && !updateNonEventGui) continue;
            if (isEventsGui) {
                new com.Guayand0.guis.EventGUI(plugin).update(player);
                continue;
            }
            if (!updateNonEventGui) continue;

            GuiUtils GUIU = new GuiUtils(plugin);
            GUIU.updateGUI(player, guiId); // Usa directamente el guiId del holder
        }
    }

    // Método para iniciar el task
    public void start() {
        // 20 ticks = 1 segundo (events GUI siempre cada segundo)
        this.runTaskTimer(plugin, 0L, 20L);
    }
}
