package com.Guayand0.data.transactions;

import com.Guayand0.MineBank;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TransactionService {

    private final MineBank plugin;
    private final TransactionStorage storage;

    public TransactionService(MineBank plugin, TransactionStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void initialize() {
        try {
            storage.initialize();
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("[MineBank] Failed to initialize transaction storage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void register(UUID playerUuid, String type, int amount, String context) {
        register(playerUuid, type, amount, context, "self");
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
            Bukkit.getConsoleSender().sendMessage(plugin.prefix + " Failed to save transaction: " + e.getMessage());
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
            Bukkit.getConsoleSender().sendMessage(plugin.prefix + " Failed to load transactions: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
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
