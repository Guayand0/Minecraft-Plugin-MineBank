package com.Guayand0.data;

import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.zlib.PlayerUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MySQLStorage implements DataStorage {

    private Connection connection;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final PlayerUtils PU = new PlayerUtils();
    private static final long TOP_CACHE_TTL_MS = 5000L;

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String pass;
    private final String params;

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final Map<String, Map<String, BankData>> bankDataCache = new HashMap<>();
    private final Map<UUID, String> playerNameCache = new HashMap<>();
    private List<UUID> cachedPlayerUUIDs;
    private long topCacheExpiresAt = 0L;
    private List<List<String>> cachedTopRows = Collections.emptyList();

    public MySQLStorage(String host, int port, String database, String user, String pass, String params) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.pass = pass;
        this.params = params;
    }

    private void connect() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        if (params != null && !params.isEmpty()) {
            if (!params.startsWith("?")) url += "?";
            url += params;
        }

        connection = DriverManager.getConnection(url, user, pass);
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public void prepareTables() {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "CREATE TABLE IF NOT EXISTS bank_data (" +
                        "priority INT UNIQUE NOT NULL," +
                        "name VARCHAR(250) UNIQUE NOT NULL," +
                        "json LONGTEXT NOT NULL," +
                        "PRIMARY KEY(priority, name)" +
                        ");"
        )) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (PreparedStatement ps = getConnection().prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "json LONGTEXT NOT NULL" +
                        ");"
        )) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (PreparedStatement ps = getConnection().prepareStatement(
                "CREATE TABLE IF NOT EXISTS interests_data (" +
                        "type VARCHAR(250) PRIMARY KEY," +
                        "json LONGTEXT NOT NULL" +
                        ");"
        )) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- PLAYER DATA ----------------
    @Override
    public void savePlayerData(UUID uuid, PlayerData data) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO player_data (uuid, json) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE json = VALUES(json)"
        )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gson.toJson(data));
            ps.executeUpdate();

            synchronized (this) {
                playerDataCache.put(uuid, data);
                cachedPlayerUUIDs = null;
                invalidateTopCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        synchronized (this) {
            PlayerData cached = playerDataCache.get(uuid);
            if (cached != null) return cached;
        }

        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT json FROM player_data WHERE uuid=?"
        )) {
            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = gson.fromJson(rs.getString("json"), PlayerData.class);
                    synchronized (this) {
                        playerDataCache.put(uuid, data);
                    }
                    return data;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<UUID> getAllPlayerUUIDs() {
        synchronized (this) {
            if (cachedPlayerUUIDs != null) {
                return new ArrayList<>(cachedPlayerUUIDs);
            }
        }

        List<UUID> uuids = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT uuid FROM player_data"
        );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    uuids.add(UUID.fromString(rs.getString("uuid")));
                } catch (Exception ignored) {}
            }

            synchronized (this) {
                cachedPlayerUUIDs = new ArrayList<>(uuids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uuids;
    }

    @Override
    public List<String> getAllPlayerNames() {
        List<String> names = new ArrayList<>();

        for (UUID uuid : getAllPlayerUUIDs()) {
            String name;
            synchronized (this) {
                name = playerNameCache.get(uuid);
            }

            if (name == null) {
                name = PU.getNameFromUUID(uuid);
                if (name != null) {
                    synchronized (this) {
                        playerNameCache.put(uuid, name);
                    }
                }
            }

            if (name != null) {
                names.add(name);
            }
        }

        return names;
    }


    // ---------------- PLAYER TOP DATA ----------------
    @Override
    public List<List<String>> getTopPlayerBankData(int amount) {
        if (amount <= 0) return new ArrayList<>();

        synchronized (this) {
            long now = System.currentTimeMillis();
            if (now < topCacheExpiresAt && !cachedTopRows.isEmpty()) {
                int topSize = Math.min(amount, cachedTopRows.size());
                return new ArrayList<>(cachedTopRows.subList(0, topSize));
            }
        }

        List<List<String>> result = new ArrayList<>();
        Map<UUID, PlayerData> players = new HashMap<>();

        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT uuid, json FROM player_data"
        );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerData data = gson.fromJson(rs.getString("json"), PlayerData.class);

                if (data != null && data.getBank() != null) {
                    players.put(uuid, data);
                }
            }

            List<Map.Entry<UUID, PlayerData>> sorted = new ArrayList<>(players.entrySet());

            sorted.sort((a, b) -> Integer.compare(
                    b.getValue().getBank().getBalance(),
                    a.getValue().getBank().getBalance()
            ));

            int pos = 0;
            for (Map.Entry<UUID, PlayerData> entry : sorted) {
                if (pos >= amount) break;

                UUID uuid = entry.getKey();
                String playerName;
                synchronized (this) {
                    playerName = playerNameCache.get(uuid);
                }

                if (playerName == null) {
                    playerName = PU.getNameFromUUID(uuid);
                    if (playerName != null) {
                        synchronized (this) {
                            playerNameCache.put(uuid, playerName);
                        }
                    }
                }

                PlayerData data = entry.getValue();

                List<String> row = new ArrayList<>();
                row.add(playerName);
                row.add(data.getBank().getName());
                row.add(String.valueOf(data.getBank().getLevel()));
                row.add(String.valueOf(data.getBank().getBalance()));

                result.add(row);
                pos++;
            }

            synchronized (this) {
                cachedTopRows = new ArrayList<>(result);
                topCacheExpiresAt = System.currentTimeMillis() + TOP_CACHE_TTL_MS;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    // ---------------- BANK DATA ----------------
    @Override
    public void saveBankData(String bankName, Map<String, BankData> bankData, int priority) {
        try {
            BankData data = bankData.get(bankName);
            if (data == null) return;

            // Guardamos solo levels envuelto en "levels"
            Map<String, Map<String, BankData.Level>> wrapper = new HashMap<>();
            wrapper.put("levels", data.getLevels());

            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO bank_data (priority, name, json) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE json = VALUES(json)"
            )) {

                ps.setInt(1, priority); // prioridad segun banks.yml
                ps.setString(2, bankName);
                ps.setString(3, gson.toJson(wrapper));

                ps.executeUpdate();
            }

            synchronized (this) {
                bankDataCache.remove(bankName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, BankData> loadBankData(String bankName) {
        synchronized (this) {
            Map<String, BankData> cached = bankDataCache.get(bankName);
            if (cached != null) {
                return new HashMap<>(cached);
            }
        }

        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT json FROM bank_data WHERE name=? ORDER BY priority ASC LIMIT 1"
        )) {
            ps.setString(1, bankName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("json");
                    Type type = new TypeToken<Map<String, Map<String, BankData.Level>>>(){}.getType();
                    Map<String, Map<String, BankData.Level>> raw = gson.fromJson(json, type);

                    BankData bankData = new BankData(bankName, raw.get("levels"));
                    Map<String, BankData> result = new HashMap<>();
                    result.put(bankName, bankData);

                    synchronized (this) {
                        bankDataCache.put(bankName, new HashMap<>(result));
                    }

                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getAllBankNames() {
        List<String> banks = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT name FROM bank_data ORDER BY priority ASC"
        );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String bankName = rs.getString("name");
                if (bankName != null && !bankName.isEmpty()) {
                    banks.add(bankName);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return banks;
    }


    // ---------------- ACCRUED INTERESTS ----------------
    @Override
    public void saveAccruedInterestData(int value) {
        try {
            Map<String, Integer> data = new HashMap<>();
            data.put("accrued_interest", value);

            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO interests_data (type, json) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE json = VALUES(json)"
            )) {
                ps.setString(1, "global");
                ps.setString(2, gson.toJson(data));
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int loadAccruedInterestData() {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT json FROM interests_data WHERE type=?"
        )) {
            ps.setString(1, "global");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Integer> data = gson.fromJson(rs.getString("json"), new TypeToken<Map<String, Integer>>(){}.getType());
                    return data.getOrDefault("accrued_interest", 0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ---------------- BEFORE-MIGRATION DATA ----------------
    @Override
    public void clearAllData() {
        try {
            Connection conn = getConnection();

            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM player_data");
                st.executeUpdate("DELETE FROM bank_data");
                st.executeUpdate("DELETE FROM interests_data");
            }

            synchronized (this) {
                playerDataCache.clear();
                bankDataCache.clear();
                playerNameCache.clear();
                cachedPlayerUUIDs = null;
                invalidateTopCache();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------------- BACKUP ----------------
    @Override
    public void backup() throws Exception {
        String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        Connection conn = getConnection();

        // Nombres dinamicos de las tablas de backup
        String playerBackupTable = "player_data_backup_" + date;
        String bankBackupTable = "bank_data_backup_" + date;
        String interestsBackupTable = "interests_data_backup_" + date;

        // Crear tablas de backup si no existen (estructura igual a original)
        conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + playerBackupTable + " LIKE player_data;"
        );
        conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + bankBackupTable + " LIKE bank_data;"
        );
        conn.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + interestsBackupTable + " LIKE interests_data;"
        );

        // ---------------- Player Data ----------------
        conn.createStatement().executeUpdate(
                "INSERT INTO " + playerBackupTable + " (uuid, json) " +
                        "SELECT uuid, json FROM player_data " +
                        "ON DUPLICATE KEY UPDATE json = VALUES(json);"
        );

        // ---------------- Bank Data ----------------
        conn.createStatement().executeUpdate(
                "INSERT INTO " + bankBackupTable + " (priority, name, json) " +
                        "SELECT priority, name, json FROM bank_data " +
                        "ON DUPLICATE KEY UPDATE json = VALUES(json);"
        );

        // ---------------- Interests ----------------
        conn.createStatement().executeUpdate(
                "INSERT INTO " + interestsBackupTable + " (type, json) " +
                        "SELECT type, json FROM interests_data " +
                        "ON DUPLICATE KEY UPDATE json = VALUES(json);"
        );
    }

    private synchronized void invalidateTopCache() {
        topCacheExpiresAt = 0L;
        cachedTopRows = Collections.emptyList();
    }
}
