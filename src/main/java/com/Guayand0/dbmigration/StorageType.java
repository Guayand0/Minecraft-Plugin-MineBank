package com.Guayand0.dbmigration;

public enum StorageType {

    JSON,
    MYSQL;
    // FUTURE: SQLITE, MONGODB...

    // Añadirlos también en MineBank-setupStorages()
    // Añadirlos también en MineBank-getDataStorageType()

    public static StorageType fromString(String type) {
        try {
            return StorageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}