package com.Guayand0.commands;

import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandPrincipal implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final MessageUtils MU = new MessageUtils();

    public CommandPrincipal(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) {
            // Consola
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    plugin.registrarPluginPlaceholders();
                    languageManager.reloadMessages();
                    languageManager.reloadGui();
                    plugin.updateBankProfitTask();
                    plugin.updateRegisterBankPermissions();

                    for (String message : languageManager.getAllMessage("messages.reload")) {
                        sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, plugin.placeholders));
                    }
                    return true;
                }
                for (String message : languageManager.getAllMessage("messages.console-error")) {
                    sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, plugin.placeholders));
                }
                return true;
            }
            for (String message : languageManager.getAllMessage("messages.console-error")) {
                sender.sendMessage(MU.getColoredReplacePluginPlaceholdersText(message, plugin.placeholders));
            }
            return true;
        }

        Player player = (Player) sender;

        try {
            // Si el jugador no tiene permisos
            if (!player.hasPermission(plugin.pluginName + ".admin")) {
                noPermMessage(player); // Mensaje
                return true;
            }

            // Si el comando tiene 1 o más argumentos
            if (args.length >= 1) {

                if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    plugin.registrarPluginPlaceholders();
                    languageManager.reloadMessages();
                    languageManager.reloadGui();
                    plugin.updateBankProfitTask();
                    plugin.updateRegisterBankPermissions();

                    reloadMessage(player); // Mensaje

                } else if (args[0].equalsIgnoreCase("help")) {
                    helpMessage(player); // Mensaje

                } else if (args[0].equalsIgnoreCase("permissions")) {
                    permissionsMessage(player); // Mensaje

                } else if (args[0].equalsIgnoreCase("info")) {
                    infoMessage(player); // Mensaje

                } else {
                    noArgMessage(player); // Mensaje
                }

            } else {
                noArgMessage(player); // Mensaje
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, "%plugin% &cAn error occurred while processing the command.", plugin.placeholders));
        }

        return true;
    }

    private void helpMessage(Player player) {
        for (String message : languageManager.getAllMessage("messages.help")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void reloadMessage(Player player) {
        for (String message : languageManager.getAllMessage("messages.reload")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void infoMessage(Player player) {
        for (String message : languageManager.getAllMessage("messages.info")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void permissionsMessage(Player player){
        for (String message : languageManager.getAllMessage("messages.permissions")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPermMessage(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noArgMessage(Player player){
        for (String message : languageManager.getAllMessage("messages.command-no-argument")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void consoleError(Player player){
        for (String message : languageManager.getAllMessage("messages.console-error")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}