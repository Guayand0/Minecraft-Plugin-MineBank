package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.dbmigration.StorageType;
import com.Guayand0.zlib.GetValues;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {

    private final MineBank plugin;
    private final DataStorage dataStorage;
    private final GetValues GV = new GetValues();

    public TabComplete(MineBank plugin) {
        this.plugin = plugin;
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // Verifica si el sender tiene el permiso "plugin.admin"
        boolean hasAdminPermission = sender.hasPermission(plugin.pluginName + ".admin");
        boolean offlinePlayersTabCompleter = GV.getBoolean(plugin, "bank.offline-player-tabcompleter", true);

        boolean hasMigrationPermission = sender.hasPermission(plugin.pluginName + ".migration");

        if (command.getName().equalsIgnoreCase(plugin.pluginName)) {

            if (args.length == 1) {

                if (hasAdminPermission) {
                    completions.addAll(Arrays.asList("help", "reload", "info", "permissions", "backup"));
                }

                if (hasAdminPermission && hasMigrationPermission) {
                    completions.addAll(Arrays.asList("migrate"));
                }

            } else if (args.length == 2) {
                if (hasAdminPermission && hasMigrationPermission) {
                    if (args[0].equalsIgnoreCase("migrate")) {
                        completions.addAll(Arrays.asList("confirm"));

                        // Obtener todos los valores del enum
                        StorageType[] types = StorageType.values();

                        // Crear todas las combinaciones tipo1-tipo2
                        for (StorageType from : types) {
                            for (StorageType to : types) {
                                if (from != to) { // Evita JSON-JSON o MYSQL-MYSQL
                                    completions.add(from.name() + "-" + to.name());
                                }
                            }
                        }
                    }
                }

            } /*else if (args.length == 3) {
                if (hasAdminPermission && hasMigrationPermission) {
                    if (args[0].equalsIgnoreCase("migrate") && !args[1].equalsIgnoreCase("confirm")) {
                        completions.addAll(Arrays.asList("backup"));
                    }
                }
            }*/

        } else if (command.getName().equalsIgnoreCase("bank")) {

            if (args.length == 1) {
                completions.addAll(Arrays.asList("help", "data", "bal", "balance", "level", "add", "deposit", "take", "withdraw", "levelup", "top", "baltop", "balancetop", "receive"));

                if (hasAdminPermission) {
                    completions.add("set");
                }

            } else if (args.length == 2) {

                switch (args[0].toLowerCase()) {
                    case "add":
                    case "deposit":
                    case "take":
                    case "withdraw":
                        completions.addAll(Arrays.asList("500", "half-balance", "all", "mid-max"));

                        if (hasAdminPermission) {
                            completions.add("player");
                        }

                        break;

                    case "top":
                    case "baltop":
                    case "balancetop":
                        completions.addAll(Arrays.asList("1", "3", "5", "8", "player"));
                        break;

                    case "receive":
                        completions.addAll(Arrays.asList("profit"));
                        //completions.addAll(Arrays.asList("profit", "lottery"));
                        break;

                    case "data":
                    case "bal":
                    case "balance":
                    case "level":
                        try {
                            completions.addAll(dataStorage.getAllPlayerNames());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    /*case "lottery":
                        completions.addAll(Arrays.asList("participate", "check", "time"));
                        break;*/

                    case "set":
                        if (hasAdminPermission) {
                            playerCompleter(offlinePlayersTabCompleter, completions);
                        }
                        break;

                    case "levelup":
                        if (hasAdminPermission) {
                            playerCompleter(offlinePlayersTabCompleter, completions);
                        }
                        break;
                }

            } else if (args.length == 3) {

                switch (args[0].toLowerCase()) {
                    case "add":
                    case "deposit":
                    case "take":
                    case "withdraw":
                        // si es admin permite modificar el dinero de un jugador
                        if (args[1].equalsIgnoreCase("player") && hasAdminPermission) {
                            playerCompleter(offlinePlayersTabCompleter, completions);
                        }
                        break;

                    case "top":
                    case "baltop":
                    case "balancetop":
                        if (args[1].equalsIgnoreCase("player")) {
                            playerCompleter(offlinePlayersTabCompleter, completions);
                        }
                        break;

                    case "set":
                        if (hasAdminPermission) {
                            completions.addAll(Arrays.asList("balance", "level"));
                        }
                        break;
                }

            } else if (args.length == 4) {

                switch (args[0].toLowerCase()) {
                    case "add":
                    case "deposit":
                    case "take":
                    case "withdraw":
                        // si es admin permite modificar el dinero de un jugador
                        if (args[1].equalsIgnoreCase("player") && hasAdminPermission) {
                            completions.addAll(Arrays.asList("100", "500", "800"));
                        }
                        break;

                    case "set":
                        if (hasAdminPermission) {
                            if (args[2].equalsIgnoreCase("balance")) {
                                completions.addAll(Arrays.asList("500", "mid-max", "max"));

                            } else if (args[2].equalsIgnoreCase("level")) {
                                completions.addAll(Arrays.asList("1", "mid-max", "max"));
                            }
                        }
                        break;
                }
            }
        }

        return completions.stream()
                .filter(option -> option.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).
                sorted().collect(Collectors.toList());

    }
    
    private void playerCompleter(boolean offline, List<String> completions) {
        try {
            // Añadir lista de todos los jugadores que hay en banco si está activado desde la config
            if (offline) {
                completions.addAll(dataStorage.getAllPlayerNames());
            } else {
                // Añadir lista de jugadores conectados
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}