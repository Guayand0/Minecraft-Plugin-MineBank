package com.Guayand0.data.transactions;

public class TransactionData {

    private final String id;
    private final String playerUuid;
    private final String type;
    private final int amount;
    private final String description;
    private final String context;
    private final long timestamp;

    public TransactionData(String id, String playerUuid, String type, int amount, String description, String context, long timestamp) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.context = context;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getContext() {
        return context;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
