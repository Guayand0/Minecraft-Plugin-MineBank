package com.Guayand0.data.transactions;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransactionService {

    private final MineBank plugin;
    private final TransactionStorage storage;

    private final MessageUtils MU = new MessageUtils();

    public TransactionService(MineBank plugin, TransactionStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void initialize() {
        try {
            storage.initialize();
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to initialize transaction storage: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void register(UUID playerUuid, String type, int amount, String context, String description) {
        if (playerUuid == null || amount <= 0) {
            return;
        }

        String normalizedType = normalizeType(type);
        long timestamp = System.currentTimeMillis();
        String normalizedDescription = (description == null || description.trim().isEmpty()) ? "self" : description.trim().toLowerCase(Locale.ROOT);
        String normalizedContext = (context == null || context.trim().isEmpty()) ? "plugin" : context.trim().toLowerCase(Locale.ROOT);

        TransactionData entry = new TransactionData(
                UUID.randomUUID().toString(),
                playerUuid.toString(),
                normalizedType,
                amount,
                normalizedDescription,
                normalizedContext,
                timestamp
        );

        try {
            storage.save(entry);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to save transaction: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public List<TransactionData> getPlayerTransactions(UUID playerUuid, int page, int pageSize) {
        if (playerUuid == null || pageSize <= 0) {
            return Collections.emptyList();
        }

        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;

        try {
            return storage.findByPlayer(playerUuid.toString(), pageSize, offset);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to load transactions: " + e.getMessage()));
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<TransactionData> getPlayerTransactionsOrdered(UUID playerUuid, int page, int pageSize, boolean asc) {
        if (playerUuid == null || pageSize <= 0) {
            return Collections.emptyList();
        }

        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;

        try {
            return storage.findByPlayerOrdered(playerUuid.toString(), pageSize, offset, asc);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to load transactions: " + e.getMessage()));
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<TransactionData> getPlayerTransactionsFilteredOrdered(UUID playerUuid, int page, int pageSize, String typeFilter, boolean asc) {
        if (playerUuid == null || pageSize <= 0) {
            return Collections.emptyList();
        }

        String normalizedFilter = normalizeType(typeFilter);
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;

        try {
            return storage.findByPlayerAndTypeOrdered(playerUuid.toString(), normalizedFilter, pageSize, offset, asc);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to load transactions: " + e.getMessage()));
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<TransactionData> getPlayerTransactionsFiltered(UUID playerUuid, int page, int pageSize, String typeFilter) {
        if (playerUuid == null || pageSize <= 0) {
            return Collections.emptyList();
        }

        String normalizedFilter = normalizeType(typeFilter);
        int safePage = Math.max(1, page);
        int targetSkip = (safePage - 1) * pageSize;
        int collected = 0;
        int skipped = 0;
        int offset = 0;
        int chunkSize = Math.max(50, pageSize * 2);

        List<TransactionData> result = new java.util.ArrayList<>();

        try {
            while (collected < pageSize) {
                List<TransactionData> chunk = storage.findByPlayer(playerUuid.toString(), chunkSize, offset);
                if (chunk.isEmpty()) break;

                for (TransactionData row : chunk) {
                    if (row == null) continue;
                    if (!normalizedFilter.equalsIgnoreCase(row.getType())) continue;

                    if (skipped < targetSkip) {
                        skipped++;
                        continue;
                    }

                    result.add(row);
                    collected++;
                    if (collected >= pageSize) break;
                }

                if (chunk.size() < chunkSize) break;
                offset += chunkSize;
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to load transactions: " + e.getMessage()));
            e.printStackTrace();
            return Collections.emptyList();
        }

        return result;
    }

    public int countPlayerTransactions(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        try {
            return storage.countByPlayer(playerUuid.toString());
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to count transactions: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    public int countPlayerTransactionsFiltered(UUID playerUuid, String typeFilter) {
        if (playerUuid == null) {
            return 0;
        }
        String normalizedFilter = normalizeType(typeFilter);
        try {
            return storage.countByPlayerAndType(playerUuid.toString(), normalizedFilter);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to count transactions: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "deposit";
        }

        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("withdraw")) {
            return "withdraw";
        }
        if (normalized.equals("set")) {
            return "set";
        }
        return "deposit";
    }
}
