package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WebSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();

    public WebSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sendMessage.send(sender, "messages.console-error", null);
            return true;
        }

        Player player = (Player) sender;
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = player.hasPermission(plugin.pluginName + ".admin") ? "bank.web.usage-admin" : "bank.web.usage";

        if (args.length != 1) {
            sendMessage.send(sender, usageKey, ph);
            return true;
        }

        boolean enabled = GV.getBoolean(plugin, "web.enabled", false);
        if (!enabled) {
            sendMessage.send(sender, "bank.web.disabled", ph);
            return true;
        }

        String publicUrl = GV.getString(plugin, "web.url", "");
        String host = GV.getString(plugin, "web.host", "localhost");
        int port = GV.getInt(plugin, "web.port", 16104);
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }

        String token = plugin.getWebTokenStore().createNewSession(player.getUniqueId());
        String url;
        if (publicUrl != null && !publicUrl.trim().isEmpty()) {
            String base = publicUrl.trim();
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                base = "http://" + base;
            }
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            url = base + "/player?t=" + token;
        } else {
            String base = host.startsWith("http://") || host.startsWith("https://") ? host : "http://" + host;
            url = base + ":" + port + "/player?t=" + token;
        }

        List<String> lines = plugin.getLanguageManager().getAllMessage("bank.web.enabled.message");
        List<String> hoverLines = plugin.getLanguageManager().getAllMessage("bank.web.enabled.hover");
        List<String> clickableLines = plugin.getLanguageManager().getAllMessage("bank.web.enabled.clickable");

        String hoverRaw = hoverLines.isEmpty() ? "" : hoverLines.get(0);
        String clickableRaw = clickableLines.isEmpty() ? "[WEB]" : clickableLines.get(0);
        if (clickableRaw == null || clickableRaw.trim().isEmpty()) {
            clickableRaw = "[WEB]";
        }
        String hoverText = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, hoverRaw, ph);
        String clickableText = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, clickableRaw, ph);

        for (String rawLine : lines) {
            if (rawLine == null) continue;
            int clickableIndex = rawLine.toLowerCase(Locale.ROOT).indexOf(clickableRaw.toLowerCase(Locale.ROOT));
            if (clickableIndex >= 0) {
                String beforeRaw = rawLine.substring(0, clickableIndex);
                String afterRaw = rawLine.substring(clickableIndex + clickableRaw.length());

                String before = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, beforeRaw, ph);
                String after = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, afterRaw, ph);
                String webLabel = clickableText;

                String lastColors = ChatColor.getLastColors(before);
                String webColored = lastColors + webLabel;

                TextComponent message = new TextComponent();
                for (BaseComponent comp : TextComponent.fromLegacyText(before)) {
                    message.addExtra(comp);
                }

                TextComponent webComp = new TextComponent(TextComponent.fromLegacyText(webColored));
                webComp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                webComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));
                message.addExtra(webComp);

                for (BaseComponent comp : TextComponent.fromLegacyText(after)) {
                    message.addExtra(comp);
                }

                player.spigot().sendMessage(message);
            } else {
                String resolved = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, rawLine, ph);
                player.sendMessage(resolved);
            }
        }

        return true;
    }
}
