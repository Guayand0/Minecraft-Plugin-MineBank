package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.guis.EventGUI;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class EventsSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public EventsSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            sendMessage.send(sender, "messages.console-error", ph);
            return true;
        }

        Player player = (Player) sender;

        try {
            EventGUI eventGUI = new EventGUI(plugin);
            eventGUI.open(player);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);
            return true;
        }
    }
}
