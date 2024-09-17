package mb.Guayando.commands;

import mb.Guayando.managers.*;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import mb.Guayando.MineBank;
import mb.Guayando.utils.MessageUtils;

import java.util.List;

public class ComandoPrincipal implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;

    public ComandoPrincipal(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        plugin.reloadConfig();

        Player player = (Player) sender;

        if (!(player instanceof Player)) {
            // Consola
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    subCommandReload(player);
                    return true;
                }
                consoleError(player);
                return true;
            }
            consoleError(player);
            return true;
        }

        if (!player.hasPermission("minebank.admin")) {
            noPerm(player);
            return true;
        }

        // /minebank args[0] args[1] args[2]
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                // minebank reload
                subCommandReload(player);
            } else if (args[0].equalsIgnoreCase("help")) {
                // minebank help
                help(player);
            } else if (args[0].equalsIgnoreCase("version")) {
                // minebank version
                subCommandVersion(player);
            } else if (args[0].equalsIgnoreCase("author")) {
                // minebank author
                subCommandAutor(player);
            }else if (args[0].equalsIgnoreCase("plugin")) {
                // minebank plugin
                subCommandPlugin(player);
            } else if (args[0].equalsIgnoreCase("permissions")) {
                // minebank permissions
                subCommandPermissions(player);
            } else {
                noArg(player); // minebank qwewe
            }
        } else {
            noArg(player); // minebank
        }
        return true;
    }

    public void help(Player player) {
        String messagePath = "messages.help";
        MessageUtils.sendMessageListWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void subCommandReload(Player player) {
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        plugin.reloadConfig();
        plugin.scheduleBankTask(); // Reiniciar la tarea del banco después de recargar la configuración

        String messagePath = "messages.reload";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void subCommandVersion(Player player) {
        String messagePath = "messages.version";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void subCommandAutor(Player player) {
        String messagePath = "messages.author";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void subCommandPlugin(Player player){
        player.sendMessage(MessageUtils.applyPlaceholdersAndColor(player, null, "%plugin% &7%link%", plugin, 0));
    }

    public void subCommandPermissions(Player player){
        String messagePath = "messages.permissions";
        MessageUtils.sendMessageListWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void noPerm(Player player){
        String messagePath = "messages.no-perm";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void noArg(Player player){
        String messagePath = "messages.command-no-argument";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void consoleError(Player player){
        String messagePath = "messages.console-error";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }
}