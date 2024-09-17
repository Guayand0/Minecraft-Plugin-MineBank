package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubComandoHelp implements CommandExecutor {

    private final MineBank plugin;

    public SubComandoHelp(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        String messagePath = "messages.help-bank";
        MessageUtils.sendMessageListWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
        return true;
    }
}