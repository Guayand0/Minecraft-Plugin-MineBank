package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
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

public class ReceiveSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public ReceiveSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = player.hasPermission(plugin.pluginName + ".admin") ? "bank.receive.usage-admin" : "bank.receive.usage";

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            sendMessage.send(sender, usageKey, ph); // Mensaje
            return true;
        }

        try {

            // Cargar datos del jugador
            PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());
            if (playerData == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();
            int offlineProfitAccrued = playerData.getBank().getOffline().getAccrued_profit();

            // Cargar datos del banco
            BankData bankData = dataStorage.loadBankData(bankName).get(bankName);
            int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();

            String arg = args[1];

            if (!arg.equalsIgnoreCase("profit")) {
                sendMessage.send(sender, usageKey, ph); // Mensaje
                return true;
            }

            if (offlineProfitAccrued <= 0) {
                sendMessage.send(sender, "bank.receive.offline-not-profit", ph); // Mensaje
                return true;
            }

            int playerBankSpace = bankMaxBalance - bankBalance;
            if (playerBankSpace < offlineProfitAccrued) {
                sendMessage.send(sender, "bank.receive.receive-exceeds", ph); // Mensaje
                return true;
            }

            // Actualizar balance y resetear offline profit
            int newBalance = bankBalance + offlineProfitAccrued;
            playerData.getBank().setBalance(newBalance);
            playerData.getBank().getOffline().setAccrued_profit(0);
            playerData.getBank().getOffline().setProfit_times(0);

            dataStorage.savePlayerData(player.getUniqueId(), playerData);

            sendMessage.send(sender, "bank.receive.offline-received-success", ph); // Mensaje

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
