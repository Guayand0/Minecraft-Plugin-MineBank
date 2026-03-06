package com.Guayand0.dbmigration;

public class PendingMigration {
    public final StorageType from;
    public final StorageType to;
    public final boolean backup;

    public PendingMigration(StorageType from, StorageType to, boolean backup) {
        this.from = from;
        this.to = to;
        this.backup = backup;
    }
}
