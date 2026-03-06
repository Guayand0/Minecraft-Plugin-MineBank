package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.inventory.BankConversation;
import com.Guayand0.utils.GuiHolder;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OnInventoryClick implements Listener {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final InventoryUtils IU = new InventoryUtils();

    private FileConfiguration languageInventoryManager;
    private final BankConversation conversationManager;

    public OnInventoryClick(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.conversationManager = new BankConversation(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        String guiIdOpened = getOpenedGuiId(event);
        if (guiIdOpened == null) return;
        handleItemClick(event, player, guiIdOpened);
    }

    /* ------------------ Métodos auxiliares ------------------ */

    private String getOpenedGuiId(InventoryClickEvent event) {
        languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));

        InventoryHolder holder = event.getView().getTopInventory().getHolder();

        if (!(holder instanceof GuiHolder)) {
            return null;
        }

        GuiHolder guiHolder = (GuiHolder) holder;
        return guiHolder.getGuiId();
    }

    private void handleItemClick(InventoryClickEvent event, Player player, String guiIdOpened) {
        // Solo aceptar clicks en el inventario GUI (top inventory)
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (!isValidItem(event.getCurrentItem())) return;

        event.setCancelled(true);

        if (player.hasMetadata("bank_click_cooldown")) return;

        player.setMetadata("bank_click_cooldown", new FixedMetadataValue(plugin, true));
        processSlotClick(player, guiIdOpened, event.getSlot());

        Bukkit.getScheduler().runTaskLater(plugin, () -> player.removeMetadata("bank_click_cooldown", plugin), 5L);
    }

    private void processSlotClick(Player player, String guiIdOpened, int slot) {
        String slotPath = "gui." + guiIdOpened + ".position-slot";
        ConfigurationSection slots = languageInventoryManager.getConfigurationSection(slotPath);
        if (slots == null || !slots.contains(String.valueOf(slot))) return;

        ConfigurationSection itemData = slots.getConfigurationSection(String.valueOf(slot));
        if (itemData != null) executeItemCommands(player, guiIdOpened, itemData);
    }

    private void executeItemCommands(Player player, String guiIdOpened, ConfigurationSection itemData) {

        if (!hasPermission(player, itemData)) return;

        List<String> commands = getAllItemCommands(itemData, "command");
        if (commands.isEmpty()) return;

        long delay = 0L;

        for (String cmd : commands) {

            if (cmd == null || cmd.trim().isEmpty()) continue;

            String rawCommand = cmd.trim();

            // Detectar delay
            if (rawCommand.startsWith("<delay:")) {
                try {
                    String ticks = rawCommand.replace("<delay:", "")
                            .replace(">", "")
                            .trim();
                    delay += Long.parseLong(ticks);
                } catch (Exception ignored) {}
                continue;
            }

            boolean runAsConsole = consoleCommandPrefix(rawCommand);
            boolean runAsBankMessage = bankMessageCommandPrefix(rawCommand);

            rawCommand = cleanCommandPrefix(rawCommand);

            if (rawCommand.isEmpty()) continue;

            final String commandToRun = rawCommand;
            final boolean console = runAsConsole;
            final boolean bankMsg = runAsBankMessage;
            final long finalDelay = delay;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
                String finalCommand = replacePlaceholders(commandToRun, ph);

                if (processSpecialCommands(player, guiIdOpened, finalCommand)) return;

                executeCommand(player, finalCommand, console, bankMsg, ph);

            }, finalDelay);
        }
    }

// -------------------- Métodos auxiliares II --------------------

    private boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private boolean hasPermission(Player player, ConfigurationSection itemData) {
        List<String> permissions = getAllItemCommands(itemData, "permission");
        if (permissions != null && !permissions.isEmpty()) {
            for (String perm : permissions) {
                if (!player.hasPermission(perm)) {
                    sendMessage.send((CommandSender) player, "messages.no-perm", null); // Mensaje
                    return false;
                }
            }
        }
        return true;
    }

    private boolean consoleCommandPrefix(String command) {
        return command.startsWith("<console>");
    }

    private boolean bankMessageCommandPrefix(String command) {
        return command.startsWith("<bank-message>");
    }

    private String cleanCommandPrefix(String command) {
        if (command.startsWith("<console>")) {
            return command.replace("<console>", "").trim();
        } else if (command.startsWith("<player>")) {
            return command.replace("<player>", "").trim();
        } else if (command.startsWith("<bank-message>")) {
            return command.replace("<bank-message>", "").trim();
        }
        return command;
    }

    private String replacePlaceholders(String command, Map<String, String> ph) {
        String result = command;
        for (Map.Entry<String, String> entry : ph.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private boolean processSpecialCommands(Player player, String guiIdOpened, String command) {
        if (command.contains("<amount>")) {
            boolean isWithdraw = command.contains("bank take");
            conversationManager.startConversation(player, guiIdOpened, isWithdraw);
            return true; // No ejecutar el comando normal
        }
        return false;
    }

    private void executeCommand(Player player, String command, boolean asConsole, boolean asBankMessage, Map<String,String> ph) {
        if (asConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (asBankMessage) {
            sendMessage.sendRaw((CommandSender) player, command, ph);
        } else {
            player.performCommand(command);
        }
    }

    public List<String> getAllItemCommands(ConfigurationSection itemData, String path) {
        if (itemData.isList(path)) {
            return itemData.getStringList(path);
        }

        String command = itemData.getString(path, "");
        if (command.isEmpty()) {
            return new ArrayList<>(); // Lista vacía si no hay comando
        }

        return Arrays.asList(command);
    }
}
