package com.Guayand0.inventory;

import com.Guayand0.MineBank;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.utils.gui.GuiUtils;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankConversation implements Listener {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    private final MessageUtils MU = new MessageUtils();

    public BankConversation(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void startConversation(Player player, String guiIdOpened, boolean isWithdraw) {

        player.closeInventory(); // Cerrar el inventario

        // Evitar múltiples conversaciones activas
        if (player.hasMetadata("conversation_active")) player.removeMetadata("conversation_active", plugin);
        player.setMetadata("conversation_active", new FixedMetadataValue(plugin, true));
        pendingInputs.put(player.getUniqueId(), new PendingInput(guiIdOpened, isWithdraw));
        sendPrompt(player, isWithdraw);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        final String input = event.getMessage() == null ? "" : event.getMessage().trim();

        plugin.getSchedulerCompat().runAtPlayer(player, () -> {
            PendingInput current = pendingInputs.get(player.getUniqueId());
            if (current == null) {
                return;
            }

            if (input.matches("\\d+")) {
                pendingInputs.remove(player.getUniqueId());
                player.removeMetadata("conversation_active", plugin);
                String command = current.isWithdraw ? "bank take " + input : "bank add " + input;
                player.performCommand(command);

                GuiUtils GUIU = new GuiUtils(plugin);
                GUIU.openGUI(player, current.guiIdOpened);
                return;
            }

            if (input.equalsIgnoreCase("exit")) {
                pendingInputs.remove(player.getUniqueId());
                player.removeMetadata("conversation_active", plugin);
                sendMessage.send(player, "bank.gui.transaction-canceled", null); // Mensaje

                GuiUtils GUIU = new GuiUtils(plugin);
                GUIU.openGUI(player, current.guiIdOpened);
                return;
            }

            sendMessage.send(player, "bank.gui.invalid-amount", null); // Mensaje
            sendPrompt(player, current.isWithdraw);
        });
    }

    private void sendPrompt(Player player, boolean isWithdraw) {
        String path = isWithdraw ? "bank.gui.amount-withdraw" : "bank.gui.amount-deposit";
        for (String msg : plugin.getLanguageManager().getAllMessage(path)) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, msg, plugin.placeholders));
        }
    }

    private static class PendingInput {
        private final String guiIdOpened;
        private final boolean isWithdraw;

        private PendingInput(String guiIdOpened, boolean isWithdraw) {
            this.guiIdOpened = guiIdOpened;
            this.isWithdraw = isWithdraw;
        }
    }
}
