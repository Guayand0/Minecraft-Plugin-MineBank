package com.Guayand0.tasks;

import com.Guayand0.MineBank;
import com.Guayand0.inventory.MainGUI;
import com.Guayand0.utils.GuiHolder;
import com.Guayand0.zlib.GetValues;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateItemsGUI extends BukkitRunnable {

    private final MineBank plugin;
    private final MainGUI mainGUI;

    private final GetValues GV = new GetValues();

    public UpdateItemsGUI(MineBank plugin) {
        this.plugin = plugin;
        this.mainGUI = plugin.getMainGUI();
    }

    @Override
    public void run() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!MainGUI.openedPlayersGUI.contains(player.getUniqueId())) continue;

            InventoryView view = player.getOpenInventory();
            Inventory top = view.getTopInventory();
            if (!(top.getHolder() instanceof GuiHolder)) continue;

            GuiHolder holder = (GuiHolder) top.getHolder();
            String guiId = holder.getGuiId();

            mainGUI.updateInventory(player, guiId); // Usa directamente el guiId del holder
        }
    }

    // Método para iniciar el task
    public void start() {
        // 40 ticks = 2 segundos
        int intervalo = GV.getInt(plugin, "gui.update-time", 40);
        this.runTaskTimer(plugin, 0L, intervalo);
    }
}
