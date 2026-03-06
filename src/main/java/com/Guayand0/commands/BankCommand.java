package com.Guayand0.commands;

import com.Guayand0.MineBank;
import com.Guayand0.commands.banksubcommands.*;
import com.Guayand0.inventory.MainGUI;
import com.Guayand0.managers.*;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BankCommand implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final SendMessage sendMessage;
    private final MainGUI mainGUI;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    private final HelpSubCommand subCommandHelp;
    private final DataSubCommand subCommandData;
    private final AddSubCommand subCommandAdd;
    private final TakeSubCommand subCommandTake;
    private final SetSubCommand subCommandSet;
    private final LevelUpSubCommand subCommandLevelUp;
    private final BalTopSubCommand subCommandBalTop;
    private final ReceiveSubCommand subCommandReceive;
    private final GuiSubCommand subCommandGui;

    public BankCommand(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.sendMessage = plugin.getSendMessage();
        this.mainGUI = plugin.getMainGUI();

        this.subCommandHelp = new HelpSubCommand(plugin);
        this.subCommandData = new DataSubCommand(plugin);
        this.subCommandAdd = new AddSubCommand(plugin);
        this.subCommandTake = new TakeSubCommand(plugin);
        this.subCommandSet = new SetSubCommand(plugin);
        this.subCommandLevelUp = new LevelUpSubCommand(plugin);
        this.subCommandBalTop = new BalTopSubCommand(plugin);
        this.subCommandReceive = new ReceiveSubCommand(plugin);
        this.subCommandGui = new GuiSubCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String, String> ph = null;

        if (!(sender instanceof Player)) {
            // Consola
            if (args.length == 0) {
                sendMessage.send(sender, "messages.console-error", ph); // Mensaje
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help":
                    return subCommandHelp.onCommand(sender, command, label, args);

                case "add":
                case "deposit":
                    return subCommandAdd.onCommand(sender, command, label, args);

                case "take":
                case "withdraw":
                    return subCommandTake.onCommand(sender, command, label, args);

                case "set":
                    return subCommandSet.onCommand(sender, command, label, args);

                case "levelup":
                    return subCommandLevelUp.onCommand(sender, command, label, args);

                default:
                    sendMessage.send(sender, "messages.console-error", ph); // Mensaje
                    return true;
            }
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        try {

            boolean isPluginEnabled = GV.getBoolean(plugin, "config.bank-allowed", true);

            // Si el banco no está activado no hacer nada
            if (!isPluginEnabled) {
                sendMessage.send(sender, "config.plugin-disabled", ph); // Mensaje
                return true;
            }

            // Si el jugador no tiene permisos no hacer nada
            if (!player.hasPermission(plugin.pluginName + ".use") && !player.hasPermission(plugin.pluginName + ".admin")) {
                sendMessage.send(sender, "messages.no-perm", ph); // Mensaje
                return true;
            }

            // Sin argumentos
            if (args.length == 0) {

                // Detectar la versión de Minecraft
                String version = Bukkit.getVersion();

                // Comprobar si es version reciente
                boolean isRecentVersion = version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") || version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20") || version.contains("1.21");
                if (isRecentVersion) {
                    mainGUI.openInventory(player, "main"); // Abrir el inventario si tiene version reciente
                } else {
                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
                }
                return true;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                // Delegar al subcomando
                case "help":
                    return subCommandHelp.onCommand(player, command, label, args);

                case "data":
                case "bal":
                case "balance":
                case "level":
                    return subCommandData.onCommand(player, command, label, args);

                case "add":
                case "deposit":
                    return subCommandAdd.onCommand(player, command, label, args);

                case "take":
                case "withdraw":
                    return subCommandTake.onCommand(player, command, label, args);

                case "set":
                    return subCommandSet.onCommand(player, command, label, args);

                case "levelup":
                    return subCommandLevelUp.onCommand(player, command, label, args);

                case "top":
                case "baltop":
                case "balancetop":
                    return subCommandBalTop.onCommand(player, command, label, args);

                case "receive":
                    return subCommandReceive.onCommand(player, command, label, args);

                case "gui":
                    return subCommandGui.onCommand(player, command, label, args);

                default:
                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }

}


//package com.Guayand0.commands;
//
//import com.Guayand0.MineBank;
//import com.Guayand0.commands.banksubcommands.*;
//import com.Guayand0.inventory.MainGUI;
//import com.Guayand0.managers.*;
//import com.Guayand0.utils.SendMessage;
//import com.Guayand0.zlib.*;
//import org.bukkit.Bukkit;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//
//import java.util.Map;
//
//public class BankCommand implements CommandExecutor {
//
//    private final MineBank plugin;
//    private final LanguageManager languageManager;
//    private final SendMessage sendMessage;
//    private final MainGUI mainGUI;
//
//    private final GetValues GV = new GetValues();
//    private final MessageUtils MU = new MessageUtils();
//    private final ExceptionManager EM = new ExceptionManager();
//
//    private final HelpSubCommand subCommandHelp;
//    private final DataSubCommand subCommandData;
//    private final AddSubCommand subCommandAdd;
//    private final TakeSubCommand subCommandTake;
//    private final SetSubCommand subCommandSet;
//    private final LevelUpSubCommand subCommandLevelUp;
//    private final BalTopSubCommand subCommandBalTop;
//    private final ReceiveSubCommand subCommandReceive;
//
//    public BankCommand(MineBank plugin) {
//        this.plugin = plugin;
//        this.languageManager = plugin.getLanguageManager();
//        this.sendMessage = plugin.getSendMessage();
//        this.mainGUI = plugin.getMainGUI();
//
//        this.subCommandHelp = new HelpSubCommand(plugin);
//        this.subCommandData = new DataSubCommand(plugin);
//        this.subCommandAdd = new AddSubCommand(plugin);
//        this.subCommandTake = new TakeSubCommand(plugin);
//        this.subCommandSet = new SetSubCommand(plugin);
//        this.subCommandLevelUp = new LevelUpSubCommand(plugin);
//        this.subCommandBalTop = new BalTopSubCommand(plugin);
//        this.subCommandReceive = new ReceiveSubCommand(plugin);
//    }
//
//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//
//        if (!(sender instanceof Player)) {
//            for (String message : languageManager.getAllMessage("messages.console-error")) {
//                sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, plugin.placeholders));
//            }
//            return true;
//        }
//
//        Player player = (Player) sender;
//        Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
//
//        try {
//
//            boolean isPluginEnabled = GV.getBoolean(plugin, "config.bank-allowed", true);
//
//            // Si el banco no está activado no hacer nada
//            if (!isPluginEnabled) {
//                sendMessage.send(sender, "config.plugin-disabled", ph); // Mensaje
//                return true;
//            }
//
//            // Si el jugador no tiene permisos no hacer nada
//            if (!player.hasPermission(plugin.pluginName + ".use") && !player.hasPermission(plugin.pluginName + ".admin")) {
//                sendMessage.send(sender, "messages.no-perm", ph); // Mensaje
//                return true;
//            }
//
//            // Sin argumentos
//            if (args.length == 0) {
//
//                // Detectar la versión de Minecraft
//                String version = Bukkit.getVersion();
//
//                // Comprobar si es version reciente
//                boolean isRecentVersion = version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") || version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20") || version.contains("1.21");
//                if (isRecentVersion) {
//                    mainGUI.openInventory(player, "main"); // Abrir el inventario si tiene version reciente
//                } else {
//                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
//                }
//                return true;
//            }
//
//            String action = args[0].toLowerCase();
//
//            // Si el action es "gui", verificar que haya un segundo argumento
//            if (action.equals("gui")) {
//
//                if (args.length < 2) {
//                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
//                    return true;
//                }
//
//                String guiName = args[1].toLowerCase();
//
//                if (!mainGUI.guiExists(guiName)) {
//                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
//                    return true;
//                }
//
//                try {
//                    mainGUI.openInventory(player, guiName);
//                } catch (Exception e) {
//                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
//                }
//                return true;
//            }
//
//            switch (action) {
//                // Delegar al subcomando
//                case "help":
//                    return subCommandHelp.onCommand(player, command, label, args);
//
//                case "data":
//                case "bal":
//                case "balance":
//                    return subCommandData.onCommand(player, command, label, args);
//
//                case "add":
//                case "deposit":
//                    return subCommandAdd.onCommand(player, command, label, args);
//
//                case "take":
//                case "withdraw":
//                    return subCommandTake.onCommand(player, command, label, args);
//
//                case "set":
//                    return subCommandSet.onCommand(player, command, label, args);
//
//                case "levelup":
//                    return subCommandLevelUp.onCommand(player, command, label, args);
//
//                case "top":
//                case "baltop":
//                case "balancetop":
//                    return subCommandBalTop.onCommand(player, command, label, args);
//
//                case "receive":
//                    return subCommandReceive.onCommand(player, command, label, args);
//
//                default:
//                    sendMessage.send(sender, "bank.general-usage", ph); // Mensaje
//                    break;
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
//            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
//        }
//
//        return true;
//    }
//
//}
