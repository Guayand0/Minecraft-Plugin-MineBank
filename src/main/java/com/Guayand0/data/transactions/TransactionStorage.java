package com.Guayand0.data.transactions;

import java.util.List;

public interface TransactionStorage {
    void initialize() throws Exception;
    void save(TransactionData transaction) throws Exception;
    List<TransactionData> findByPlayer(String playerUuid, int limit, int offset) throws Exception;
}
