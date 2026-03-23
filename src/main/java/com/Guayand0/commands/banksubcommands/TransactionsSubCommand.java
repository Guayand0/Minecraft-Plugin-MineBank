package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.guis.TransactionGUI;
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

public class TransactionsSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final TransactionGUI transactionGUI;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public TransactionsSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.transactionGUI = plugin.getTransactionGUI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = player.hasPermission(plugin.pluginName + ".admin") ? "bank.transaction.usage-admin" : "bank.transaction.usage";

        try {
            if (!plugin.getMainGUI().guiExists("transactions")) {
                sendMessage.send(sender, "bank.gui.not-found", ph);
                return true;
            }

            /*if (args.length >= 2 && "player".equalsIgnoreCase(args[1])) {
                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    sendMessage.send(sender, "messages.no-perm", ph);
                    return true;
                }

                if (args.length < 3) {
                    sendMessage.send(sender, "bank.transaction-usage", ph);
                    return true;
                }

                String targetName = args[2];
                UUID targetUuid = PU.getUUIDFromName(targetName);
                if (targetUuid == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph);
                    return true;
                }

                PlayerData targetData = dataStorage.loadPlayerData(targetUuid);
                if (targetData == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph);
                    return true;
                }

                int page = 1;
                if (args.length >= 4) {
                    page = parsePageOrFail(sender, ph, args[3]);
                    if (page == -1) return true;
                }

                transactionGUI.open(player, page);
                return true;
            }*/

            int page = 1;
            String filter = null;

            if (args.length >= 2) {
                if (isTypeFilter(args[1])) {
                    filter = normalizeFilter(args[1]);
                    if (args.length >= 3) {
                        page = parsePageOrFail(sender, ph, args[2], usageKey);
                        if (page == -1) return true;
                    }
                } else {
                    page = parsePageOrFail(sender, ph, args[1], usageKey);
                    if (page == -1) return true;
                }
            }

            transactionGUI.open(player, page, filter);
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
            sendMessage.send(sender, "messages.processing-command-error", ph);
        }
        return true;
    }

    private int parsePageOrFail(CommandSender sender, Map<String, String> ph, String rawPage, String usageKey) {
        try {
            int page = Integer.parseInt(rawPage);
            if (page <= 0) {
                sendMessage.send(sender, usageKey, ph);
                return -1;
            }
            return page;
        } catch (NumberFormatException e) {
            sendMessage.send(sender, usageKey, ph);
            return -1;
        }
    }

    private boolean isTypeFilter(String raw) {
        if (raw == null) return false;
        String normalized = raw.trim().toLowerCase();
        return "deposit".equals(normalized) || "withdraw".equals(normalized);
    }

    private String normalizeFilter(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }
}
