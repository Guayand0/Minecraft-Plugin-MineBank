package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.Guayand0.zlib.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BalTopSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final PlayerUtils PU = new PlayerUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    public BalTopSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        try {
            int amount = 10;

            // /bank baltop
            if (args.length == 1) {
                sendMessage.send(sender, "bank.top.title", ph); // Mensaje

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(amount);
                int position = 1;

                for (List<String> bankInfo : topBanks) {
                    ph.put("%topbankposition%", String.valueOf(position));
                    ph.put("%topplayername%", bankInfo.get(0));
                    ph.put("%topbankname%", bankInfo.get(1));
                    ph.put("%topbanklevel%", bankInfo.get(2));
                    ph.put("%topbankbalance%", BSP.format(plugin, bankInfo.get(3)));

                    sendMessage.send(sender, "bank.top.entry", ph); // Mensaje
                    position++;
                }
                return true;
            }

            // /bank baltop player <player>
            if (args.length >= 3 && args[1].equalsIgnoreCase("player")) {
                String targetName = args[2];
                UUID targetUUID = PU.getUUIDFromName(targetName);

                if (targetUUID == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                PlayerData targetData = dataStorage.loadPlayerData(targetUUID);
                if (targetData == null) {
                    sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                    return true;
                }

                sendMessage.send(sender, "bank.top.title", ph); // Mensaje

                ph.put("%topbankposition%", String.valueOf(plugin.getPlayerTopPosition(targetUUID)));
                ph.put("%topplayername%", targetName);
                ph.put("%topbankname%", targetData.getBank().getName());
                ph.put("%topbanklevel%", String.valueOf(targetData.getBank().getLevel()));
                ph.put("%topbankbalance%", BSP.format(plugin, String.valueOf(targetData.getBank().getBalance())));

                sendMessage.send(sender, "bank.top.entry", ph); // Mensaje
                return true;
            }

            // /bank baltop <amount>
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                return true;
            }

            sendMessage.send(sender, "bank.top.title", ph); // Mensaje

            List<List<String>> topBanks = dataStorage.getTopPlayerBankData(amount);
            int position = 1;

            for (List<String> bankInfo : topBanks) {
                ph.put("%topbankposition%", String.valueOf(position));
                ph.put("%topplayername%", bankInfo.get(0));
                ph.put("%topbankname%", bankInfo.get(1));
                ph.put("%topbanklevel%", bankInfo.get(2));
                ph.put("%topbankbalance%", BSP.format(plugin, bankInfo.get(3)));

                sendMessage.send(sender, "bank.top.entry", ph); // Mensaje
                position++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
