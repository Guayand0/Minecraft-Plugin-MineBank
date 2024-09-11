package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.utils.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        Player player = (Player) sender;
        // Obtener y mostrar los mensajes de ayuda
        List<String> helpMessages = languageManager.getStringList("messages.help-bank");

        if (helpMessages != null) {
            for (String message : helpMessages) {
                player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
            }
        }

        return true;
    }
}
