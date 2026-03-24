package com.Guayand0.guis;

import com.Guayand0.MineBank;
import com.Guayand0.data.transactions.TransactionData;
import com.Guayand0.data.transactions.TransactionService;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.gui.GuiUtils;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionGUI {

    private static final int PAGE_SIZE = 45;
    private static final int FIRST_CONTENT_SLOT = 0;
    private static final int LAST_CONTENT_SLOT = 44;

    private final MineBank plugin;
    private final TransactionService transactionService;

    private final MessageUtils MU = new MessageUtils();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    private final Map<UUID, Integer> currentPages = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentFilters = new ConcurrentHashMap<>();

    public TransactionGUI(MineBank plugin, TransactionService transactionService) {
        this.plugin = plugin;
        this.transactionService = transactionService;
    }

    public void open(Player player, int page) {
        open(player, page, null);
    }

    public void open(Player player, int page, String typeFilter) {

        int safePage = Math.max(1, page);
        currentPages.put(player.getUniqueId(), safePage);
        if (typeFilter == null || typeFilter.trim().isEmpty()) {
            currentFilters.remove(player.getUniqueId());
        } else {
            currentFilters.put(player.getUniqueId(), typeFilter.trim().toLowerCase());
        }

        GuiUtils GUIU = new GuiUtils(plugin);
        GUIU.openGUI(player, "transactions");
        renderTransactions(player, safePage);
    }

    public void openNextPage(Player player) {
        int currentPage = getCurrentPage(player);
        int nextPage = currentPage + 1;
        String filter = getCurrentFilter(player);
        List<TransactionData> nextPageRows = filter == null
                ? transactionService.getPlayerTransactions(player.getUniqueId(), nextPage, PAGE_SIZE)
                : transactionService.getPlayerTransactionsFiltered(player.getUniqueId(), nextPage, PAGE_SIZE, filter);

        if (nextPageRows.isEmpty()) {
            return;
        }

        open(player, nextPage, filter);
    }

    public void openPreviousPage(Player player) {
        int currentPage = getCurrentPage(player);
        int previousPage = Math.max(1, currentPage - 1);
        open(player, previousPage, getCurrentFilter(player));
    }

    public int getCurrentPage(Player player) {
        return currentPages.getOrDefault(player.getUniqueId(), 1);
    }

    public String getCurrentFilter(Player player) {
        return currentFilters.get(player.getUniqueId());
    }

    private void renderTransactions(Player player, int page) {

        Inventory top = player.getOpenInventory().getTopInventory();

        String filter = getCurrentFilter(player);
        List<TransactionData> rows = filter == null
                ? transactionService.getPlayerTransactions(player.getUniqueId(), page, PAGE_SIZE)
                : transactionService.getPlayerTransactionsFiltered(player.getUniqueId(), page, PAGE_SIZE, filter);
        if (rows.isEmpty()) {
            return;
        }

        int slot = FIRST_CONTENT_SLOT;
        for (TransactionData entry : rows) {
            if (slot > LAST_CONTENT_SLOT) {
                break;
            }
            top.setItem(slot, createTransactionItem(entry));
            slot++;
        }
    }

    private ItemStack createTransactionItem(TransactionData transaction) {
        boolean isDeposit = "deposit".equalsIgnoreCase(transaction.getType());
        boolean isSet = "set".equalsIgnoreCase(transaction.getType());
        boolean isAdminTransaction = "admin".equalsIgnoreCase(transaction.getDescription()) || "console".equalsIgnoreCase(transaction.getDescription());

        Material material;
        if (isSet) {
            material = resolveMaterial("WHITE_DYE", "BONE_MEAL");
        } else if (isAdminTransaction) {
            material = isDeposit
                    ? resolveMaterial("GREEN_DYE", "CACTUS_GREEN", "LIME_DYE")
                    : resolveMaterial("ORANGE_DYE", "RED_DYE", "ROSE_RED");
        } else {
            material = isDeposit
                    ? resolveMaterial("LIME_DYE", "GREEN_DYE", "CACTUS_GREEN")
                    : resolveMaterial("RED_DYE", "ROSE_RED");
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String amount = BSP.format(plugin, String.valueOf(transaction.getAmount()));
        String typeLabel = isSet ? "&fset" : (isDeposit ? "&adeposit" : "&cwithdraw");

        String titleColor = isSet ? "&f" : (isDeposit ? "&a" : "&c");
        meta.setDisplayName(MU.getColoredText(titleColor + amount));

        List<String> lore = new ArrayList<>();
        lore.add(MU.getColoredText(typeLabel));
        String descriptionText;
        if ("admin".equalsIgnoreCase(transaction.getDescription())) {
            descriptionText = "&6admin";
        } else if ("console".equalsIgnoreCase(transaction.getDescription())) {
                descriptionText = "&dconsole";
        } else {
            descriptionText = "&7self";
        }
        lore.add(MU.getColoredText("&f" + descriptionText));
        lore.add(MU.getColoredText("&b" + transaction.getContext()));
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(transaction.getTimestamp()));
        lore.add(MU.getColoredText("&7" + formattedDate));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private Material resolveMaterial(String... materialNames) {
        for (String name : materialNames) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try next fallback name for older/newer Minecraft versions.
            }
        }
        return Material.PAPER;
    }
}
