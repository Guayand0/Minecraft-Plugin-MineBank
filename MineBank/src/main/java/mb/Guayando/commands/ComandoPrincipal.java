package mb.Guayando.commands;

import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
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
    private int profitPercentage, minAmountToWin, profitInterval, minAmountToLose, interestsWithdraw;
    private boolean bankUseEnabled;

    private final FileConfiguration config;

    public ComandoPrincipal(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.config = plugin.getConfig();

        updateConfig();
    }
    private void updateConfig() {
        this.minAmountToWin = plugin.getConfig().getInt("bank.profit.min-amount-to-win", 100);
        this.profitPercentage = plugin.getConfig().getInt("bank.profit.keep-in-bank", 1);
        this.profitInterval = plugin.getConfig().getInt("bank.profit.interval", 300);
        this.bankUseEnabled = plugin.getConfig().getBoolean("config.bank-use", true);
        this.minAmountToLose = plugin.getConfig().getInt("bank.interests.min-amount-to-lose", 500);
        this.interestsWithdraw = plugin.getConfig().getInt("bank.interests.withdraw", 2);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        plugin.reloadConfig();
        updateConfig();

        Player player = (Player) sender;
        if (!(player instanceof Player)) {
            // Consola
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    languageManager.reloadLanguage();
                    bankManager.reloadBank(); // Recargar la configuración del banco
                    plugin.reloadConfig();

                    // Reiniciar la tarea del banco después de recargar la configuración
                    plugin.scheduleBankTask();

                    Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(MineBank.prefix + "&aReload complete."));
                    return true;
                }

                consoleError(player); // messages.console-error
                return true;
            }
            consoleError(player); // messages.console-error
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
        List<String> helpMessages = languageManager.getStringList("messages.help");
        if (helpMessages != null) {
            for(String m : helpMessages){
                m = PlaceholderAPI.setPlaceholders(player, m); // Procesar placeholders de PlaceholderAPI
                player.sendMessage(MessageUtils.getColoredMessage(m));
            }
        }
    }

    public void subCommandReload(Player player) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco
        plugin.reloadConfig();

        // Reiniciar la tarea del banco después de recargar la configuración
        plugin.scheduleBankTask();

        String mensaje = languageManager.getMessage("messages.reload");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }

    public void subCommandVersion(Player player) {
        String mensaje = languageManager.getMessage("messages.version");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix).replaceAll("%version%", plugin.version).replaceAll("%latestversion%", plugin.getLatestVersion());
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }

    public void subCommandAutor(Player player) {
        String mensaje = languageManager.getMessage("messages.author");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix).replaceAll("%author%", plugin.getDescription().getAuthors().toString());
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }

    public void subCommandPlugin(Player player){
        String mensaje = "%plugin% &7https://www.spigotmc.org/resources/119147/".replaceAll("%plugin%", MineBank.prefix);
        mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
        player.sendMessage(MessageUtils.getColoredMessage(mensaje));
    }

    public void subCommandPermissions(Player player){
        List<String> permMessages = languageManager.getStringList("messages.permissions");
        if (permMessages != null) {
            for(String m : permMessages){
                m = PlaceholderAPI.setPlaceholders(player, m); // Procesar placeholders de PlaceholderAPI
                player.sendMessage(MessageUtils.getColoredMessage(m));
            }
        }
    }

    public void noPerm(Player player){
        String mensaje = languageManager.getMessage("messages.no-perm");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
    public void noArg(Player player){
        String mensaje = languageManager.getMessage("messages.command-no-argument");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
    public void consoleError(Player player){
        String mensaje = languageManager.getMessage("messages.console-error");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            mensaje = PlaceholderAPI.setPlaceholders(player, mensaje); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
}