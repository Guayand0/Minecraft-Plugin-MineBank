package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.optional.qual.OptionalBottom;

import javax.annotation.Nullable;
import java.util.Map;

public class SendMessage {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();

    public SendMessage(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();

    }

    public void send(CommandSender sender, String messagePath, Map<String, String> ph) {
        Map<String, String> placeholders = (ph != null) ? ph : plugin.placeholders;
        for (String message : languageManager.getAllMessage(messagePath)) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                sender.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, placeholders));
            } else {
                sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, placeholders));
            }
        }
    }

    public void sendRaw(CommandSender sender, String message, Map<String,String> ph) {
        Map<String, String> placeholders = (ph != null) ? ph : plugin.placeholders;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sender.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, placeholders));
        } else {
            sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, placeholders));
        }
    }
}
