package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.MySQLStorage;
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

        if (args.length < 2) {
            sendMessage.send(sender, "messages.migrate.usage", ph); // Mensaje
            return true;
        }

        try {
            // ----------------- Confirm -----------------
            if ("confirm".equalsIgnoreCase(args[1])) {
                if (!plugin.getPendingMigrations().containsKey(player.getUniqueId())) {
                    sendMessage.send(sender, "messages.migrate.no-pending", ph); // Mensaje
                    return true;
                }
                PendingMigration pending = plugin.getPendingMigrations().remove(player.getUniqueId());

                StorageManager sm = plugin.getStorageManager();
                DataStorage fromStorage = sm.get(pending.from);
                DataStorage toStorage = sm.get(pending.to);

                // Si destino es MYSQL y no está iniciado → iniciarlo
                if (pending.to == StorageType.MYSQL && toStorage == null) {
                    String host = GV.getString(plugin, "bank.data.host");
                    int port = GV.getInt(plugin, "bank.data.port", 3306);
                    String database = GV.getString(plugin, "bank.data.database");
                    String user = GV.getString(plugin, "bank.data.user");
                    String password = GV.getString(plugin, "bank.data.password");
                    String params = GV.getString(plugin, "bank.data.connection_params");

                    // Validar datos MySQL
                    if (host == null || host.isEmpty() || database == null || database.isEmpty() || user == null || user.isEmpty() || password == null) {
                        sendMessage.send(sender, "messages.migrate.failed-credentials", ph); // Mensaje
                        return true;
                    }

                    try {
                        MySQLStorage mysql = new MySQLStorage(host, port, database, user, password, params);

                        mysql.prepareTables();
                        sm.register(StorageType.MYSQL, mysql);
                        toStorage = mysql;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(ex, plugin)));
                        sendMessage.send(sender, "messages.migrate.failed-connection", ph); // Mensaje
                        return true;
                    }
                }

                if (fromStorage == null || toStorage == null) {
                    sendMessage.send(sender, "messages.migrate.failed", ph); // Mensaje
                    return true;
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

                try {
                    // Limpiar destino antes de migrar
                    toStorage.clearAllData();

                    MigrationResult result = DataMigrator.migrate(fromStorage, toStorage);
                    ph.put("%MIGRATEDFROM%", pending.from.name().toUpperCase());
                    ph.put("%MIGRATEDTO%", pending.to.name().toUpperCase());
                    ph.put("%MIGRATEDPLAYERS%", String.valueOf(result.getPlayersMigrated()));
                    ph.put("%MIGRATEDBANKS%", String.valueOf(result.getBanksMigrated()));
                    ph.put("%MIGRATEDINTERESTS%", String.valueOf(result.getAccruedInterest()));
                    ph.put("%MIGRATEDTRANSACTIONS%", String.valueOf(result.getTransactionsMigrated()));
                    sendMessage.send(sender, "messages.migrate.success", ph); // Mensaje

                } catch (Exception e) {
                    e.printStackTrace();
                    if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                    ph.put("%migrationError%", e.getMessage());
                    sendMessage.send(sender, "messages.migrate.failed", ph); // Mensaje
                }

                return true;
            }

            String migration = args[1];
            String[] parts = migration.split("-");

            if (!migration.matches("^[A-Z]+-[A-Z]+$") || parts.length != 2) {
                sendMessage.send(sender, "messages.migrate.usage", ph); // Mensaje
                return true;
            }

            StorageType from = StorageType.fromString(parts[0]);
            StorageType to = StorageType.fromString(parts[1]);

            if (from == null || to == null || from == to) {
                sendMessage.send(sender, "messages.migrate.not-supported", ph); // Mensaje
                return true;
            }

            if (from.name().equals(to.name())) {
                sendMessage.send(sender, "messages.migrate.same-storage-type", ph); // Mensaje
                return true;
            }

            if (!from.name().equals(GV.getString(plugin, "bank.data.type").toUpperCase())) {
                ph.put("%MIGRATEDFROM%", from.name().toUpperCase());
                sendMessage.send(sender, "messages.migrate.not-current-data-type", ph); // Mensaje
                return true;
            }

            // Guardar migración pendiente para confirmar
            /*boolean backup = args.length > 2 && "backup".equalsIgnoreCase(args[2]);
            PendingMigration pendingMigration = new PendingMigration(from, to, backup);*/
            PendingMigration pendingMigration = new PendingMigration(from, to, false);
            plugin.getPendingMigrations().put(player.getUniqueId(), pendingMigration);
            sendMessage.send(sender, "messages.migrate.confirm-needed", ph); // Mensaje

            // Programar eliminación automática después de 10 segundos (200 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (plugin.getPendingMigrations().containsKey(player.getUniqueId())) {
                        plugin.getPendingMigrations().remove(player.getUniqueId());
                        // Mensaje al jugador si sigue en línea
                        Player p = Bukkit.getPlayer(player.getUniqueId());
                        if (p != null && p.isOnline()) {
                            sendMessage.send(p, "messages.migrate.confirm-expired", ph); // Mensaje
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
