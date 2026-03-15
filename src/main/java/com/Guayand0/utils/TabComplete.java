package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.dbmigration.StorageType;
import com.Guayand0.zlib.GetValues;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                    completions.addAll(Arrays.asList("help", "reload", "info", "permissions", "backup", "event", "bank"));
                }

                if (hasAdminPermission && hasMigrationPermission) {
                    completions.addAll(Arrays.asList("migrate"));
                }

            } else if (args.length == 2) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("event")) {
                    completions.addAll(Arrays.asList("profit", "tax"));
                }
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    completions.addAll(Arrays.asList("create", "add", "modify", "delete", "rename"));
                }
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

            } else if (args.length == 3) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("event")) {
                    completions.addAll(Arrays.asList("x2", "x1.5", "x0.6", "cancel"));
                }
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (Arrays.asList("add", "modify", "delete", "rename").contains(args[1].toLowerCase())) {
                        try {
                            completions.addAll(dataStorage.getAllBankNames());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (args[1].equalsIgnoreCase("delete")) {
                        completions.add("confirm");
                    }
                }
            } else if (args.length == 4) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("event")) {
                    if (!args[2].equalsIgnoreCase("cancel")) {
                        completions.addAll(Arrays.asList("30s", "15m", "6h", "1d", "1w"));
                    }
                }
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add")) {
                        completions.addAll(Arrays.asList("level"));
                    } else if (args[1].equalsIgnoreCase("modify")) {
                        completions.addAll(Arrays.asList("level", "priority"));
                    } else if (args[1].equalsIgnoreCase("delete")) {
                        completions.addAll(Arrays.asList("level"));
                    } else if (args[1].equalsIgnoreCase("rename")) {
                    }
                }
            } else if (args.length == 5) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("level")) {
                        String nextLevel = resolveNextBankLevel(args[2]);
                        if (nextLevel != null) {
                            completions.add(nextLevel);
                        }
                    } else if (args[1].equalsIgnoreCase("modify") && args[3].equalsIgnoreCase("level")) {
                        completions.addAll(resolveBankLevels(args[2]));
                    } else if (args[1].equalsIgnoreCase("delete") && args[3].equalsIgnoreCase("level")) {
                        completions.addAll(resolveBankLevels(args[2]));
                    }
                }
            } else if (args.length == 6) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("level")) {
                        completions.addAll(Arrays.asList("max_balance"));
                    } else if (args[1].equalsIgnoreCase("modify") && args[3].equalsIgnoreCase("level")) {
                        completions.addAll(Arrays.asList("max_balance", "upgrade_cost"));
                    }
                }
            } else if (args.length == 7) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("level") && args[5].equalsIgnoreCase("max_balance")) {
                        completions.addAll(Arrays.asList("1000", "5000", "10000"));
                    } else if (args[1].equalsIgnoreCase("modify") && args[3].equalsIgnoreCase("level")) {
                        String currentValue = resolveBankLevelFieldValue(args[2], args[4], args[5]);
                        if (currentValue != null) {
                            completions.add(currentValue);
                        }
                    }
                }
            } else if (args.length == 8) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("level")) {
                        completions.addAll(Arrays.asList("upgrade_cost"));
                    }
                }
            } else if (args.length == 9) {
                if (hasAdminPermission && args[0].equalsIgnoreCase("bank")) {
                    if (args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("level") && args[7].equalsIgnoreCase("upgrade_cost")) {
                        completions.addAll(Arrays.asList("0", "1000", "5000"));
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
                completions.addAll(Arrays.asList("help", "data", "bal", "balance", "level", "add", "deposit", "take", "withdraw", "levelup", "top", "baltop", "balancetop", "receive", "transactions", "history", "events"));

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

                    case "history":
                    case "transactions":
                        completions.addAll(Arrays.asList("1", "2", "3"));
                        if (hasAdminPermission) {
                            completions.add("player");
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

                    case "history":
                    case "transactions":
                        if (hasAdminPermission && args[1].equalsIgnoreCase("player")) {
                            playerCompleter(offlinePlayersTabCompleter, completions);
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
    
    private List<String> resolveBankLevels(String bankName) {
        if (bankName == null || bankName.isEmpty()) return Collections.emptyList();
        String resolvedName = resolveExistingBankName(bankName);
        Map<String, BankData> bankDataMap = dataStorage.loadBankData(resolvedName);
        if (bankDataMap == null || !bankDataMap.containsKey(resolvedName)) {
            return Collections.emptyList();
        }

        Map<String, BankData.Level> levels = bankDataMap.get(resolvedName).getLevels();
        if (levels == null || levels.isEmpty()) return Collections.emptyList();

        List<Integer> sorted = new ArrayList<>();
        for (String key : levels.keySet()) {
            try {
                sorted.add(Integer.parseInt(key));
            } catch (Exception ignored) {}
        }
        Collections.sort(sorted);

        List<String> result = new ArrayList<>();
        for (Integer level : sorted) {
            result.add(String.valueOf(level));
        }
        return result;
    }

    private String resolveNextBankLevel(String bankName) {
        List<String> levels = resolveBankLevels(bankName);
        int max = 0;
        for (String level : levels) {
            try {
                max = Math.max(max, Integer.parseInt(level));
            } catch (Exception ignored) {}
        }
        return String.valueOf(max + 1);
    }

    private String resolveExistingBankName(String bankName) {
        try {
            for (String name : dataStorage.getAllBankNames()) {
                if (name.equalsIgnoreCase(bankName)) {
                    return name;
                }
            }
        } catch (Exception ignored) {}
        return bankName;
    }

    private String resolveBankLevelFieldValue(String bankName, String levelRaw, String field) {
        if (bankName == null || levelRaw == null || field == null) return null;
        String resolvedName = resolveExistingBankName(bankName);
        Map<String, BankData> bankDataMap = dataStorage.loadBankData(resolvedName);
        if (bankDataMap == null || !bankDataMap.containsKey(resolvedName)) {
            return null;
        }

        Map<String, BankData.Level> levels = bankDataMap.get(resolvedName).getLevels();
        if (levels == null) return null;

        BankData.Level level = levels.get(levelRaw);
        if (level == null) return null;

        if ("max_balance".equalsIgnoreCase(field)) {
            return String.valueOf(level.getMax_balance());
        }
        if ("upgrade_cost".equalsIgnoreCase(field)) {
            return String.valueOf(level.getUpgrade_cost());
        }
        return null;
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
