package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.*;
import com.Guayand0.dbmigration.*;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class MigrateSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public MigrateSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = "bank.migrate.usage-admin";

        if (args.length < 2) {
            sendMessage.send(sender, usageKey, ph); // Mensaje
            return true;
        }

        try {
            // ----------------- Confirm -----------------
            if ("confirm".equalsIgnoreCase(args[1])) {
                if (!plugin.getPendingMigrations().containsKey(player.getUniqueId())) {
                    sendMessage.send(sender, "bank.migrate.no-pending", ph); // Mensaje
                    return true;
                }
                PendingMigration pending = plugin.getPendingMigrations().remove(player.getUniqueId());

                StorageManager sm = plugin.getStorageManager();
                DataStorage fromStorage = sm.get(pending.from);
                DataStorage toStorage = sm.get(pending.to);

                // Si destino es JSON y no está iniciado → iniciarlo
                if (pending.to == StorageType.JSON && toStorage == null) {
                    JsonStorage json = new JsonStorage(plugin.getDataFolder());
                    sm.register(StorageType.JSON, json);
                    toStorage = json;
                }

                // Si destino es MYSQL y no está iniciado → iniciarlo
                if (pending.to == StorageType.MYSQL && toStorage == null) {
                    String mysqlUri = GV.getString(plugin, "bank.data.mysql-uri");
                    String host = GV.getString(plugin, "bank.data.host");
                    int port = GV.getInt(plugin, "bank.data.port", 3306);
                    String database = GV.getString(plugin, "bank.data.database");
                    String user = GV.getString(plugin, "bank.data.user");
                    String password = GV.getString(plugin, "bank.data.password");
                    String params = GV.getString(plugin, "bank.data.connection_params");

                    // Validar datos MySQL
                    if ((mysqlUri == null || mysqlUri.isEmpty()) && (host == null || host.isEmpty() || database == null || database.isEmpty() || user == null || user.isEmpty() || password == null)) {
                        ph.put("%MIGRATEDTO%", pending.to.name().toUpperCase());
                        sendMessage.send(sender, "bank.migrate.failed-credentials", ph); // Mensaje
                        return true;
                    }

                    try {
                        MySQLStorage mysql;
                        if (mysqlUri != null && !mysqlUri.isEmpty()) {
                            mysql = new MySQLStorage(mysqlUri);
                        } else {
                            mysql = new MySQLStorage(host, port, database, user, password, params);
                        }

                        mysql.prepareTables();
                        sm.register(StorageType.MYSQL, mysql);
                        toStorage = mysql;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(ex, plugin)));
                        ph.put("%MIGRATEDTO%", pending.to.name().toUpperCase());
                        sendMessage.send(sender, "bank.migrate.failed-connection", ph); // Mensaje
                        return true;
                    }
                }

                // Si destino es SQLITE y no está iniciado → iniciarlo
                if (pending.to == StorageType.SQLITE && toStorage == null) {
                    try {
                        SQLiteStorage sqlite = new SQLiteStorage(plugin.getDataFolder());
                        sqlite.prepareTables();
                        sm.register(StorageType.SQLITE, sqlite);
                        toStorage = sqlite;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(ex, plugin)));
                        sendMessage.send(sender, "bank.migrate.failed-connection", ph); // Mensaje
                        return true;
                    }
                }

                /*// Si destino es POSTGRESQL y no está iniciado → iniciarlo
                if (pending.to == StorageType.POSTGRESQL && toStorage == null) {
                    String pgUri = GV.getString(plugin, "bank.data.postgresql-uri");
                    String host = GV.getString(plugin, "bank.data.host");
                    int port = GV.getInt(plugin, "bank.data.port", 5432);
                    String database = GV.getString(plugin, "bank.data.database");
                    String user = GV.getString(plugin, "bank.data.user");
                    String password = GV.getString(plugin, "bank.data.password");
                    String params = GV.getString(plugin, "bank.data.connection_params");

                    // Validar datos PostgreSQL
                    if ((pgUri == null || pgUri.isEmpty()) && (host == null || host.isEmpty() || database == null || database.isEmpty() || user == null || user.isEmpty() || password == null)) {
                        sendMessage.send(sender, "bank.migrate.failed-credentials", ph); // Mensaje
                        return true;
                    }

                    try {
                        PostgreSQLStorage pg;
                        if (pgUri != null && !pgUri.isEmpty()) {
                            pg = new PostgreSQLStorage(pgUri);
                        } else {
                            pg = new PostgreSQLStorage(host, port, database, user, password, params);
                        }

                        pg.prepareTables();
                        sm.register(StorageType.POSTGRESQL, pg);
                        toStorage = pg;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(ex, plugin)));
                        sendMessage.send(sender, "bank.migrate.failed-connection", ph); // Mensaje
                        return true;
                    }
                }

                // Si destino es MONGODB y no está iniciado → iniciarlo
                if (pending.to == StorageType.MONGODB && toStorage == null) {
                    String mongoUri = GV.getString(plugin, "bank.data.mongodb-uri");
                    String host = GV.getString(plugin, "bank.data.host");
                    int port = GV.getInt(plugin, "bank.data.port", 27017);
                    String database = GV.getString(plugin, "bank.data.database");
                    String user = GV.getString(plugin, "bank.data.user");
                    String password = GV.getString(plugin, "bank.data.password");
                    String params = GV.getString(plugin, "bank.data.connection_params");

                    try {
                        MongoDBStorage mongo;
                        if (mongoUri != null && !mongoUri.isEmpty()) {
                            mongo = new MongoDBStorage(mongoUri);
                        } else {
                            mongo = new MongoDBStorage(host, port, database, user, password, params);
                        }
                        mongo.prepareCollections();
                        sm.register(StorageType.MONGODB, mongo);
                        toStorage = mongo;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(ex, plugin)));
                        sendMessage.send(sender, "bank.migrate.failed-connection", ph); // Mensaje
                        return true;
                    }
                }*/

                if (fromStorage == null || toStorage == null) {
                    throw new Exception("Error while connecting to database");
                }

