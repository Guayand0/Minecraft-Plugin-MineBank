package com.Guayand0.utils;

import com.Guayand0.MineBank;
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
    private final BankUtils BU = new BankUtils();

    public TabComplete(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // Verifica si el sender tiene el permiso "plugin.admin"
        boolean hasAdminPermission = sender.hasPermission(plugin.pluginName + ".admin");

        if (command.getName().equalsIgnoreCase(plugin.pluginName)) {

            if (args.length == 1) {

                if (hasAdminPermission) {
                    completions.addAll(Arrays.asList("help", "reload", "info", "permissions"));
                }
            }

        } else if (command.getName().equalsIgnoreCase("bank")) {

            if (args.length == 1) {
                completions.addAll(Arrays.asList("help", "add", "deposit", "take", "withdraw", "top", "baltop", "balancetop", "data", "levelup", "receive"));

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

                        // si es admin permite modificar el dinero de un jugador
                        if (hasAdminPermission) {
                            try { completions.addAll(BU.getPlayerNameOfBank(plugin)); } catch (Exception e) { e.printStackTrace(); }
                        }
                        break;

                    case "top":
                    case "baltop":
                    case "balancetop":
                        completions.addAll(Arrays.asList("1", "3", "5", "8"));
                        break;

                    case "receive":
                        completions.addAll(Arrays.asList("profit"));
                        break;

                    case "data":
                        try { completions.addAll(BU.getPlayerNameOfBank(plugin)); } catch (Exception e) { e.printStackTrace(); }
                        break;

                    case "set":

                        boolean setOfflinePlayersTabCompleter = BU.getBankSetTabCompleterOfflinePlayers(plugin);

                        // Añadir lista de todos los jugadores que hay en banco si está activado desde la config
                        if (setOfflinePlayersTabCompleter) {
                            try { completions.addAll(BU.getPlayerNameOfBank(plugin)); } catch (Exception e) { e.printStackTrace(); }
                        } else {
                            // Añadir lista de jugadores conectados
                            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().collect(Collectors.toList()));
                        }
                        break;
                }

            } else if (args.length == 3) {

                switch (args[0].toLowerCase()) {
                    case "add":
                    case "deposit":
                    case "take":
                    case "withdraw":
                        if (hasAdminPermission) {
                            List<String> invalidAmounts = Arrays.asList("500", "half-balance", "all", "mid-max");
                            if (!invalidAmounts.contains(args[1]) && !args[1].matches("\\d+")) {
                                completions.addAll(Arrays.asList("100", "500", "800"));
                            }
                        }
                        break;

                    case "set":
                        completions.addAll(Arrays.asList("balance", "level"));
                        break;
                }

            } else if (args.length == 4) {

                if (args[0].equalsIgnoreCase("set")) {

                    if (args[2].equalsIgnoreCase("balance")) {
                        completions.addAll(Arrays.asList("500", "mid-max", "max"));

                    } else if (args[2].equalsIgnoreCase("level")) {
                        completions.addAll(Arrays.asList("1", "mid-max", "max"));
                    }
                }
            }
        }

        return completions.stream()
                .filter(option -> option.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}