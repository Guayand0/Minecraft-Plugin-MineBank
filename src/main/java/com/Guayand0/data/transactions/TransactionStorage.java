package com.Guayand0.data.transactions;

import java.util.List;

public interface TransactionStorage {
    void initialize() throws Exception;
    void save(TransactionData transaction) throws Exception;
    List<TransactionData> findByPlayer(String playerUuid, int limit, int offset) throws Exception;
    List<TransactionData> findByPlayerOrdered(String playerUuid, int limit, int offset, boolean asc) throws Exception;
    List<TransactionData> findByPlayerAndTypeOrdered(String playerUuid, String type, int limit, int offset, boolean asc) throws Exception;
    int countByPlayer(String playerUuid) throws Exception;
    int countByPlayerAndType(String playerUuid, String type) throws Exception;
}
