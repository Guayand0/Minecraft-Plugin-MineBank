package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.utils.CustomItemManager;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ItemSubCommand implements CommandExecutor {

    private static final long CONFIRM_EXPIRE_TICKS = 200L;

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final CustomItemManager customItemManager;

    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

    public ItemSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.customItemManager = new CustomItemManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage.sendRaw(sender, plugin.prefix + " &cThis command can only be used by players", null);
            return true;
        }

        Player player = (Player) sender;
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        if (!player.hasPermission(plugin.pluginName + ".admin")) {
            sendMessage.send(player, "messages.no-perm", ph);
            return true;
        }

        if (args.length < 1 || !"item".equalsIgnoreCase(args[0])) {
            sendUsage(player);
            return true;
        }

        try {
            if (args.length < 2) {
                sendUsage(player);
                return true;
            }

            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "list":
                    return handleList(player, args);

                case "save":
                    return handleSave(player, args);

                case "get":
                    return handleGet(player, args);

                case "rename":
                    return handleRename(player, args);

                case "replace":
                    return handleReplace(player, args);

                case "delete":
                    return handleDelete(player, args);

                default:
                    sendUsage(player);
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
            sendMessage.send(player, "messages.processing-command-error", ph);
            return true;
        }
    }

    private boolean handleSave(Player player, String[] args) throws IOException {
        if (args.length != 3) {
            sendUsage(player);
            return true;
        }

        String customItemId = args[2];
        if (!isValidCustomItemId(customItemId)) {
            sendItemMessage(player, "bank.item.invalid-id", customItemId, null);
            return true;
        }

        File file = customItemManager.resolveItemFile(customItemId);
        if (file.exists()) {
            sendItemMessage(player, "bank.item.already-exists", customItemId, null);
            return true;
        }

        ItemStack handItem = getHeldItem(player);
        if (isEmpty(handItem)) {
            sendItemMessage(player, "bank.item.hand-empty", null, null);
            return true;
        }

        customItemManager.saveItem(customItemId, handItem);
        sendItemMessage(player, "bank.item.saved", customItemId, null);
        return true;
    }

    private boolean handleList(Player player, String[] args) {
        if (args.length != 2) {
            sendUsage(player);
            return true;
        }

        java.util.List<String> ids = customItemManager.getCustomItemIds();
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        ph.put("%amount%", String.valueOf(ids.size()));

        if (ids.isEmpty()) {
            sendMessage.send(player, "bank.item.list-empty", ph);
            return true;
        }

        sendMessage.send(player, "bank.item.list-header", ph);
        for (String id : ids) {
            ph.put("%customItemID%", id);
            sendMessage.send(player, "bank.item.list-entry", ph);
        }
        return true;
    }

    private boolean handleGet(Player player, String[] args) {
        if (args.length != 3 && args.length != 4) {
            sendUsage(player);
            return true;
        }

        String customItemId = args[2];
        File file = customItemManager.resolveExistingItemFile(customItemId);
        if (file == null) {
            sendItemMessage(player, "bank.item.not-found", customItemId, null);
            return true;
        }

        int amount = 1;
        if (args.length == 4) {
            amount = parsePositiveInt(args[3]);
            if (amount <= 0) {
                sendMessage.send(player, "bank.not-positive-integer", plugin.buildPlayerPlaceholders(player.getUniqueId()));
                return true;
            }
        }

        ItemStack item = customItemManager.loadItem(customItemManager.getCanonicalItemId(file));
        if (isEmpty(item)) {
            sendItemMessage(player, "bank.item.load-failed", customItemId, null);
            return true;
        }

        item.setAmount(Math.min(amount, item.getMaxStackSize()));
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        ph.put("%customItemID%", customItemManager.getCanonicalItemId(file));
        ph.put("%amount%", String.valueOf(item.getAmount()));
        sendMessage.send(player, "bank.item.received", ph);
        return true;
    }

    private boolean handleRename(Player player, String[] args) {
        if (args.length == 3 && isDecisionArg(args[2])) {
            return handlePendingDecision(player, ActionType.RENAME, args);
        }

        if (args.length != 4) {
            sendUsage(player);
            return true;
        }

        String currentId = args[2];
        String newId = args[3];

        File currentFile = customItemManager.resolveExistingItemFile(currentId);
        if (currentFile == null) {
            sendItemMessage(player, "bank.item.not-found", currentId, null);
            return true;
        }

        if (!isValidCustomItemId(newId)) {
            sendItemMessage(player, "bank.item.invalid-new-id", currentId, newId);
            return true;
        }

        if (customItemManager.resolveItemFile(newId).exists()) {
            sendItemMessage(player, "bank.item.target-exists", currentId, newId);
            return true;
        }

        PendingAction action = PendingAction.rename(customItemManager.getCanonicalItemId(currentFile), newId);
        pendingActions.put(player.getUniqueId(), action);
        schedulePendingExpire(player, action);

        sendItemMessage(player, "bank.item.rename-confirm-needed", action.sourceId, action.targetId);
        return true;
    }

    private boolean handleReplace(Player player, String[] args) {
        if (args.length == 3 && isDecisionArg(args[2])) {
            return handlePendingDecision(player, ActionType.REPLACE, args);
        }

        if (args.length != 3) {
            sendUsage(player);
            return true;
        }

        String customItemId = args[2];
        File file = customItemManager.resolveExistingItemFile(customItemId);
        if (file == null) {
            sendItemMessage(player, "bank.item.not-found", customItemId, null);
            return true;
        }

        ItemStack handItem = getHeldItem(player);
        if (isEmpty(handItem)) {
            sendItemMessage(player, "bank.item.hand-empty", null, null);
            return true;
        }

        PendingAction action = PendingAction.replace(customItemManager.getCanonicalItemId(file), handItem.clone());
        pendingActions.put(player.getUniqueId(), action);
        schedulePendingExpire(player, action);

        sendItemMessage(player, "bank.item.replace-confirm-needed", action.sourceId, null);
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length == 3 && isDecisionArg(args[2])) {
            return handlePendingDecision(player, ActionType.DELETE, args);
        }

        if (args.length != 3) {
            sendUsage(player);
            return true;
        }

        String customItemId = args[2];
        File file = customItemManager.resolveExistingItemFile(customItemId);
        if (file == null) {
            sendItemMessage(player, "bank.item.not-found", customItemId, null);
            return true;
        }

        PendingAction action = PendingAction.delete(customItemManager.getCanonicalItemId(file));
        pendingActions.put(player.getUniqueId(), action);
        schedulePendingExpire(player, action);

        sendItemMessage(player, "bank.item.delete-confirm-needed", action.sourceId, null);
        return true;
    }

    private boolean handlePendingDecision(Player player, ActionType expectedType, String[] args) {
        String decision;
        if (args.length == 3) {
            decision = args[2];
        } else if (args.length == 4) {
            decision = args[3];
        } else {
            sendUsage(player);
            return true;
        }

        if (!"confirm".equalsIgnoreCase(decision) && !"cancel".equalsIgnoreCase(decision)) {
            return false;
        }

        PendingAction pending = pendingActions.get(player.getUniqueId());
        if (pending == null || pending.type != expectedType) {
            sendItemMessage(player, "bank.item.no-pending", null, null);
            return true;
        }

        if ("cancel".equalsIgnoreCase(decision)) {
            pendingActions.remove(player.getUniqueId());
            sendItemMessage(player, "bank.item.cancelled", null, null);
            return true;
        }

        try {
            executePendingAction(player, pending);
            pendingActions.remove(player.getUniqueId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void executePendingAction(Player player, PendingAction action) throws IOException {
        switch (action.type) {
            case RENAME:
                File sourceFile = customItemManager.resolveExistingItemFile(action.sourceId);
                if (sourceFile == null) {
                    sendItemMessage(player, "bank.item.not-found", action.sourceId, null);
                    return;
                }
                File targetFile = customItemManager.resolveItemFile(action.targetId);
                if (targetFile.exists()) {
                    sendItemMessage(player, "bank.item.target-exists", action.sourceId, action.targetId);
                    return;
                }
                customItemManager.ensureItemDirectory();
                if (!sourceFile.renameTo(targetFile)) {
                    throw new IOException("Could not rename item file");
                }
                sendItemMessage(player, "bank.item.renamed", action.sourceId, action.targetId);
                return;

            case REPLACE:
                File replaceFile = customItemManager.resolveExistingItemFile(action.sourceId);
                if (replaceFile == null) {
                    sendItemMessage(player, "bank.item.not-found", action.sourceId, null);
                    return;
                }
                if (isEmpty(action.itemSnapshot)) {
                    sendItemMessage(player, "bank.item.pending-item-invalid", action.sourceId, null);
                    return;
                }
                customItemManager.saveItem(action.sourceId, action.itemSnapshot);
                sendItemMessage(player, "bank.item.replaced", action.sourceId, null);
                return;

            case DELETE:
                File deleteFile = customItemManager.resolveExistingItemFile(action.sourceId);
                if (deleteFile == null) {
                    sendItemMessage(player, "bank.item.not-found", action.sourceId, null);
                    return;
                }
                if (!deleteFile.delete()) {
                    throw new IOException("Could not delete item file");
                }
                sendItemMessage(player, "bank.item.deleted", action.sourceId, null);
        }
    }

    private void sendUsage(Player player) {
        sendMessage.send(player, "bank.item.usage-admin", plugin.buildPlayerPlaceholders(player.getUniqueId()));
    }

    private boolean isValidCustomItemId(String customItemId) {
        return customItemId != null && customItemId.matches("^[A-Za-z0-9_-]+$");
    }

    private ItemStack getHeldItem(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private int parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isDecisionArg(String raw) {
        return "confirm".equalsIgnoreCase(raw) || "cancel".equalsIgnoreCase(raw);
    }

    private void sendItemMessage(Player player, String path, String customItemId, String newCustomItemId) {
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        if (customItemId != null) {
            ph.put("%customItemID%", customItemId);
        }
        if (newCustomItemId != null) {
            ph.put("%newCustomItemID%", newCustomItemId);
        }
        sendMessage.send(player, path, ph);
    }

    private void schedulePendingExpire(Player player, PendingAction action) {
        UUID playerId = player.getUniqueId();
        plugin.getSchedulerCompat().runAtPlayerLater(player, () -> {
            PendingAction current = pendingActions.get(playerId);
            if (current == action) {
                pendingActions.remove(playerId);
                Player online = Bukkit.getPlayer(playerId);
                if (online != null && online.isOnline()) {
                    sendItemMessage(online, "bank.item.confirm-expired", action.sourceId, action.targetId);
                }
            }
        }, CONFIRM_EXPIRE_TICKS);
    }

    private enum ActionType {
        RENAME,
        REPLACE,
        DELETE
    }

    private static class PendingAction {
        private final ActionType type;
        private final String sourceId;
        private final String targetId;
        private final ItemStack itemSnapshot;

        private PendingAction(ActionType type, String sourceId, String targetId, ItemStack itemSnapshot) {
            this.type = type;
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.itemSnapshot = itemSnapshot;
        }

        private static PendingAction rename(String sourceId, String targetId) {
            return new PendingAction(ActionType.RENAME, sourceId, targetId, null);
        }

        private static PendingAction replace(String sourceId, ItemStack itemSnapshot) {
            ItemStack stored = itemSnapshot != null ? itemSnapshot.clone() : null;
            if (stored != null) {
                stored.setAmount(1);
            }
            return new PendingAction(ActionType.REPLACE, sourceId, null, stored);
        }

        private static PendingAction delete(String sourceId) {
            return new PendingAction(ActionType.DELETE, sourceId, null, null);
        }
    }
}
