package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;


public class SubComandoMax implements CommandExecutor {

    private final MineBank plugin;
    private String targetPlayerName;

    public SubComandoMax(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        if (args.length < 2) {
            maxUsage(player);
            return true;
        }

        String type = args[1];

        if (args.length >= 3) {
            targetPlayerName = args[2];
            String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);
            if (targetUUID == null) {
                notFoundPlayer(player);
                return true;
            }
        } else {
            targetPlayerName = null;
        }

        if (type.equalsIgnoreCase("bal") || type.equalsIgnoreCase("balance")) {
            // Lógica para obtener el balance
            if (targetPlayerName == null) {
                playerBankMaxStorage(player);
            } else {
                targetBankMaxStorage(player);
            }
        } else if (type.equalsIgnoreCase("level")) {
            if (targetPlayerName == null) {
                playerBankMaxLevel(player);
            } else {
                String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);
                if (targetUUID != null) {
                    targetBankMaxLevel(player);
                } else {
                    notFoundPlayer(player);
                }
            }
        } else {
            maxUsage(player);
        }

        return true;
    }

    private void maxUsage(Player player) {
        String messagePath = "bank.usage.max";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void playerBankMaxStorage(Player player) {
        String messagePath = "bank.max.playerBankMaxStorage";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void targetBankMaxStorage(Player player) {
        // Mensaje de depuración
        String messagePath = "bank.max.targetBankMaxStorage";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0);
    }

    private void playerBankMaxLevel(Player player) {
        String messagePath = "bank.max.playerBankMaxLevel";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void targetBankMaxLevel(Player player) {
        // Mensaje de depuración
        String messagePath = "bank.max.targetBankMaxLevel";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0);
    }

    private void notFoundPlayer(Player player) {
        String messagePath = "bank.notFoundPlayer";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0);
    }
}
