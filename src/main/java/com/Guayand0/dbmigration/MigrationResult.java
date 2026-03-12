package com.Guayand0.dbmigration;

public class MigrationResult {

    private final int playersMigrated;
    private final int banksMigrated;
    private final int accruedInterest;
    private final int transactionsMigrated;

    public MigrationResult(int playersMigrated, int banksMigrated, int accruedInterest, int transactionsMigrated) {
        this.playersMigrated = playersMigrated;
        this.banksMigrated = banksMigrated;
        this.accruedInterest = accruedInterest;
        this.transactionsMigrated = transactionsMigrated;
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

    public int getTransactionsMigrated() {
        return transactionsMigrated;
    }
}
