package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.utils.SendMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BackupSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    public BackupSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        }

        try {
            String usageKey = "bank.backup.usage-admin";
            if (args.length > 1) {
                sendMessage.send(sender, usageKey, ph);
                return true;
            }

            dataStorage.backup();
            sendMessage.send(sender, "bank.backup.success", ph);
        } catch (Exception e) {
            sendMessage.send(sender, "bank.backup.failed", ph);
        }

        return true;
    }
}
