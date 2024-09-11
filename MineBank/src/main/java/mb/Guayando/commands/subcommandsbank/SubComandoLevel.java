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

public class SubComandoLevel implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private String targetPlayerName;

    public SubComandoLevel(MineBank plugin) {
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

        if (args.length > 1) {
            targetPlayerName = args[1];
        } else {
            targetPlayerName = null;
        }

        if (targetPlayerName != null) {
            String uuid = getPlayerUUIDByName(targetPlayerName);
            String name = targetPlayerName;

            // Verifica si el UUID del jugador es válido
            if (uuid == null) {
                notFoundPlayer(player);
                return true;
            }

            String playerPath = "bank." + uuid + "." + name;
            int level = bankConfig.getInt(playerPath + ".level", 1);
            playerLevel(player, name, level);
        } else {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            int level = bankConfig.getInt(playerPath + ".level", 1);
            yourLevel(player, level);
        }
        return true;
    }

    private void yourLevel(Player player, int level) {
        String message = languageManager.getMessage("bank.level.yourLevel");
        if (message != null) {
            message = message.replace("%plugin%", MineBank.prefix).replace("%level%", String.valueOf(level));
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private void playerLevel(Player player, String targetPlayerName, int level) {
        String message = languageManager.getMessage("bank.level.playerLevel");
        if (message != null) {
            message = message.replace("%plugin%", MineBank.prefix).replace("%player%", targetPlayerName).replace("%level%", String.valueOf(level));
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private void notFoundPlayer(Player player) {
        String message = languageManager.getMessage("bank.notFoundPlayer");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private String getPlayerUUIDByName(String playerName) {
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración actualizada
        // Recorre todas las entradas en bank.yml para encontrar el UUID correspondiente al nombre del jugador
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String name : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                if (name.equalsIgnoreCase(playerName)) {
                    return uuid;
                }
            }
        }
        return null; // Retorna null si no se encuentra el jugador
    }
}
