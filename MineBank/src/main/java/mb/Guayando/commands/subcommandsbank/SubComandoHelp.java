package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SubComandoHelp implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;

    public SubComandoHelp(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco

        // Obtener y mostrar los mensajes de ayuda
        List<String> helpMessages = languageManager.getStringList("messages.help-bank");

        for (String message : helpMessages) {
            sender.sendMessage(MessageUtils.getColoredMessage(message));
        }

        return true;
    }
}
