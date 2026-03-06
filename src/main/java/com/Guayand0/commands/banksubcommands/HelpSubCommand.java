package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.utils.SendMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class HelpSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    public HelpSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        }

        try {
            sendMessage.send(sender, "bank.help", ph); // Mensaje
        } catch (Exception e) {
            sendMessage.send(sender, "bank.help", ph); // Mensaje
        }
        return true;
    }
}
