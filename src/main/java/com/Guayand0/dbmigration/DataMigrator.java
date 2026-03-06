package com.Guayand0.dbmigration;

import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class DataMigrator {

    public static MigrationResult migrate(DataStorage from, DataStorage to) {

        int players = 0;
        int banks = 0;
        int interest = 0;

        // -------- PLAYER DATA --------
        for (UUID uuid : from.getAllPlayerUUIDs()) {
            PlayerData data = from.loadPlayerData(uuid);
            if (data != null) {
                to.savePlayerData(uuid, data);
                players++;
            }
        }

        // -------- BANK DATA --------
        List<String> bankNames = from.getAllBankNames();
        int priority = 1; // asignar prioridad según el orden de getAllBankNames()
        for (String bankName : bankNames) {
            Map<String, BankData> bankDataMap = from.loadBankData(bankName);
            if (bankDataMap != null && !bankDataMap.isEmpty()) {
                BankData bankData = bankDataMap.values().iterator().next();
                Map<String, BankData> mapToSave = new HashMap<>();
                mapToSave.put(bankName, bankData);

                to.saveBankData(bankName, mapToSave, priority);
                banks++;
                priority++;
            }
        }

        // -------- ACCRUED INTEREST --------
        interest = from.loadAccruedInterestData();
        to.saveAccruedInterestData(interest);

        return new MigrationResult(players, banks, interest);
    }
}