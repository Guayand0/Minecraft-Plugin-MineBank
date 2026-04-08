package com.Guayand0.commands;

import com.Guayand0.MineBank;
import com.Guayand0.commands.minebanksubcommands.ItemSubCommand;
import com.Guayand0.commands.minebanksubcommands.BackupSubCommand;
import com.Guayand0.commands.minebanksubcommands.BankSubCommand;
import com.Guayand0.commands.minebanksubcommands.EventSubCommand;
import com.Guayand0.commands.minebanksubcommands.MigrateSubCommand;
import com.Guayand0.managers.ConfigMessageKeyUpdater;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.utils.gui.GuiMain;
import com.Guayand0.utils.gui.GuiUtils;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MineBankCommand implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    private final MigrateSubCommand subCommandMigrate;
    private final BackupSubCommand subCommandBackup;
    private final EventSubCommand subCommandEvent;
    private final BankSubCommand subCommandBank;
    private final ItemSubCommand subCommandItem;

    public MineBankCommand(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.sendMessage = new SendMessage(plugin);

        this.subCommandMigrate = new MigrateSubCommand(plugin);
        this.subCommandBackup = new BackupSubCommand(plugin);
        this.subCommandEvent = new EventSubCommand(plugin);
        this.subCommandBank = new BankSubCommand(plugin);
        this.subCommandItem = new ItemSubCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Map<String,String> ph = null;

        if (!(sender instanceof Player)) {
            // Consola
            if (args.length == 0) {
                sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                return true;
            }

            switch (args[0].toLowerCase()) {

                case "reload":
                    reload();
                    sendMessage.send(sender, "messages.reload", ph); // Mensaje
                    return true;

                case "info":
                    sendMessage.send(sender, "messages.info", ph); // Mensaje
                    return true;

                case "permissions":
                    sendMessage.send(sender, "messages.permissions", ph); // Mensaje
                    return true;

                case "backup":
                    return subCommandBackup.onCommand(sender, command, label, args);

                case "event":
                    return subCommandEvent.onCommand(sender, command, label, args);

                default:
                    sendMessage.send(sender, "messages.console-help", ph); // Mensaje
                    return true;
            }
        }

        Player player = (Player) sender;
        ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        try {
            // Si el jugador no tiene permisos
            if (!player.hasPermission(plugin.pluginName + ".admin")) {
                sendMessage.send(sender, "messages.no-perm", ph); // Mensaje
                return true;
            }

            if (args.length == 0) {
                sendMessage.send(sender, "messages.command-no-argument", ph); // Mensaje
            }

            // Si el comando tiene 1 o más argumentos
            if (args.length >= 1) {

                String action = args[0].toLowerCase();

                switch (action) {
                    case "help":
                        sendMessage.send(sender, "messages.help", ph); // Mensaje
                        return true;

                    case "reload":
                        reload();

                        sendMessage.send(sender, "messages.reload", ph); // Mensaje
                        return true;

                    case "info":
                        sendMessage.send(sender, "messages.info", ph); // Mensaje
                        return true;

                    case "permissions":
                        sendMessage.send(sender, "messages.permissions", ph); // Mensaje
                        return true;

                    // Delegar al subcomando
                    case "migrate":
                        if (!player.hasPermission(plugin.pluginName + ".migration")) {
                            sendMessage.send(sender, "messages.no-perm", ph); // Mensaje
                            return true;
                        } else {
                            return subCommandMigrate.onCommand(player, command, label, args);
                        }

                    case "backup":
                        if (!player.hasPermission(plugin.pluginName + ".backup")) {
                            sendMessage.send(sender, "messages.no-perm", ph); // Mensaje
                            return true;
                        } else {
                            return subCommandBackup.onCommand(player, command, label, args);
                        }

                    case "event":
                        return subCommandEvent.onCommand(player, command, label, args);

                    case "bank":
                        return subCommandBank.onCommand(player, command, label, args);

                    case "item":
                        return subCommandItem.onCommand(player, command, label, args);

                    default:
                        sendMessage.send(sender, "messages.command-no-argument", ph); // Mensaje
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }

    private void reload() {
        plugin.reloadConfig();
        ConfigMessageKeyUpdater keyUpdater = new ConfigMessageKeyUpdater(plugin);
        keyUpdater.syncConfig();
        plugin.reloadConfig();
        plugin.registrarPluginPlaceholders();
        keyUpdater.syncMessages();
        languageManager.reloadMessages();
        languageManager.reloadGui();
        new GuiMain(plugin).reloadGuiConfig();
        new GuiUtils(plugin).reloadGUI();
        plugin.updateBankProfitTask();
        plugin.updateRegisterBankPermissionTask();
        plugin.restartWebServer();
    }
}
