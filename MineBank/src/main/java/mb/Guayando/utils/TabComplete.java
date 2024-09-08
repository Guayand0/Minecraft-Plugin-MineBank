package mb.Guayando.utils;

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("minebank") || command.getName().equalsIgnoreCase("mb")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("help", "reload", "version", "author", "permissions", "plugin"));
            }
        } else if (command.getName().equalsIgnoreCase("bank")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("help", "add", "deposit", "take", "withdraw", "set", "top", "baltop", "balancetop", "bal", "balance", "max", "level", "levelup"));
            } else if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "add":
                    case "deposit":
                    case "take":
                    case "withdraw":
                        completions.addAll(Arrays.asList("500", "mid", "max", "midmax"));
                        break;
                    case "set":
                    case "bal":
                    case "balance":
                    case "level":
                        // Añadir lista de jugadores conectados
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()));
                        break;
                    case "max":
                        completions.addAll(Arrays.asList("balance", "level"));
                        break;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("set")) {
                    completions.addAll(Arrays.asList("balance", "level"));
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("set")) {
                    switch (args[2].toLowerCase()) {
                        case "balance":
                            completions.addAll(Arrays.asList("500", "midmax", "max"));
                            break;
                        case "level":
                            completions.addAll(Arrays.asList("1", "midmax", "max"));
                            break;
                    }
                }
            } else if (args.length == 5 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("level")) {
                // Sugerir true y false cuando el comando es /bank set <player> level <level>
                completions.addAll(Arrays.asList("true", "false"));
            }
        }

        return completions.stream()
                .filter(option -> option.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
