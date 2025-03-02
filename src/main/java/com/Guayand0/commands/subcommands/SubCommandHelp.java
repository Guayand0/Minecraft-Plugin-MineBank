package com.Guayand0.commands.subcommands;

import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SubCommandHelp implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();

    public SubCommandHelp(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        bankHelp(player); // Mensaje

        return true;
    }

    private void bankHelp(Player player) {
        for (String message : languageManager.getAllMessage("bank.help")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
