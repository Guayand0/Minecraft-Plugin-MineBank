package com.Guayand0.dbmigration;

import com.Guayand0.data.DataStorage;
import com.Guayand0.data.player.PlayerData;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private final Map<StorageType, DataStorage> storages = new EnumMap<>(StorageType.class);

    public void register(StorageType type, DataStorage storage) {
        storages.put(type, storage);
    }

    public DataStorage get(StorageType type) {
        return storages.get(type);
    }

    public boolean exists(StorageType type) {
        return storages.containsKey(type);
    }
}