package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.Guayand0.zlib.PlayerUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class AddSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final DataStorage dataStorage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();
    private final PlayerUtils PU = new PlayerUtils();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    private final Economy economy;

    public AddSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.dataStorage = plugin.getStorage();

        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            try {
                sendMessage.sendRaw(sender, "%plugin% &fComing soon!", ph); // Mensaje
                return true;

/*                // args[0]=add, args[1]=player, args[2]=target, args[3]=amount
                if (args.length < 4 || !args[1].equalsIgnoreCase("player")) {
                    sendMessage.send(sender, "bank.add-usage", ph); // Mensaje
                    return true;
                }
*/
            } catch (Exception e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
                sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
            }
            return true;
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            sendMessage.send(sender, "bank.add-usage", ph); // Mensaje
            return true;
        }

        try {

            UUID uuid;
            String amountArg;
            String targetName = "N/A";

            if (args[1].equalsIgnoreCase("player")) {

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    sendMessage.send(sender, "bank.add-usage", ph); // Mensaje
                    return true;
                }

                // args[0]=add, args[1]=player, args[2]=target, args[3]=amount
                if (args.length < 4) {
                    sendMessage.send(sender, "bank.add-usage", ph); // Mensaje
                    return true;
                }

                targetName = args[2];
                uuid = PU.getUUIDFromName(targetName);
                 amountArg = args[3];
            } else {
                uuid = player.getUniqueId();
                amountArg = args[1];
            }

            ph = plugin.buildPlayerPlaceholders(uuid);

            if (uuid == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            // Cargar datos del jugador
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null) {
                sendMessage.send(sender, "bank.unregistered-player", ph); // Mensaje
                return true;
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            // Datos del banco
            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                sendMessage.send(sender, "bank.unregistered-bank", ph); // Mensaje
                return true;
            }

            BankData bankData = bankDataMap.get(bankName);
            int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();

            // Determinar cantidad a depositar
            int amountDeposited;

            if (args[1].equalsIgnoreCase("player")) {

                try {
                    amountDeposited = Integer.parseInt(amountArg);
                } catch (NumberFormatException e) {
                    sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                    return true;
                }

                ph.put("%targetPlayerName%", targetName);

                // Verificar que no exceda el máximo
                if ((bankBalance + amountDeposited) > bankMaxBalance) {
                    ph.put("%targetBankMaxBalance%", BSP.format(plugin, String.valueOf(bankMaxBalance)));
                    sendMessage.send(sender, "bank.add.target-deposit-exceeds" , ph); // Mensaje
                    return true;
                }

                // Actualizar balance del banco
                int newBalance = bankBalance + amountDeposited;
                playerData.getBank().setBalance(newBalance);
                dataStorage.savePlayerData(uuid, playerData);

                // Mensaje al ejecutor
                ph.put("%amount%", BSP.format(plugin, String.valueOf(amountDeposited)));
                sendMessage.send(sender, "bank.add.target-deposit-success" , ph); // Mensaje

                /*// Mensaje al jugador objetivo si está online y activado en config

                deposit-received: '%chatPlugin% &eYou have received &6%amount% &einto your bank'

                Player targetPlayer = Bukkit.getPlayer(uuid);
                boolean alertPlayer = GV.getBoolean(plugin, "bank.admin-modify-bank-data-alert", false);
                if (targetPlayer != null && targetPlayer.isOnline() && alertPlayer) {
                    Map<String,String> phTarget = plugin.buildPlayerPlaceholders(uuid);
                    phTarget.put("%amount%", BSP.format(plugin, String.valueOf(amountDeposited)));
                    sendMessage.send(targetPlayer, "bank.add.deposit-received", phTarget);  // Mensaje
                }*/

            } else {

                int playerEconomyBalance = PU.getPlayerBalance(player, economy);

                if (args[1].equalsIgnoreCase("half-balance")) {
                    amountDeposited = bankBalance / 2;
                } else if (args[1].equalsIgnoreCase("all")) {
                    amountDeposited = Math.min(playerEconomyBalance, (bankMaxBalance - bankBalance));
                } else if (args[1].equalsIgnoreCase("mid-max")) {
                    amountDeposited = bankMaxBalance / 2;
                } else {
                    try {
                        amountDeposited = Integer.parseInt(amountArg);
                    } catch (NumberFormatException e) {
                        sendMessage.send(sender, "bank.not-positive-integer", ph); // Mensaje
                        return true;
                    }
                }

                if (amountDeposited <= 0) {
                    sendMessage.send(sender, "bank.not-positive-integer" , ph); // Mensaje
                    return true;
                }

                if (playerEconomyBalance < amountDeposited) {
                    sendMessage.send(sender, "bank.add.not-enough-bank-balance" , ph); // Mensaje
                    return true;
                }

                if ((bankBalance + amountDeposited) > bankMaxBalance) {
                    sendMessage.send(sender, "bank.add.deposit-exceeds" , ph); // Mensaje
                    return true;
                }

                economy.withdrawPlayer(player, amountDeposited);

                // Actualizar balance
                int newBalance = bankBalance + amountDeposited;
                playerData.getBank().setBalance(newBalance);
                dataStorage.savePlayerData(uuid, playerData);

                ph.put("%amount%", BSP.format(plugin, String.valueOf(amountDeposited)));
                sendMessage.send(sender, "bank.add.deposit-success" , ph); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