//                // Hacer backup si fue indicado
//                if (pending.backup) {
//                    try {
//                        fromStorage.backup();
//                        sendMessage.send(sender, "messages.backup.success", ph); // Mensaje
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        sendMessage.send(sender, "messages.backup.failed", ph); // Mensaje
//                        if (plugin.getPendingMigrations().containsKey(player.getUniqueId())) {
//                            plugin.getPendingMigrations().remove(player.getUniqueId());
//                            // Mensaje al jugador si sigue en línea
//                            Player p = Bukkit.getPlayer(player.getUniqueId());
//                            if (p != null && p.isOnline()) {
//                                sendMessage.send(p, "messages.migrate.confirm-expired", ph); // Mensaje
//                            }
//                        }
//                        return true;
//                    }
//                }
                DataStorage finalToStorage = toStorage;
                DataStorage finalFromStorage = fromStorage;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {

                            long start = System.currentTimeMillis();
                            finalToStorage.clearAllData();

                            MigrationResult result = DataMigrator.migrate(finalFromStorage, finalToStorage);
                            long elapsedMs = System.currentTimeMillis() - start;

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                ph.put("%MIGRATEDFROM%", pending.from.name().toUpperCase());
                                ph.put("%MIGRATEDTO%", pending.to.name().toUpperCase());
                                ph.put("%MIGRATEDPLAYERS%", String.valueOf(result.getPlayersMigrated()));
                                ph.put("%MIGRATEDBANKS%", String.valueOf(result.getBanksMigrated()));
                                ph.put("%MIGRATEDINTERESTS%", String.valueOf(result.getAccruedInterest()));
                                ph.put("%MIGRATEDTRANSACTIONS%", String.valueOf(result.getTransactionsMigrated()));
                                ph.put("%MIGRATIONTIME%", elapsedMs + "ms");

                                sendMessage.send(sender, "bank.migrate.success", ph);
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            ph.put("%MIGRATIONERROR%", e.getMessage());
                            sendMessage.send(sender, "bank.migrate.failed", ph);
                            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                        }
                    }
                }.runTaskAsynchronously(plugin);

                return true;
            }

            String migration = args[1];
            String[] parts = migration.split("-");

            if (!migration.matches("^[A-Z]+-[A-Z]+$") || parts.length != 2) {
                sendMessage.send(sender, usageKey, ph); // Mensaje
                return true;
            }

            StorageType from = StorageType.fromString(parts[0]);
            StorageType to = StorageType.fromString(parts[1]);

            if (from == null || to == null || from == to) {
                sendMessage.send(sender, "bank.migrate.not-supported", ph); // Mensaje
                return true;
            }

            if (from.name().equals(to.name())) {
                sendMessage.send(sender, "bank.migrate.same-storage-type", ph); // Mensaje
                return true;
            }

            if (!from.name().equals(GV.getString(plugin, "bank.data.type").toUpperCase())) {
                ph.put("%MIGRATEDFROM%", from.name().toUpperCase());
                sendMessage.send(sender, "bank.migrate.not-current-data-type", ph); // Mensaje
                return true;
            }

            // Guardar migración pendiente para confirmar
            /*boolean backup = args.length > 2 && "backup".equalsIgnoreCase(args[2]);
            PendingMigration pendingMigration = new PendingMigration(from, to, backup);*/
            PendingMigration pendingMigration = new PendingMigration(from, to, false);
            plugin.getPendingMigrations().put(player.getUniqueId(), pendingMigration);
            sendMessage.send(sender, "bank.migrate.confirm-needed", ph); // Mensaje

            // Programar eliminación automática después de 10 segundos (200 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    PendingMigration current = plugin.getPendingMigrations().get(player.getUniqueId());
                    if (current != null && current == pendingMigration) {
                        plugin.getPendingMigrations().remove(player.getUniqueId());
                        // Mensaje al jugador si sigue en línea
                        Player p = Bukkit.getPlayer(player.getUniqueId());
                        if (p != null && p.isOnline()) {
                            sendMessage.send(p, "bank.migrate.confirm-expired", ph); // Mensaje
                        }
                    }
                }
            }.runTaskLater(plugin, 200L); // 200 ticks = 10 segundos

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph); // Mensaje
        }

        return true;
    }
}
