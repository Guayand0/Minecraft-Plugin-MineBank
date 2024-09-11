// plugin.reloadConfig(); // Recargar el archivo de configuracion
// plugin.getLanguageManager().reloadLanguage(); // Recargar el archivo de idioma
// plugin.getBankManager().reloadBank(); // Recargar el archivo de banco
// plugin.getBankInventoryManager().reloadInventory();  // Recargar el inventario del banco
package mb.Guayando.commands;

import mb.Guayando.MineBank;
import mb.Guayando.commands.subcommandsbank.*;
import mb.Guayando.config.BankInventoryManager;
import mb.Guayando.event.BankInventoryEvent;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ComandoBank implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private final BankInventoryManager bankInventoryManager;
    private final FileConfiguration config;
    private final BankInventoryEvent bankInventoryEvent;

    private final SubComandoBalance subComandoBalance;
    private final SubComandoAdd subComandoAdd;
    private final SubComandoTake subComandoTake;
    private final SubComandoMax subComandoMax;
    private final SubComandoBalTop subComandoBalTop;
    private final SubComandoLevel subComandoLevel;
    private final SubComandoLevelUp subComandoLevelUp;
    private final SubComandoSet subComandoSet;
    private final SubComandoHelp subComandoHelp;

    public ComandoBank(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.bankInventoryManager = plugin.getBankInventoryManager();
        this.config = plugin.getConfig();
        this.bankInventoryEvent = new BankInventoryEvent(plugin); // Inicializar BankInventoryEvent
        plugin.getServer().getPluginManager().registerEvents(bankInventoryEvent, plugin); // Registrar el evento

        this.subComandoBalance = new SubComandoBalance(plugin);
        this.subComandoAdd = new SubComandoAdd(plugin);
        this.subComandoTake = new SubComandoTake(plugin);
        this.subComandoMax = new SubComandoMax(plugin);
        this.subComandoBalTop = new SubComandoBalTop(plugin);
        this.subComandoLevel = new SubComandoLevel(plugin);
        this.subComandoLevelUp = new SubComandoLevelUp(plugin);
        this.subComandoSet = new SubComandoSet(plugin);
        this.subComandoHelp = new SubComandoHelp(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco
        plugin.reloadConfig();
        bankInventoryManager.reloadInventory();

        // Obtener el valor actualizado del booleano
        boolean bankUse = plugin.getConfig().getBoolean("config.bank-use");
        Player player = (Player) sender;
        if (!bankUse) {
            bankDisabled(player);
            return true;
        }

        if (!player.hasPermission("minebank.use") && !player.hasPermission("minebank.admin")) {
            noPerm(player);
            return true;
        }

        if (!(player instanceof Player)) {
            notPlayer(player);
            return true;
        }
        // Sin argumentos
        if (args.length == 0) {
            // Detectar la versión de Minecraft
            String version = Bukkit.getVersion();
            boolean isRecentVersion = version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") || version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20") || version.contains("1.21");
            if (isRecentVersion) {
                bankInventoryEvent.openBankInventory(player); // Abrir el inventario del banco si tiene version reciente
            } else {
                bankUsage(player);
            }
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("bal") || action.equals("balance")) {
            // Delegar al subcomando
            return subComandoBalance.onCommand(player, command, label, args);
        } else if (action.equals("add") || action.equals("deposit")) {
            return subComandoAdd.onCommand(player, command, label, args);
        } else if (action.equals("take") || action.equals("withdraw")) {
            return subComandoTake.onCommand(player, command, label, args);
        } else if (action.equals("max")) {
            return subComandoMax.onCommand(player, command, label, args);
        } else if (action.equals("top") || action.equals("baltop") || action.equals("balancetop")) {
            return subComandoBalTop.onCommand(player, command, label, args);
        } else if (action.equals("level")) {
            return subComandoLevel.onCommand(player, command, label, args);
        } else if (action.equals("levelup")) {
            return subComandoLevelUp.onCommand(player, command, label, args);
        } else if (action.equals("set")) {
            if (!player.hasPermission("minebank.admin")) {
                noPerm(player);
                return true;
            }
            return subComandoSet.onCommand(player, command, label, args);
        } else if (action.equals("help")) {
            return subComandoHelp.onCommand(player, command, label, args);
        } else {
            bankUsage(player);
        }
        // Aquí puedes añadir más lógica si el comando es ejecutado por un jugador.
        return true;
    }

    private void bankDisabled(Player player){
        String message = languageManager.getMessage("config.bank-disabled");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }
    public void bankUsage(Player player){
        String message = languageManager.getMessage("bank.usage.bank");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }
    private void notPlayer(Player player) {
        String message = languageManager.getMessage("bank.notPlayer");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }
    private void noPerm(Player player){
        String message = languageManager.getMessage("messages.no-perm");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }
}