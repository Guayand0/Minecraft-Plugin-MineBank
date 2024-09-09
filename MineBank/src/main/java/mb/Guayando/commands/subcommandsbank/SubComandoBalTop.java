package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class SubComandoBalTop implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;

    public SubComandoBalTop(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco

        Player player = (Player) sender;
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco

        // Crear un mapa para almacenar los balances de los jugadores
        Map<String, Integer> topBalances = new HashMap<>();

        // Iterar sobre todos los jugadores y sus balances
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                topBalances.put(playerName, balance);
            }
        }

        // Ordenar los balances en orden descendente
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // Enviar el título del top a los jugadores
        title(player);

        // Mostrar los 10 primeros jugadores con mayores balances
        for (int i = 0; i < Math.min(10, sortedList.size()); i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            String baltop = languageManager.getMessage("bank.top.entry");
            if (baltop != null) {
                baltop = baltop.replace("%plugin%", MineBank.prefix).replace("%position%", String.valueOf(i + 1)).replace("%player%", entry.getKey()).replace("%balance%", String.valueOf(entry.getValue()));
                baltop = PlaceholderAPI.setPlaceholders(player, baltop); // Procesar placeholders de PlaceholderAPI
                player.sendMessage(MessageUtils.getColoredMessage(baltop));
            }
        }

        return true;
    }

    private void title(Player player) {
        String title = languageManager.getMessage("bank.top.title");
        if (title != null) {
            title = title.replace("%plugin%", MineBank.prefix);
            title = PlaceholderAPI.setPlaceholders(player, title); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(title));
        }
    }
}
