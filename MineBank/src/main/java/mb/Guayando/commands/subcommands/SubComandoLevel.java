package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubComandoLevel implements CommandExecutor {

    private final MineBank plugin;

    public SubComandoLevel(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        String targetPlayerName;
        if (args.length > 1) {
            targetPlayerName = args[1];
        } else {
            targetPlayerName = null;
        }

        if (targetPlayerName != null) {
            String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);

            // Verifica si el UUID del jugador es válido
            if (targetUUID == null) {
                notFoundPlayer(player, targetPlayerName);
                return true;
            }
            targetBankLevel(player, targetPlayerName);
        } else {
            playerBankLevel(player);
        }
        return true;
    }

    private void playerBankLevel(Player player) {
        String messagePath = "bank.level.playerBankLevel";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void targetBankLevel(Player player, String targetPlayerName) {
        String messagePath = "bank.level.targetBankLevel";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0);
    }

    private void notFoundPlayer(Player player, String targetPlayerName) {
        String messagePath = "bank.notFoundPlayer";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0);
    }
}
