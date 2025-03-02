package com.Guayand0.commands;

import com.Guayand0.MineBank;
import com.Guayand0.commands.subcommands.*;
import com.Guayand0.events.BankInventoryEvent;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBank implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankInventoryEvent bankInventoryEvent;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();

    private final SubCommandAdd subCommandAdd;
    private final SubCommandTake subCommandTake;
    private final SubCommandData subCommandData;
    private final SubCommandLevelUp subCommandLevelUp;
    private final SubCommandHelp subCommandHelp;
    private final SubCommandBalTop subCommandBalTop;
    private final SubCommandSet subCommandSet;
    private final SubCommandReceive subCommandReceive;
    //private final SubCommandLottery subCommandLottery;

    public CommandBank(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankInventoryEvent = plugin.getBankInventoryEvent();

        this.subCommandAdd = new SubCommandAdd(plugin);
        this.subCommandTake = new SubCommandTake(plugin);
        this.subCommandData = new SubCommandData(plugin);
        this.subCommandLevelUp = new SubCommandLevelUp(plugin);
        this.subCommandHelp = new SubCommandHelp(plugin);
        this.subCommandBalTop = new SubCommandBalTop(plugin);
        this.subCommandSet = new SubCommandSet(plugin);
        this.subCommandReceive = new SubCommandReceive(plugin);
        //this.subCommandLottery = new SubCommandLottery(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            for (String message : languageManager.getAllMessage("messages.console-error")) {
                sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, plugin.placeholders));
            }
            return true;
        }

        Player player = (Player) sender;

        try {

            boolean bankUseAllowed = BU.getBankAllowed(plugin);

            // Si el banco no está activado no hacer nada
            if (!bankUseAllowed) {
                bankDisabled(player); // Mensaje
                return true;
            }

            // Si el jugador no tiene permisos no hacer nada
            if (!player.hasPermission(plugin.pluginName + ".use") && !player.hasPermission(plugin.pluginName + ".admin")) {
                noPerm(player); // Mensaje
                return true;
            }

            // Sin argumentos
            if (args.length == 0) {

                // Detectar la versión de Minecraft
                String version = Bukkit.getVersion();

                // Comprobar si es version reciente
                boolean isRecentVersion = version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") || version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20") || version.contains("1.21");
                if (isRecentVersion) {
                    bankInventoryEvent.openBankInventory(player); // Abrir el inventario del banco si tiene version reciente
                } else {
                    bankUsage(player); // Mensaje
                }
                return true;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "data":
                    // Delegar al subcomando
                    return subCommandData.onCommand(player, command, label, args);
                case "add":
                case "deposit":
                    return subCommandAdd.onCommand(player, command, label, args);
                case "take":
                case "withdraw":
                    return subCommandTake.onCommand(player, command, label, args);
                case "top":
                case "baltop":
                case "balancetop":
                    return subCommandBalTop.onCommand(player, command, label, args);
                case "levelup":
                    return subCommandLevelUp.onCommand(player, command, label, args);
                case "set":
                    if (!player.hasPermission(plugin.pluginName + ".admin")) {
                        noPerm(player); // Mensaje
                        return true;
                    }
                    return subCommandSet.onCommand(player, command, label, args);
                case "help":
                    return subCommandHelp.onCommand(player, command, label, args);
                case "receive":
                    return subCommandReceive.onCommand(player, command, label, args);
                default:
                    bankUsage(player); // Mensaje
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, "%plugin% &cAn error occurred while processing the command.", plugin.placeholders));
        }

        return true;
    }

    private void bankDisabled(Player player){
        for (String message : languageManager.getAllMessage("messages.bank-disabled")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankUsage(Player player){
        for (String message : languageManager.getAllMessage("bank.general-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPerm(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
