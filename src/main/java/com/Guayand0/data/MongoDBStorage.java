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
//import com.mongodb.client.FindIterable;
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//import com.mongodb.client.model.Filters;
//import com.mongodb.client.model.IndexOptions;
//import com.mongodb.client.model.Indexes;
//import com.mongodb.client.model.ReplaceOptions;
//import com.mongodb.client.model.Sorts;
//import org.bson.Document;
//
//import java.lang.reflect.Type;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//public class MongoDBStorage implements DataStorage, TransactionStorage {
//
//    private static final long TOP_CACHE_TTL_MS = 5000L;
//
//    private final String connectionUri;
//    private final String host;
//    private final int port;
//    private final String database;
//    private final String user;
//    private final String pass;
//    private final String params;
//
//    private MongoClient client;
//    private MongoDatabase db;
//    private boolean prepared = false;
//
//    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
//    private final PlayerUtils PU = new PlayerUtils();
//
//    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
//    private final Map<String, Map<String, BankData>> bankDataCache = new HashMap<>();
//    private final Map<UUID, String> playerNameCache = new HashMap<>();
//    private List<UUID> cachedPlayerUUIDs;
//    private long topCacheExpiresAt = 0L;
//    private List<List<String>> cachedTopRows = Collections.emptyList();
//
//    public MongoDBStorage(String host, int port, String database, String user, String pass, String params) {
//        this.connectionUri = null;
//        this.host = host;
//        this.port = port;
//        this.database = database;
//        this.user = user;
//        this.pass = pass;
//        this.params = params;
//    }
//
//    public MongoDBStorage(String connectionUri) {
//        this.connectionUri = connectionUri;
//        this.host = null;
//        this.port = 0;
//        this.database = null;
//        this.user = null;
//        this.pass = null;
//        this.params = null;
//    }
//
//    private synchronized void connect() {
//        if (connectionUri != null && !connectionUri.isEmpty()) {
//            String dbName = "minebank";
//            client = MongoClients.create(connectionUri);
//            db = client.getDatabase(dbName);
//            return;
//        }
//
//        String credentials = "";
//        if (user != null && !user.isEmpty()) {
//            credentials = user;
//            if (pass != null && !pass.isEmpty()) {
//                credentials += ":" + pass;
//            }
//            credentials += "@";
//        }
//
//        String suffix = "";
//        if (params != null && !params.isEmpty()) {
//            suffix = params.startsWith("?") ? params : "?" + params;
//        }
//
//        String mongoUri = "mongodb://" + credentials + host + ":" + port + "/" + database + suffix;
//        client = MongoClients.create(mongoUri);
//        db = client.getDatabase(database);
//    }
//
//    private synchronized void ensureConnected() {
//        if (client == null) {
//            connect();
//        }
//    }
//
//    public synchronized void prepareCollections() {
//        ensureConnected();
//        if (prepared) return;
//
//        MongoCollection<Document> bank = getBankCollection();
//        MongoCollection<Document> transactions = getTransactionsCollection();
//
//        bank.createIndex(Indexes.ascending("priority"), new IndexOptions().unique(true));
//        transactions.createIndex(Indexes.compoundIndex(
//                Indexes.ascending("player_uuid"),
//                Indexes.descending("timestamp")
//        ));
//
//        prepared = true;
//    }
//
//    private MongoCollection<Document> getPlayerCollection() {
//        ensureConnected();
//        return db.getCollection("player_data");
//    }
//
//    private MongoCollection<Document> getBankCollection() {
//        ensureConnected();
//        return db.getCollection("bank_data");
//    }
//
//    private MongoCollection<Document> getInterestsCollection() {
//        ensureConnected();
//        return db.getCollection("interests_data");
//    }
//
//    private MongoCollection<Document> getTransactionsCollection() {
//        ensureConnected();
//        return db.getCollection("transactions");
//    }
//
//    // ---------------- PLAYER DATA ----------------
//    @Override
//    public void savePlayerData(UUID uuid, PlayerData data) {
//        prepareCollections();
//        Document doc = new Document("_id", uuid.toString())
//                .append("json", gson.toJson(data));
//
//        getPlayerCollection().replaceOne(
//                Filters.eq("_id", uuid.toString()),
//                doc,
//                new ReplaceOptions().upsert(true)
//        );
//
//        synchronized (this) {
//            playerDataCache.put(uuid, data);
//            cachedPlayerUUIDs = null;
//            invalidateTopCache();
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
//        prepareCollections();
//        Document doc = getPlayerCollection().find(Filters.eq("_id", uuid.toString())).first();
//        if (doc == null) return null;
//
//        PlayerData data = gson.fromJson(doc.getString("json"), PlayerData.class);
//        synchronized (this) {
//            playerDataCache.put(uuid, data);
//        }
//        return data;
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
//        prepareCollections();
//        List<UUID> uuids = new ArrayList<>();
//        FindIterable<Document> docs = getPlayerCollection()
//                .find()
//                .projection(new Document("_id", 1));
//
//        for (Document doc : docs) {
//            try {
//                uuids.add(UUID.fromString(doc.getString("_id")));
//            } catch (Exception ignored) {}
//        }
//
//        synchronized (this) {
//            cachedPlayerUUIDs = new ArrayList<>(uuids);
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
//        prepareCollections();
//        List<List<String>> result = new ArrayList<>();
//        Map<UUID, PlayerData> players = new HashMap<>();
//
//        for (Document doc : getPlayerCollection().find()) {
//            String uuidStr = doc.getString("_id");
//            if (uuidStr == null) continue;
//
//            try {
//                UUID uuid = UUID.fromString(uuidStr);
//                PlayerData data = gson.fromJson(doc.getString("json"), PlayerData.class);
//                if (data != null && data.getBank() != null) {
//                    players.put(uuid, data);
//                }
//            } catch (Exception ignored) {}
//        }
//
//        List<Map.Entry<UUID, PlayerData>> sorted = new ArrayList<>(players.entrySet());
//
//        sorted.sort((a, b) -> Integer.compare(
//                b.getValue().getBank().getBalance(),
//                a.getValue().getBank().getBalance()
//        ));
//
//        int pos = 0;
//        for (Map.Entry<UUID, PlayerData> entry : sorted) {
//            if (pos >= amount) break;
//
//            UUID uuid = entry.getKey();
//            String playerName;
//            synchronized (this) {
//                playerName = playerNameCache.get(uuid);
//            }
//
//            if (playerName == null) {
//                playerName = PU.getNameFromUUID(uuid);
//                if (playerName != null) {
//                    synchronized (this) {
//                        playerNameCache.put(uuid, playerName);
//                    }
//                }
//            }
//
//            PlayerData data = entry.getValue();
//
//            List<String> row = new ArrayList<>();
//            row.add(playerName);
//            row.add(data.getBank().getName());
//            row.add(String.valueOf(data.getBank().getLevel()));
//            row.add(String.valueOf(data.getBank().getBalance()));
//
//            result.add(row);
//            pos++;
//        }
//
//        synchronized (this) {
//            cachedTopRows = new ArrayList<>(result);
//            topCacheExpiresAt = System.currentTimeMillis() + TOP_CACHE_TTL_MS;
//        }
//
//        return result;
//    }
//
//    // ---------------- BANK DATA ----------------
//    @Override
//    public void saveBankData(String bankName, Map<String, BankData> bankData, int priority) {
//        prepareCollections();
//        BankData data = bankData.get(bankName);
//        if (data == null) return;
//
//        Map<String, Map<String, BankData.Level>> wrapper = new HashMap<>();
//        wrapper.put("levels", data.getLevels());
//
//        Document doc = new Document("_id", bankName)
//                .append("priority", priority)
//                .append("json", gson.toJson(wrapper));
//
//        getBankCollection().replaceOne(
//                Filters.eq("_id", bankName),
//                doc,
//                new ReplaceOptions().upsert(true)
//        );
//
//        synchronized (this) {
//            bankDataCache.remove(bankName);
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
//        prepareCollections();
//        Document doc = getBankCollection().find(Filters.eq("_id", bankName)).first();
//        if (doc == null) return null;
//
//        String json = doc.getString("json");
//        Type type = new TypeToken<Map<String, Map<String, BankData.Level>>>(){}.getType();
//        Map<String, Map<String, BankData.Level>> raw = gson.fromJson(json, type);
//
//        BankData bankDataObj = new BankData(bankName, raw.get("levels"));
//        Map<String, BankData> result = new HashMap<>();
//        result.put(bankName, bankDataObj);
//
//        synchronized (this) {
//            bankDataCache.put(bankName, new HashMap<>(result));
//        }
//
//        return result;
//    }
//
//    @Override
//    public List<String> getAllBankNames() {
//        prepareCollections();
//        List<String> banks = new ArrayList<>();
//
//        FindIterable<Document> docs = getBankCollection()
//                .find()
//                .sort(Sorts.ascending("priority"))
//                .projection(new Document("_id", 1));
//
//        for (Document doc : docs) {
//            String name = doc.getString("_id");
//            if (name != null && !name.isEmpty()) {
//                banks.add(name);
//            }
//        }
//
//        return banks;
//    }
//
//    // ---------------- ACCRUED INTERESTS ----------------
//    @Override
//    public void saveAccruedInterestData(int value) {
//        prepareCollections();
//        Map<String, Integer> data = new HashMap<>();
//        data.put("accrued_interest", value);
//
//        Document doc = new Document("_id", "global")
//                .append("json", gson.toJson(data));
//
//        getInterestsCollection().replaceOne(
//                Filters.eq("_id", "global"),
//                doc,
//                new ReplaceOptions().upsert(true)
//        );
//    }
//
//    @Override
//    public int loadAccruedInterestData() {
//        prepareCollections();
//        Document doc = getInterestsCollection().find(Filters.eq("_id", "global")).first();
//        if (doc == null) return 0;
//
//        Map<String, Integer> data = gson.fromJson(
//                doc.getString("json"),
//                new TypeToken<Map<String, Integer>>(){}.getType()
//        );
//        return data.getOrDefault("accrued_interest", 0);
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
//    @Override
//    public List<TransactionData> getAllTransactions() {
//        prepareCollections();
//        List<TransactionData> transactions = new ArrayList<>();
//
//        for (Document doc : getTransactionsCollection().find()) {
//            Long ts = doc.getLong("timestamp");
//            transactions.add(new TransactionData(
//                    doc.getString("_id"),
//                    doc.getString("player_uuid"),
//                    doc.getString("type"),
//                    doc.getInteger("amount", 0),
//                    doc.getString("description"),
//                    doc.getString("context"),
//                    ts == null ? 0L : ts
//            ));
//        }
//
//        return transactions;
//    }
//
//    @Override
//    public void initialize() {
//        prepareCollections();
//    }
//
//    @Override
//    public void save(TransactionData transaction) {
//        prepareCollections();
//        Document doc = new Document("_id", transaction.getId())
//                .append("player_uuid", transaction.getPlayerUuid())
//                .append("type", transaction.getType())
//                .append("amount", transaction.getAmount())
//                .append("description", transaction.getDescription())
//                .append("context", transaction.getContext())
//                .append("timestamp", transaction.getTimestamp());
//
//        getTransactionsCollection().replaceOne(
//                Filters.eq("_id", transaction.getId()),
//                doc,
//                new ReplaceOptions().upsert(true)
//        );
//    }
//
//    @Override
//    public List<TransactionData> findByPlayer(String playerUuid, int limit, int offset) {
//        prepareCollections();
//        List<TransactionData> transactions = new ArrayList<>();
//
//        FindIterable<Document> docs = getTransactionsCollection()
//                .find(Filters.eq("player_uuid", playerUuid))
//                .sort(Sorts.descending("timestamp"))
//                .skip(offset)
//                .limit(limit);
//
//        for (Document doc : docs) {
//            Long ts = doc.getLong("timestamp");
//            transactions.add(new TransactionData(
//                    doc.getString("_id"),
//                    doc.getString("player_uuid"),
//                    doc.getString("type"),
//                    doc.getInteger("amount", 0),
//                    doc.getString("description"),
//                    doc.getString("context"),
//                    ts == null ? 0L : ts
//            ));
//        }
//
//        return transactions;
//    }
//
//    // ---------------- BEFORE-MIGRATION DATA ----------------
//    @Override
//    public void clearAllData() {
//        prepareCollections();
//        getPlayerCollection().deleteMany(new Document());
//        getBankCollection().deleteMany(new Document());
//        getInterestsCollection().deleteMany(new Document());
//        getTransactionsCollection().deleteMany(new Document());
//
//        synchronized (this) {
//            playerDataCache.clear();
//            bankDataCache.clear();
//            playerNameCache.clear();
//            cachedPlayerUUIDs = null;
//            invalidateTopCache();
//        }
//    }
//
//    // ---------------- BACKUP ----------------
//    @Override
//    public void backup() throws Exception {
//        prepareCollections();
//        String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
//
//        backupCollection(getPlayerCollection(), "player_data_backup_" + date);
//        backupCollection(getBankCollection(), "bank_data_backup_" + date);
//        backupCollection(getInterestsCollection(), "interests_data_backup_" + date);
//        backupCollection(getTransactionsCollection(), "transactions_backup_" + date);
//    }
//
//    private void backupCollection(MongoCollection<Document> source, String backupName) {
//        MongoCollection<Document> target = db.getCollection(backupName);
//        List<Document> docs = new ArrayList<>();
//        for (Document doc : source.find()) {
//            docs.add(new Document(doc));
//        }
//        if (!docs.isEmpty()) {
//            target.insertMany(docs);
//        }
//    }
//
//    private synchronized void invalidateTopCache() {
//        topCacheExpiresAt = 0L;
//        cachedTopRows = Collections.emptyList();
//    }
//}
