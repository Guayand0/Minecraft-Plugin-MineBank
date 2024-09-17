package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SubComandoBalTop implements CommandExecutor {

    private final MineBank plugin;

    public SubComandoBalTop(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        // Obtener los mensajes formateados desde MethodUtils
        List<String> topBalancesMessages = MethodUtils.getFormattedTopBalances(plugin, 10);

        // Enviar el título del top a los jugadores
        sendTitle(player);

        // Enviar los mensajes ya formateados al jugador
        for (String message : topBalancesMessages) {
            player.sendMessage(MessageUtils.applyPlaceholdersAndColor(player, null, message, plugin, 0));
        }

        return true;
    }

    private void sendTitle(Player player) {
        String messagePath = "bank.top.title";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }
}