package com.Guayand0.data;

import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionData;
import com.Guayand0.data.transactions.TransactionStorage;
import com.Guayand0.zlib.PlayerUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class JsonStorage implements DataStorage, TransactionStorage {

    private final File folder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final PlayerUtils PU = new PlayerUtils();

    public JsonStorage(File folder) {
        this.folder = folder;
        if (!folder.exists()) folder.mkdirs();
    }


    // ---------------- PLAYER DATA ----------------
    @Override
    public void savePlayerData(UUID uuid, PlayerData data) {
        try {
            File file = new File(folder, "data/player_data/" + uuid.toString() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        try {
            File file = new File(folder, "data/player_data/" + uuid.toString() + ".json");
            if (!file.exists()) return null;

            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, PlayerData.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<UUID> getAllPlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        File folderPlayer = new File(folder, "data/player_data");
        File[] files = folderPlayer.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return uuids;

        for (File file : files) {
            try {
                uuids.add(UUID.fromString(file.getName().replace(".json", "")));
            } catch (Exception ignored) {}
        }
        return uuids;
    }

    @Override
    public List<String> getAllPlayerNames() {
        List<String> names = new ArrayList<>();
        PlayerUtils PU = new PlayerUtils();

        for (UUID uuid : getAllPlayerUUIDs()) {
            String name = PU.getNameFromUUID(uuid);
            if (name != null) {
                names.add(name);
            }
        }

        return names;
    }


    // ---------------- PLAYER TOP DATA ----------------
    @Override
    public List<List<String>> getTopPlayerBankData(int amount) {

        List<List<String>> result = new ArrayList<>();
        Map<UUID, PlayerData> players = new HashMap<>();

        try {
            File folderPlayer = new File(folder, "data/player_data");
            File[] files = folderPlayer.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) return result;

            for (File file : files) {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));

                try (FileReader reader = new FileReader(file)) {
                    PlayerData data = gson.fromJson(reader, PlayerData.class);
                    if (data != null && data.getBank() != null) {
                        players.put(uuid, data);
                    }
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
                String playerName = PU.getNameFromUUID(uuid);
                PlayerData data = entry.getValue();

                List<String> row = new ArrayList<>();
                row.add(playerName);
                row.add(data.getBank().getName());
                row.add(String.valueOf(data.getBank().getLevel()));
                row.add(String.valueOf(data.getBank().getBalance()));

                result.add(row);
                pos++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    // ---------------- BANK DATA ----------------
    public void saveBankData(String bankName, Map<String, BankData> bankData, int priority) {
        try {
            File folderBank = new File(folder, "data/bank_data");
            if (!folderBank.exists()) folderBank.mkdirs();

            File file = new File(folderBank, bankName + ".json");

            // Guardamos solo los levels
            Map<String, BankData.Level> levels = bankData.get(bankName).getLevels();

            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("levels", levels);

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(wrapper, writer);
            }

            File banksYml = new File(folder, "data/banks.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(banksYml);

            if (!config.isConfigurationSection("bank-priority")) {
                config.createSection("bank-priority");
            }

            // Insertamos la prioridad en la posición correspondiente
            config.set("bank-priority." + priority, bankName);

            // Guardar cambios
            config.save(banksYml);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, BankData> loadBankData(String bankName) {
        try {
            File folderBank = new File(folder, "data/bank_data");
            File file = new File(folderBank, bankName + ".json");

            if (!file.exists()) return null;

            Type type = new TypeToken<Map<String, Map<String, BankData.Level>>>() {}.getType();

            try (FileReader reader = new FileReader(file)) {
                Map<String, Map<String, BankData.Level>> raw = gson.fromJson(reader, type);
                BankData data = new BankData(bankName, raw.get("levels"));

                Map<String, BankData> result = new HashMap<>();
                result.put(bankName, data);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getAllBankNames() {
        List<String> banks = new ArrayList<>();
        File banksYml = new File(folder, "data/banks.yml");
        File folderBank = new File(folder, "data/bank_data");

        if (!banksYml.exists() || !folderBank.exists()) return banks;

        FileConfiguration config = YamlConfiguration.loadConfiguration(banksYml);
        ConfigurationSection section = config.getConfigurationSection("bank-priority");
        if (section == null) return banks;

        Set<String> added = new HashSet<>();
        int priority = 1;

        while (section.contains(String.valueOf(priority))) {

            String bankName = section.getString(String.valueOf(priority));
            File bankFile = new File(folderBank, bankName + ".json");

            if (bankName != null && bankFile.exists() && !added.contains(bankName)) {
                banks.add(bankName);
                added.add(bankName);
            }
            priority++;
        }

        return banks;
    }


    // ---------------- ACCRUED INTERESTS ----------------
    @Override
    public void saveAccruedInterestData(int value) {
        try {
            File file = new File(folder, "data/interests_data.json");

            Map<String, Integer> data = new HashMap<>();
            data.put("accrued_interest", value);

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int loadAccruedInterestData() {
        try {
            File file = new File(folder, "data/interests_data.json");

            if (!file.exists()) return 0;

            try (FileReader reader = new FileReader(file)) {
                Map<String, Integer> data = gson.fromJson(reader, new TypeToken<Map<String, Integer>>(){}.getType());
                return data.getOrDefault("accrued_interest", 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ---------------- TRANSACTIONS ----------------
    @Override
    public void saveTransaction(TransactionData transaction) {
        try {
            initialize();
            save(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TransactionData> getAllTransactions() {
        List<TransactionData> transactions = new ArrayList<>();
        File sqliteFile = new File(folder, "data/transactions.db");
        if (!sqliteFile.exists()) {
            return transactions;
        }

        try (Connection sqlite = getTransactionConnection()) {
            initialize();
            try (PreparedStatement select = sqlite.prepareStatement(
                    "SELECT id, player_uuid, type, amount, description, context, timestamp FROM transactions"
            );
                 ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new TransactionData(
                            rs.getString("id"),
                            rs.getString("player_uuid"),
                            rs.getString("type"),
                            rs.getInt("amount"),
                            rs.getString("description"),
                            rs.getString("context"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return transactions;
    }

    @Override
    public void initialize() throws Exception {
        try (Connection connection = getTransactionConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS transactions (" +
                             "id TEXT PRIMARY KEY," +
                             "player_uuid TEXT NOT NULL," +
                             "type TEXT NOT NULL," +
                             "amount INTEGER NOT NULL," +
                             "description TEXT NOT NULL," +
                             "context TEXT NOT NULL," +
                             "timestamp INTEGER NOT NULL" +
                             ")"
             )) {
            ps.executeUpdate();
        }

        try (Connection connection = getTransactionConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "CREATE INDEX IF NOT EXISTS idx_transactions_player_ts ON transactions (player_uuid, timestamp DESC)"
             )) {
            ps.executeUpdate();
        }
    }

    @Override
    public void save(TransactionData transaction) throws Exception {
        try (Connection connection = getTransactionConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT OR IGNORE INTO transactions (id,player_uuid,type,amount,description,context,timestamp) VALUES(?,?,?,?,?,?,?)"
             )) {
            ps.setString(1, transaction.getId());
            ps.setString(2, transaction.getPlayerUuid());
            ps.setString(3, transaction.getType());
            ps.setInt(4, transaction.getAmount());
            ps.setString(5, transaction.getDescription());
            ps.setString(6, transaction.getContext());
            ps.setLong(7, transaction.getTimestamp());
            ps.executeUpdate();
        }
    }

    @Override
    public List<TransactionData> findByPlayer(String playerUuid, int limit, int offset) throws Exception {
        List<TransactionData> transactions = new ArrayList<>();

        try (Connection connection = getTransactionConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT id, player_uuid, type, amount, description, context, timestamp " +
                             "FROM transactions " +
                             "WHERE player_uuid = ? " +
                             "ORDER BY timestamp DESC " +
                             "LIMIT ? OFFSET ?"
             )) {
            ps.setString(1, playerUuid);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new TransactionData(
                            rs.getString("id"),
                            rs.getString("player_uuid"),
                            rs.getString("type"),
                            rs.getInt("amount"),
                            rs.getString("description"),
                            rs.getString("context"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        }

        return transactions;
    }

    private Connection getTransactionConnection() throws Exception {
        File sqliteFile = new File(folder, "data/transactions.db");
        File parent = sqliteFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
    }

    // ---------------- BEFORE-MIGRATION DATA ----------------
    @Override
    public void clearAllData() {
        try {
            // Borrar archivos de jugadores
            File folderPlayer = new File(folder, "data/player_data");
            if (folderPlayer.exists() && folderPlayer.isDirectory()) {
                for (File file : folderPlayer.listFiles((dir, name) -> name.endsWith(".json"))) {
                    file.delete();
                }
            }

            // Borrar archivos de bancos
            File folderBank = new File(folder, "data/bank_data");
            if (folderBank.exists() && folderBank.isDirectory()) {
                for (File file : folderBank.listFiles((dir, name) -> name.endsWith(".json"))) {
                    file.delete();
                }
            }

            // Borrar banks.yml
            File banksYml = new File(folder, "data/banks.yml");
            if (banksYml.exists()) {
                banksYml.delete();
            }

            // Borrar intereses
            File interestsFile = new File(folder, "data/interests_data.json");
            if (interestsFile.exists()) {
                interestsFile.delete();
            }

            // Borrar transacciones
            File transactionsFile = new File(folder, "data/transactions.db");
            if (transactionsFile.exists()) {
                transactionsFile.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ---------------- BACKUP ----------------
    @Override
    public void backup() throws Exception {

        // Fecha para el nombre del backup
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // Carpeta backups/
        File backupsFolder = new File(folder, "backups");
        if (!backupsFolder.exists()) backupsFolder.mkdirs();

        // Carpeta del backup actual
        File backupRoot = new File(backupsFolder, date);
        backupRoot.mkdirs();

        // Copiar el contenido de MineBank
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {

            // No copiar la carpeta backups para evitar copiar backups dentro de backups
            if (file.getName().equals("backups")) continue;

            copyFileOrDir(file, new File(backupRoot, file.getName()));
        }
    }

    /**
     * Copia un archivo o carpeta de forma recursiva usando un buffer de 4096 bytes
     */
    private void copyFileOrDir(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File f : files) {
                    copyFileOrDir(f, new File(target, f.getName()));
                }
            }
        } else {
            target.getParentFile().mkdirs();
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }
}
