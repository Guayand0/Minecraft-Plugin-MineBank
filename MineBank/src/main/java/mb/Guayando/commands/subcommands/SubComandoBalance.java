package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubComandoBalance implements CommandExecutor {

    private final MineBank plugin;

    public SubComandoBalance(MineBank plugin) {
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

        // Lógica para obtener el balance
        if (targetPlayerName == null) {
            playerBalance(player);
        } else {
            String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);
            if (targetUUID != null) {
                // Si el jugador está desconectado, pasar `null` para targetPlayer pero usar su UUID
                targetBalance(targetPlayerName, player);
            } else {
                notFoundPlayer(player, targetPlayerName);
            }
        }
        return true;
    }

    public void playerBalance(Player player) {
        String messagePath = "bank.bal.playerBankBalance";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void targetBalance(String targetPlayerName, Player player) {
        String messagePath = "bank.bal.targetBankBalance";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void notFoundPlayer(Player player, String targetPlayerName) {
        String messagePath = "bank.notFoundPlayer";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }
}
