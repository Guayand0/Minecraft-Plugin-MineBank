package com.Guayand0.data;

import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.bank.BankData;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DataStorage {

    // ---------------- PLAYER DATA ----------------
    void savePlayerData(UUID uuid, PlayerData data);
    PlayerData loadPlayerData(UUID uuid);
    List<UUID> getAllPlayerUUIDs();
    List<String> getAllPlayerNames();

    // ---------------- PLAYER TOP ----------------
    List<List<String>> getTopPlayerBankData(int amount);


    // ---------------- BANK DATA ----------------
    void saveBankData(String bankName, Map<String, BankData> bankData, int priority);
    Map<String, BankData> loadBankData(String bankName);
    List<String> getAllBankNames();

    // ---------------- ACCRUED INTERESTS ----------------
    void saveAccruedInterestData(int value);
    int loadAccruedInterestData();

    // ---------------- BEFORE-MIGRATION DATA ----------------
    void clearAllData();

    // ---------------- BACKUP ----------------
    void backup() throws Exception;
}
