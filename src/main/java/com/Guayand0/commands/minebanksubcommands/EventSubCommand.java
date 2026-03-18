package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.managers.EventManager;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class EventSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public EventSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = new SendMessage(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<String, String> ph;

        if (!(sender instanceof Player)) {
            ph = plugin.buildPlayerPlaceholders(null);

            try {
                if (args.length < 2) {
                    sendMessage.send(sender, "messages.console-help", ph);
                    return true;
                }

                EventManager.EventType type = parseType(args[1]);
                if (type == null) {
                    sendMessage.send(sender, "messages.console-help", ph);
                    return true;
                }

                String typeLabel = type == EventManager.EventType.PROFIT ? "profit" : "tax";
                ph.put("%type%", typeLabel);

                if (args.length >= 3 && args[2].equalsIgnoreCase("cancel")) {
                    if (!plugin.getEventManager().isActive(type)) {
                        sendMessage.send(sender, "bank.event.not-active", ph);
                        return true;
                    }
                    plugin.getEventManager().cancelEvent(type);
                    sendMessage.send(sender, "bank.event.cancelled", ph);
                    return true;
                }

                if (args.length < 4) {
                    sendMessage.send(sender, "messages.console-help", ph);
                    return true;
                }

                Double multiplier = parseMultiplier(args[2]);
                if (multiplier == null) {
                    sendMessage.send(sender, "bank.event.invalid-multiplier", ph);
                    return true;
                }

                Long durationMillis = parseDurationMillis(args[3]);
                if (durationMillis == null) {
                    sendMessage.send(sender, "bank.event.invalid-duration", ph);
                    return true;
                }

                plugin.getEventManager().startEvent(type, multiplier, durationMillis);
                ph.put("%multiplier%", formatMultiplier(multiplier));
                ph.put("%duration%", formatDuration(durationMillis));
                sendMessage.send(sender, "bank.event.started", ph);
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                sendMessage.send(sender, "messages.processing-command-error", ph);
                return true;
            }
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        try {
            if (args.length < 2) {
                sendMessage.send(sender, "bank.event.usage-admin", ph);
                return true;
            }

            EventManager.EventType type = parseType(args[1]);
            if (type == null) {
                sendMessage.send(sender, "bank.event.usage-admin", ph);
                return true;
            }

            String typeLabel = type == EventManager.EventType.PROFIT ? "profit" : "tax";
            ph.put("%type%", typeLabel);

            if (args.length >= 3 && args[2].equalsIgnoreCase("cancel")) {
                if (!plugin.getEventManager().isActive(type)) {
                    sendMessage.send(sender, "bank.event.not-active", ph);
                    return true;
                }
                plugin.getEventManager().cancelEvent(type);
                sendMessage.send(sender, "bank.event.cancelled", ph);
                return true;
            }

            if (args.length < 4) {
                sendMessage.send(sender, "bank.event.usage-admin", ph);
                return true;
            }

            Double multiplier = parseMultiplier(args[2]);
            if (multiplier == null) {
                sendMessage.send(sender, "bank.event.invalid-multiplier", ph);
                return true;
            }

            Long durationMillis = parseDurationMillis(args[3]);
            if (durationMillis == null) {
                sendMessage.send(sender, "bank.event.invalid-duration", ph);
                return true;
            }

            plugin.getEventManager().startEvent(type, multiplier, durationMillis);
            ph.put("%multiplier%", formatMultiplier(multiplier));
            ph.put("%duration%", formatDuration(durationMillis));
            sendMessage.send(sender, "bank.event.started", ph);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);
            return true;
        }
    }

    private EventManager.EventType parseType(String raw) {
        if (raw == null) return null;
        if (raw.equalsIgnoreCase("profit")) return EventManager.EventType.PROFIT;
        if (raw.equalsIgnoreCase("tax")) return EventManager.EventType.TAX;
        return null;
    }

    private Double parseMultiplier(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        if (!raw.startsWith("x")) return null;
        String num = raw.substring(1);
        if (num.isEmpty()) return null;
        try {
            double value = Double.parseDouble(num);
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseDurationMillis(String raw) {
        if (raw == null || raw.length() < 2) return null;
        char unit = raw.charAt(raw.length() - 1);
        String number = raw.substring(0, raw.length() - 1);
        if (!number.matches("\\d+")) return null;

        long value;
        try {
            value = Long.parseLong(number);
        } catch (NumberFormatException e) {
            return null;
        }
        if (value <= 0) return null;

        long seconds;
        switch (unit) {
            case 's':
                seconds = value;
                break;
            case 'm':
                seconds = value * 60L;
                break;
            case 'h':
                seconds = value * 3600L;
                break;
            case 'd':
                seconds = value * 86400L;
                break;
            case 'w':
                seconds = value * 604800L;
                break;
            default:
                return null;
        }
        return seconds * 1000L;
    }

    private String formatMultiplier(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long weeks = totalSeconds / (7L * 24L * 3600L);
        totalSeconds %= (7L * 24L * 3600L);
        long days = totalSeconds / (24L * 3600L);
        totalSeconds %= (24L * 3600L);
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
