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

        if (!bankUse) {
            bankDisabled(sender);
            return true;
        }

        if (!sender.hasPermission("minebank.use") && !sender.hasPermission("minebank.admin")) {
            noPerm(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            notPlayer(sender);
            return true;
        }
        // Sin argumentos
        if (args.length == 0) {
            // Si no se pasan argumentos, mostrar el mensaje de uso
            //bankUsage(sender);

            // Detectar la versión de Minecraft
            //String version = Bukkit.getVersion();
            //boolean isRecentVersion = version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") || version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20") || version.contains("1.21");
            //if (isRecentVersion) {
            if (isVersionAtLeast(13.0)) {
                bankInventoryEvent.openBankInventory((Player) sender); // Abrir el inventario del banco si tiene version reciente
            } else {
                bankUsage(sender);
            }
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("balance") || action.equals("bal")) {
            // Delegar al subcomando
            return subComandoBalance.onCommand(sender, command, label, args);
        } else if (action.equals("add") || action.equals("deposit")) {
            return subComandoAdd.onCommand(sender, command, label, args);
        } else if (action.equals("take") || action.equals("withdraw")) {
            return subComandoTake.onCommand(sender, command, label, args);
        } else if (action.equals("max")) {
            return subComandoMax.onCommand(sender, command, label, args);
        } else if (action.equals("top") || action.equals("baltop") || action.equals("balancetop")) {
            return subComandoBalTop.onCommand(sender, command, label, args);
        } else if (action.equals("level") || action.equals("lvl")) {
            return subComandoLevel.onCommand(sender, command, label, args);
        } else if (action.equals("levelup")) {
            return subComandoLevelUp.onCommand(sender, command, label, args);
        } else if (action.equals("set")) {
            if (!sender.hasPermission("minebank.admin")) {
                noPerm(sender);
                return true;
            }
            return subComandoSet.onCommand(sender, command, label, args);
        } else if (action.equals("help")) {
            return subComandoHelp.onCommand(sender, command, label, args);
        } else {
            bankUsage(sender);
        }
        // Aquí puedes añadir más lógica si el comando es ejecutado por un jugador.
        return true;
    }

    private boolean isVersionAtLeast(double minVersion) {
        String versionString = Bukkit.getServer().getClass().getPackage().getName();
        String version = versionString.substring(versionString.lastIndexOf('v') + 1);
        double serverVersion = Double.parseDouble(version.substring(2, version.lastIndexOf('_')) + "." + version.charAt(version.length() - 1));
        return serverVersion >= minVersion;
    }

    private void bankDisabled(CommandSender sender){
        String mensaje = languageManager.getMessage("config.bank-disabled");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
    public void bankUsage(CommandSender sender){
        String mensaje = languageManager.getMessage("bank.usage.bank");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
    private void notPlayer(CommandSender sender) {
        String mensaje = languageManager.getMessage("bank.notPlayer");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
    private void noPerm(CommandSender sender){
        String mensaje = languageManager.getMessage("messages.no-perm");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
}