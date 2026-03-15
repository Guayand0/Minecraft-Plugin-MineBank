//package com.Guayand0.data;
//
//import com.Guayand0.data.bank.BankData;
//import com.Guayand0.data.player.PlayerData;
//import com.Guayand0.data.transactions.TransactionData;
//import com.Guayand0.data.transactions.TransactionStorage;
//import com.Guayand0.zlib.PlayerUtils;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;
//
//import java.lang.reflect.Type;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.Statement;
//import java.sql.SQLException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//public class PostgreSQLStorage implements DataStorage, TransactionStorage {
//
//    private Connection connection;
//    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
//    private final PlayerUtils PU = new PlayerUtils();
//    private static final long TOP_CACHE_TTL_MS = 5000L;
//    private boolean bulkMode = false;
//    private Boolean bulkPreviousAutoCommit = null;
//
//    private final String connectionUri;
//    private final String host;
//    private final int port;
//    private final String database;
//    private final String user;
//    private final String pass;
//    private final String params;
//
//    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
//    private final Map<String, Map<String, BankData>> bankDataCache = new HashMap<>();
//    private final Map<UUID, String> playerNameCache = new HashMap<>();
//    private List<UUID> cachedPlayerUUIDs;
//    private long topCacheExpiresAt = 0L;
//    private List<List<String>> cachedTopRows = Collections.emptyList();
//
//    public PostgreSQLStorage(String host, int port, String database, String user, String pass, String params) {
//        this.connectionUri = null;
//        this.host = host;
//        this.port = port;
//        this.database = database;
//        this.user = user;
//        this.pass = pass;
//        this.params = params;
//    }
//
//    public PostgreSQLStorage(String connectionUri) {
//        this.connectionUri = connectionUri;
//        this.host = null;
//        this.port = 0;
//        this.database = null;
//        this.user = null;
//        this.pass = null;
//        this.params = null;
//    }
//
//    private void connect() throws SQLException {
//
//        if (connectionUri != null && !connectionUri.isEmpty()) {
//            String url = "jdbc:" + connectionUri;
//            connection = DriverManager.getConnection(url);
//            return;
//        }
//
//        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
//
//        if (params != null && !params.isEmpty()) {
//            if (!params.startsWith("?")) url += "?";
//            url += params;
//        }
//
//        connection = DriverManager.getConnection(url, user, pass);
//    }
//
//    private Connection getConnection() throws SQLException {
//        if (connection == null || connection.isClosed()) {
//            connect();
//        }
//        return connection;
//    }
//
//    public void prepareTables() {
//        try (Statement st = getConnection().createStatement()) {
//            st.executeUpdate(
//                    "CREATE TABLE IF NOT EXISTS bank_data (" +
//                            "priority INTEGER UNIQUE NOT NULL," +
//                            "name VARCHAR(250) UNIQUE NOT NULL," +
//                            "json TEXT NOT NULL," +
//                            "PRIMARY KEY(name)" +
//                            ")"
//            );
//            st.executeUpdate(
//                    "CREATE TABLE IF NOT EXISTS player_data (" +
//                            "uuid VARCHAR(36) PRIMARY KEY," +
//                            "json TEXT NOT NULL" +
//                            ")"
//            );
//            st.executeUpdate(
//                    "CREATE TABLE IF NOT EXISTS interests_data (" +
//                            "type VARCHAR(250) PRIMARY KEY," +
//                            "json TEXT NOT NULL" +
//                            ")"
//            );
//            st.executeUpdate(
//                    "CREATE TABLE IF NOT EXISTS transactions (" +
//                            "id VARCHAR(36) PRIMARY KEY," +
//                            "player_uuid VARCHAR(36) NOT NULL," +
//                            "type VARCHAR(32) NOT NULL," +
//                            "amount INTEGER NOT NULL," +
//                            "description TEXT NOT NULL," +
//                            "context VARCHAR(32) NOT NULL," +
//                            "timestamp BIGINT NOT NULL" +
//                            ")"
//            );
//            st.executeUpdate(
//                    "CREATE INDEX IF NOT EXISTS idx_transactions_player_ts ON transactions (player_uuid, timestamp DESC)"
//            );
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ---------------- PLAYER DATA ----------------
//    @Override
//    public void savePlayerData(UUID uuid, PlayerData data) {
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "INSERT INTO player_data (uuid, json) VALUES (?, ?) " +
//                        "ON CONFLICT(uuid) DO UPDATE SET json = EXCLUDED.json"
//        )) {
//            ps.setString(1, uuid.toString());
//            ps.setString(2, gson.toJson(data));
//            ps.executeUpdate();
//
//            synchronized (this) {
//                playerDataCache.put(uuid, data);
//                cachedPlayerUUIDs = null;
//                invalidateTopCache();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void savePlayersBatch(Map<UUID, PlayerData> players) {
//        if (players == null || players.isEmpty()) return;
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "INSERT INTO player_data (uuid, json) VALUES (?, ?) " +
//                        "ON CONFLICT(uuid) DO UPDATE SET json = EXCLUDED.json"
//        )) {
//            for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
//                if (entry.getKey() == null || entry.getValue() == null) continue;
//                ps.setString(1, entry.getKey().toString());
//                ps.setString(2, gson.toJson(entry.getValue()));
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public PlayerData loadPlayerData(UUID uuid) {
//        synchronized (this) {
//            PlayerData cached = playerDataCache.get(uuid);
//            if (cached != null) return cached;
//        }
//
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT json FROM player_data WHERE uuid=?"
//        )) {
//            ps.setString(1, uuid.toString());
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    PlayerData data = gson.fromJson(rs.getString("json"), PlayerData.class);
//                    synchronized (this) {
//                        playerDataCache.put(uuid, data);
//                    }
//                    return data;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    public List<UUID> getAllPlayerUUIDs() {
//        synchronized (this) {
//            if (cachedPlayerUUIDs != null) {
//                return new ArrayList<>(cachedPlayerUUIDs);
//            }
//        }
//
//        List<UUID> uuids = new ArrayList<>();
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT uuid FROM player_data"
//        );
//             ResultSet rs = ps.executeQuery()) {
//
//            while (rs.next()) {
//                try {
//                    uuids.add(UUID.fromString(rs.getString("uuid")));
//                } catch (Exception ignored) {}
//            }
//
//            synchronized (this) {
//                cachedPlayerUUIDs = new ArrayList<>(uuids);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return uuids;
//    }
//
//    @Override
//    public List<String> getAllPlayerNames() {
//        List<String> names = new ArrayList<>();
//
//        for (UUID uuid : getAllPlayerUUIDs()) {
//            String name;
//            synchronized (this) {
//                name = playerNameCache.get(uuid);
//            }
//
//            if (name == null) {
//                name = PU.getNameFromUUID(uuid);
//                if (name != null) {
//                    synchronized (this) {
//                        playerNameCache.put(uuid, name);
//                    }
//                }
//            }
//
//            if (name != null) {
//                names.add(name);
//            }
//        }
//
//        return names;
//    }
//
//    // ---------------- PLAYER TOP DATA ----------------
//    @Override
//    public List<List<String>> getTopPlayerBankData(int amount) {
//        if (amount <= 0) return new ArrayList<>();
//
//        synchronized (this) {
//            long now = System.currentTimeMillis();
//            if (now < topCacheExpiresAt && !cachedTopRows.isEmpty()) {
//                int topSize = Math.min(amount, cachedTopRows.size());
//                return new ArrayList<>(cachedTopRows.subList(0, topSize));
//            }
//        }
//
//        List<List<String>> result = new ArrayList<>();
//        Map<UUID, PlayerData> players = new HashMap<>();
//
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT uuid, json FROM player_data"
//        );
//             ResultSet rs = ps.executeQuery()) {
//
//            while (rs.next()) {
//                UUID uuid = UUID.fromString(rs.getString("uuid"));
//                PlayerData data = gson.fromJson(rs.getString("json"), PlayerData.class);
//
//                if (data != null && data.getBank() != null) {
//                    players.put(uuid, data);
//                }
//            }
//
//            List<Map.Entry<UUID, PlayerData>> sorted = new ArrayList<>(players.entrySet());
//
//            sorted.sort((a, b) -> Integer.compare(
//                    b.getValue().getBank().getBalance(),
//                    a.getValue().getBank().getBalance()
//            ));
//
//            int pos = 0;
//            for (Map.Entry<UUID, PlayerData> entry : sorted) {
//                if (pos >= amount) break;
//
//                UUID uuid = entry.getKey();
//                String playerName;
//                synchronized (this) {
//                    playerName = playerNameCache.get(uuid);
//                }
//
//                if (playerName == null) {
//                    playerName = PU.getNameFromUUID(uuid);
//                    if (playerName != null) {
//                        synchronized (this) {
//                            playerNameCache.put(uuid, playerName);
//                        }
//                    }
//                }
//
//                PlayerData data = entry.getValue();
//
//                List<String> row = new ArrayList<>();
//                row.add(playerName);
//                row.add(data.getBank().getName());
//                row.add(String.valueOf(data.getBank().getLevel()));
//                row.add(String.valueOf(data.getBank().getBalance()));
//
//                result.add(row);
//                pos++;
//            }
//
//            synchronized (this) {
//                cachedTopRows = new ArrayList<>(result);
//                topCacheExpiresAt = System.currentTimeMillis() + TOP_CACHE_TTL_MS;
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }
//
//    // ---------------- BANK DATA ----------------
//    @Override
//    public void saveBankData(String bankName, Map<String, BankData> bankData, int priority) {
//        try {
//            BankData data = bankData.get(bankName);
//            if (data == null) return;
//
//            Map<String, Map<String, BankData.Level>> wrapper = new HashMap<>();
//            wrapper.put("levels", data.getLevels());
//
//            try (PreparedStatement ps = getConnection().prepareStatement(
//                    "INSERT INTO bank_data (priority, name, json) VALUES (?, ?, ?) " +
//                            "ON CONFLICT(name) DO UPDATE SET priority = EXCLUDED.priority, json = EXCLUDED.json"
//            )) {
//
//                ps.setInt(1, priority);
//                ps.setString(2, bankName);
//                ps.setString(3, gson.toJson(wrapper));
//
//                ps.executeUpdate();
//            }
//
//            synchronized (this) {
//                bankDataCache.remove(bankName);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void saveBanksBatch(List<String> bankNames, List<BankData> bankDataList, List<Integer> priorities) {
//        if (bankNames == null || bankDataList == null || priorities == null || bankNames.isEmpty()) return;
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "INSERT INTO bank_data (priority, name, json) VALUES (?, ?, ?) " +
//                        "ON CONFLICT(name) DO UPDATE SET priority = EXCLUDED.priority, json = EXCLUDED.json"
//        )) {
//            int count = Math.min(bankNames.size(), bankDataList.size());
//            for (int i = 0; i < count; i++) {
//                String bankName = bankNames.get(i);
//                BankData data = bankDataList.get(i);
//                int priority = priorities.size() > i && priorities.get(i) != null ? priorities.get(i) : (i + 1);
//                if (bankName == null || bankName.isEmpty() || data == null) {
//                    continue;
//                }
//
//                Map<String, Map<String, BankData.Level>> wrapper = new HashMap<>();
//                wrapper.put("levels", data.getLevels());
//
//                ps.setInt(1, priority);
//                ps.setString(2, bankName);
//                ps.setString(3, gson.toJson(wrapper));
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public Map<String, BankData> loadBankData(String bankName) {
//        synchronized (this) {
//            Map<String, BankData> cached = bankDataCache.get(bankName);
//            if (cached != null) {
//                return new HashMap<>(cached);
//            }
//        }
//
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT json FROM bank_data WHERE name=? ORDER BY priority ASC LIMIT 1"
//        )) {
//            ps.setString(1, bankName);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    String json = rs.getString("json");
//                    Type type = new TypeToken<Map<String, Map<String, BankData.Level>>>(){}.getType();
//                    Map<String, Map<String, BankData.Level>> raw = gson.fromJson(json, type);
//
//                    BankData bankData = new BankData(bankName, raw.get("levels"));
//                    Map<String, BankData> result = new HashMap<>();
//                    result.put(bankName, bankData);
//
//                    synchronized (this) {
//                        bankDataCache.put(bankName, new HashMap<>(result));
//                    }
//
//                    return result;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    public List<String> getAllBankNames() {
//        List<String> banks = new ArrayList<>();
//
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT name FROM bank_data ORDER BY priority ASC"
//        );
//             ResultSet rs = ps.executeQuery()) {
//
//            while (rs.next()) {
//                String bankName = rs.getString("name");
//                if (bankName != null && !bankName.isEmpty()) {
//                    banks.add(bankName);
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return banks;
//    }
//
//    // ---------------- ACCRUED INTERESTS ----------------
//    @Override
//    public void saveAccruedInterestData(int value) {
//        try {
//            Map<String, Integer> data = new HashMap<>();
//            data.put("accrued_interest", value);
//
//            try (PreparedStatement ps = getConnection().prepareStatement(
//                    "INSERT INTO interests_data (type, json) VALUES (?, ?) " +
//                            "ON CONFLICT(type) DO UPDATE SET json = EXCLUDED.json"
//            )) {
//                ps.setString(1, "global");
//                ps.setString(2, gson.toJson(data));
//                ps.executeUpdate();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public int loadAccruedInterestData() {
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT json FROM interests_data WHERE type=?"
//        )) {
//            ps.setString(1, "global");
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    Map<String, Integer> data = gson.fromJson(rs.getString("json"), new TypeToken<Map<String, Integer>>(){}.getType());
//                    return data.getOrDefault("accrued_interest", 0);
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    // ---------------- TRANSACTIONS ----------------
//    @Override
//    public void saveTransaction(TransactionData transaction) {
//        try {
//            initialize();
//            save(transaction);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void saveTransactionsBatch(List<TransactionData> transactions) {
//        if (transactions == null || transactions.isEmpty()) return;
//
//        try {
//            initialize();
//            try (PreparedStatement insert = getConnection().prepareStatement(
//                    "INSERT INTO transactions (id,player_uuid,type,amount,description,context,timestamp) " +
//                            "VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING"
//            )) {
//                for (TransactionData transaction : transactions) {
//                    if (transaction == null) continue;
//                    insert.setString(1, transaction.getId());
//                    insert.setString(2, transaction.getPlayerUuid());
//                    insert.setString(3, transaction.getType());
//                    insert.setInt(4, transaction.getAmount());
//                    insert.setString(5, transaction.getDescription());
//                    insert.setString(6, transaction.getContext());
//                    insert.setLong(7, transaction.getTimestamp());
//                    insert.addBatch();
//                }
//                insert.executeBatch();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public List<TransactionData> getAllTransactions() {
//        List<TransactionData> transactions = new ArrayList<>();
//
//        try {
//            initialize();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try (PreparedStatement select = getConnection().prepareStatement(
//                "SELECT id, player_uuid, type, amount, description, context, timestamp FROM transactions"
//        );
//             ResultSet rs = select.executeQuery()) {
//            while (rs.next()) {
//                transactions.add(new TransactionData(
//                        rs.getString("id"),
//                        rs.getString("player_uuid"),
//                        rs.getString("type"),
//                        rs.getInt("amount"),
//                        rs.getString("description"),
//                        rs.getString("context"),
//                        rs.getLong("timestamp")
//                ));
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return transactions;
//    }
//
//    @Override
//    public void initialize() throws Exception {
//        prepareTables();
//    }
//
//    @Override
//    public void save(TransactionData transaction) throws Exception {
//        try (PreparedStatement insert = getConnection().prepareStatement(
//                "INSERT INTO transactions (id,player_uuid,type,amount,description,context,timestamp) " +
//                        "VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING"
//        )) {
//            insert.setString(1, transaction.getId());
//            insert.setString(2, transaction.getPlayerUuid());
//            insert.setString(3, transaction.getType());
//            insert.setInt(4, transaction.getAmount());
//            insert.setString(5, transaction.getDescription());
//            insert.setString(6, transaction.getContext());
//            insert.setLong(7, transaction.getTimestamp());
//            insert.executeUpdate();
//        }
//    }
//
//    @Override
//    public List<TransactionData> findByPlayer(String playerUuid, int limit, int offset) throws Exception {
//        List<TransactionData> transactions = new ArrayList<>();
//
//        try (PreparedStatement ps = getConnection().prepareStatement(
//                "SELECT id, player_uuid, type, amount, description, context, timestamp " +
//                        "FROM transactions " +
//                        "WHERE player_uuid = ? " +
//                        "ORDER BY timestamp DESC " +
//                        "LIMIT ? OFFSET ?"
//        )) {
//            ps.setString(1, playerUuid);
//            ps.setInt(2, limit);
//            ps.setInt(3, offset);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) {
//                    transactions.add(new TransactionData(
//                            rs.getString("id"),
//                            rs.getString("player_uuid"),
//                            rs.getString("type"),
//                            rs.getInt("amount"),
//                            rs.getString("description"),
//                            rs.getString("context"),
//                            rs.getLong("timestamp")
//                    ));
//                }
//            }
//        }
//
//        return transactions;
//    }
//
//    // ---------------- BEFORE-MIGRATION DATA ----------------
//    @Override
//    public void clearAllData() {
//        try {
//            Connection conn = getConnection();
//
//            try (Statement st = conn.createStatement()) {
//                st.executeUpdate("DELETE FROM player_data");
//                st.executeUpdate("DELETE FROM bank_data");
//                st.executeUpdate("DELETE FROM interests_data");
//                st.executeUpdate("DELETE FROM transactions");
//            }
//
//            synchronized (this) {
//                playerDataCache.clear();
//                bankDataCache.clear();
//                playerNameCache.clear();
//                cachedPlayerUUIDs = null;
//                invalidateTopCache();
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ---------------- BACKUP ----------------
//    @Override
//    public void backup() throws Exception {
//        String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
//        Connection conn = getConnection();
//
//        String playerBackupTable = "player_data_backup_" + date;
//        String bankBackupTable = "bank_data_backup_" + date;
//        String interestsBackupTable = "interests_data_backup_" + date;
//        String transactionsBackupTable = "transactions_backup_" + date;
//
//        conn.createStatement().executeUpdate(
//                "CREATE TABLE IF NOT EXISTS " + playerBackupTable + " (LIKE player_data INCLUDING ALL);"
//        );
//        conn.createStatement().executeUpdate(
//                "CREATE TABLE IF NOT EXISTS " + bankBackupTable + " (LIKE bank_data INCLUDING ALL);"
//        );
//        conn.createStatement().executeUpdate(
//                "CREATE TABLE IF NOT EXISTS " + interestsBackupTable + " (LIKE interests_data INCLUDING ALL);"
//        );
//        conn.createStatement().executeUpdate(
//                "CREATE TABLE IF NOT EXISTS " + transactionsBackupTable + " (LIKE transactions INCLUDING ALL);"
//        );
//
//        conn.createStatement().executeUpdate(
//                "INSERT INTO " + playerBackupTable + " (uuid, json) " +
//                        "SELECT uuid, json FROM player_data " +
//                        "ON CONFLICT (uuid) DO UPDATE SET json = EXCLUDED.json;"
//        );
//
//        conn.createStatement().executeUpdate(
//                "INSERT INTO " + bankBackupTable + " (priority, name, json) " +
//                        "SELECT priority, name, json FROM bank_data " +
//                        "ON CONFLICT (name) DO UPDATE SET priority = EXCLUDED.priority, json = EXCLUDED.json;"
//        );
//
//        conn.createStatement().executeUpdate(
//                "INSERT INTO " + interestsBackupTable + " (type, json) " +
//                        "SELECT type, json FROM interests_data " +
//                        "ON CONFLICT (type) DO UPDATE SET json = EXCLUDED.json;"
//        );
//
//        conn.createStatement().executeUpdate(
//                "INSERT INTO " + transactionsBackupTable + " (id, player_uuid, type, amount, description, context, timestamp) " +
//                        "SELECT id, player_uuid, type, amount, description, context, timestamp FROM transactions " +
//                        "ON CONFLICT (id) DO UPDATE SET " +
//                        "player_uuid = EXCLUDED.player_uuid, " +
//                        "type = EXCLUDED.type, " +
//                        "amount = EXCLUDED.amount, " +
//                        "description = EXCLUDED.description, " +
//                        "context = EXCLUDED.context, " +
//                        "timestamp = EXCLUDED.timestamp;"
//        );
//    }
//
//    public void beginBulkOperation() throws SQLException {
//        Connection conn = getConnection();
//        bulkPreviousAutoCommit = conn.getAutoCommit();
//        if (bulkPreviousAutoCommit) {
//            conn.setAutoCommit(false);
//        }
//        bulkMode = true;
//    }
//
//    public void endBulkOperation(boolean commit) {
//        if (!bulkMode) return;
//        try {
//            Connection conn = getConnection();
//            if (commit) {
//                conn.commit();
//            } else {
//                conn.rollback();
//            }
//            if (bulkPreviousAutoCommit != null) {
//                conn.setAutoCommit(bulkPreviousAutoCommit);
//            } else {
//                conn.setAutoCommit(true);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            bulkMode = false;
//            bulkPreviousAutoCommit = null;
//        }
//    }
//
//    private synchronized void invalidateTopCache() {
//        topCacheExpiresAt = 0L;
//        cachedTopRows = Collections.emptyList();
//    }
//}
