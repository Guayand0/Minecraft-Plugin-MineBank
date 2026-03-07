package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
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

import java.util.Map;
import java.util.UUID;

public class DataSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();
    private final PlayerUtils PU = new PlayerUtils();

    public DataSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        try {
            String playerName;
            UUID uuid;

            if (args.length < 2) {
                uuid = player.getUniqueId();
            } else {
                playerName = args[1];
                uuid = PU.getUUIDFromName(playerName);
            }

            if (uuid == null) {
                Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }
            Map<String, String> ph = plugin.buildPlayerPlaceholders(uuid);

            // Cargar datos del jugador
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = playerData.getBank().getName();

            // Datos del banco
            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                return true;
            }

            sendMessage.send(sender, "bank.data.bank-data", ph); // Mensaje

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
