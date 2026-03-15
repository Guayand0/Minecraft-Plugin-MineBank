package com.Guayand0.dbmigration;

import com.Guayand0.data.DataStorage;
import com.Guayand0.data.JsonStorage;
import com.Guayand0.data.MySQLStorage;
//import com.Guayand0.data.PostgreSQLStorage;
import com.Guayand0.data.SQLiteStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataMigrator {

    public static MigrationResult migrate(DataStorage from, DataStorage to) {

        int players = 0;
        int banks = 0;
        int interest = 0;
        int transactions = 0;

        SQLiteStorage sqliteTo = (to instanceof SQLiteStorage) ? (SQLiteStorage) to : null;
        MySQLStorage mysqlTo = (to instanceof MySQLStorage) ? (MySQLStorage) to : null;
        //PostgreSQLStorage pgTo = (to instanceof PostgreSQLStorage) ? (PostgreSQLStorage) to : null;
        JsonStorage jsonTo = (to instanceof JsonStorage) ? (JsonStorage) to : null;
        boolean bulkStarted = false;

        try {
            if (sqliteTo != null) {
                sqliteTo.beginBulkOperation();
                bulkStarted = true;
            } else if (mysqlTo != null) {
                mysqlTo.beginBulkOperation();
                bulkStarted = true;
            } /*else if (pgTo != null) {
                pgTo.beginBulkOperation();
                bulkStarted = true;
            }*/ else if (jsonTo != null) {
                jsonTo.beginBulkOperation();
                bulkStarted = true;
            }

            // -------- PLAYER DATA --------
            List<UUID> allPlayerUuids = from.getAllPlayerUUIDs();
            Map<UUID, PlayerData> playersToSave = new HashMap<>();
            for (UUID uuid : allPlayerUuids) {
                PlayerData data = from.loadPlayerData(uuid);
                if (data != null) {
                    playersToSave.put(uuid, data);
                    players++;
                }
            }
            if (sqliteTo != null) {
                sqliteTo.savePlayersBatch(playersToSave);
            } else if (mysqlTo != null) {
                mysqlTo.savePlayersBatch(playersToSave);
            } /*else if (pgTo != null) {
                pgTo.savePlayersBatch(playersToSave);
            }*/ else if (jsonTo != null) {
                jsonTo.savePlayersBatch(playersToSave);
            } else {
                for (Map.Entry<UUID, PlayerData> entry : playersToSave.entrySet()) {
                    to.savePlayerData(entry.getKey(), entry.getValue());
                }
            }

            // -------- BANK DATA --------
            List<String> bankNames = from.getAllBankNames();
            List<String> bankNamesToSave = new ArrayList<>();
            List<BankData> bankDataToSave = new ArrayList<>();
            List<Integer> bankPrioritiesToSave = new ArrayList<>();
            int priority = 1; // asignar prioridad segun el orden de getAllBankNames()
            for (String bankName : bankNames) {
                Map<String, BankData> bankDataMap = from.loadBankData(bankName);
                if (bankDataMap != null && !bankDataMap.isEmpty()) {
                    BankData bankData = bankDataMap.values().iterator().next();
                    bankNamesToSave.add(bankName);
                    bankDataToSave.add(bankData);
                    bankPrioritiesToSave.add(priority);
                    banks++;
                }
                priority++;
            }

            if (sqliteTo != null) {
                sqliteTo.saveBanksBatch(bankNamesToSave, bankDataToSave, bankPrioritiesToSave);
            } else if (mysqlTo != null) {
                mysqlTo.saveBanksBatch(bankNamesToSave, bankDataToSave, bankPrioritiesToSave);
            } /*else if (pgTo != null) {
                pgTo.saveBanksBatch(bankNamesToSave, bankDataToSave, bankPrioritiesToSave);
            }*/ else if (jsonTo != null) {
                jsonTo.saveBanksBatch(bankNamesToSave, bankDataToSave, bankPrioritiesToSave);
            } else {
                for (int i = 0; i < bankNamesToSave.size(); i++) {
                    String bankName = bankNamesToSave.get(i);
                    BankData bankData = bankDataToSave.get(i);
                    int p = bankPrioritiesToSave.size() > i && bankPrioritiesToSave.get(i) != null
                            ? bankPrioritiesToSave.get(i)
                            : (i + 1);
                    Map<String, BankData> mapToSave = new HashMap<>();
                    mapToSave.put(bankName, bankData);
                    to.saveBankData(bankName, mapToSave, p);
                }
            }

            // -------- ACCRUED INTEREST --------
            interest = from.loadAccruedInterestData();
            to.saveAccruedInterestData(interest);

            // -------- TRANSACTIONS --------
            List<TransactionData> allTransactions = from.getAllTransactions();
            if (allTransactions != null) {
                if (sqliteTo != null) {
                    sqliteTo.saveTransactionsBatch(allTransactions);
                    transactions = allTransactions.size();
                } else if (mysqlTo != null) {
                    mysqlTo.saveTransactionsBatch(allTransactions);
                    transactions = allTransactions.size();
                } /*else if (pgTo != null) {
                    pgTo.saveTransactionsBatch(allTransactions);
                    transactions = allTransactions.size();
                }*/ else if (jsonTo != null) {
                    jsonTo.saveTransactionsBatch(allTransactions);
                    transactions = allTransactions.size();
                } else {
                    for (TransactionData transaction : allTransactions) {
                        if (transaction != null) {
                            to.saveTransaction(transaction);
                            transactions++;
                        }
                    }
                }
            }

            if (sqliteTo != null) {
                sqliteTo.endBulkOperation(true);
            } else if (mysqlTo != null) {
                mysqlTo.endBulkOperation(true);
            } /*else if (pgTo != null) {
                pgTo.endBulkOperation(true);
            }*/ else if (jsonTo != null) {
                jsonTo.endBulkOperation(true);
            }

            return new MigrationResult(players, banks, interest, transactions);
        } catch (Exception e) {
            if (sqliteTo != null && bulkStarted) {
                sqliteTo.endBulkOperation(false);
            } else if (mysqlTo != null && bulkStarted) {
                mysqlTo.endBulkOperation(false);
            } /*else if (pgTo != null && bulkStarted) {
                pgTo.endBulkOperation(false);
            }*/ else if (jsonTo != null && bulkStarted) {
                jsonTo.endBulkOperation(false);
            }
            throw new RuntimeException(e);
        }
    }
}
