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

    public TransactionGUI(MineBank plugin, TransactionService transactionService) {
        this.plugin = plugin;
        this.transactionService = transactionService;
    }

    public void open(Player player, int page) {

        int safePage = Math.max(1, page);
        currentPages.put(player.getUniqueId(), safePage);

        GuiUtils GUIU = new GuiUtils(plugin);
        GUIU.openGUI(player, "transactions");
        renderTransactions(player, safePage);
    }

    public void openNextPage(Player player) {
        int currentPage = getCurrentPage(player);
        int nextPage = currentPage + 1;
        List<TransactionData> nextPageRows = transactionService.getPlayerTransactions(player.getUniqueId(), nextPage, PAGE_SIZE);

        if (nextPageRows.isEmpty()) {
            return;
        }

        open(player, nextPage);
    }

    public void openPreviousPage(Player player) {
        int currentPage = getCurrentPage(player);
        int previousPage = Math.max(1, currentPage - 1);
        open(player, previousPage);
    }

    public int getCurrentPage(Player player) {
        return currentPages.getOrDefault(player.getUniqueId(), 1);
    }

    private void renderTransactions(Player player, int page) {

        Inventory top = player.getOpenInventory().getTopInventory();

        List<TransactionData> rows = transactionService.getPlayerTransactions(player.getUniqueId(), page, PAGE_SIZE);
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
        boolean isAdminTransaction = "admin".equalsIgnoreCase(transaction.getDescription());

        Material material;
        if (isSet) {
            material = Material.WHITE_DYE;
        } else if (isAdminTransaction) {
            material = isDeposit ? Material.GREEN_DYE : Material.ORANGE_DYE;
        } else {
            material = isDeposit ? Material.LIME_DYE : Material.RED_DYE;
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
}
