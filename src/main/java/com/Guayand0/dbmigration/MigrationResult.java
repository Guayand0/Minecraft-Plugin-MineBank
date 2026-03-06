package com.Guayand0.dbmigration;

public class MigrationResult {

    private final int playersMigrated;
    private final int banksMigrated;
    private final int accruedInterest;

    public MigrationResult(int playersMigrated, int banksMigrated, int accruedInterest) {
        this.playersMigrated = playersMigrated;
        this.banksMigrated = banksMigrated;
        this.accruedInterest = accruedInterest;
    }

    public int getPlayersMigrated() {
        return playersMigrated;
    }

    public int getBanksMigrated() {
        return banksMigrated;
    }

    public int getAccruedInterest() {
        return accruedInterest;
    }
}