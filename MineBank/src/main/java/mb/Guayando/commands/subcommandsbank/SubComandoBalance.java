package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class SubComandoBalance implements CommandExecutor {

    private String targetPlayerName;
    private final LanguageManager languageManager;
    private final BankManager bankManager;

    public SubComandoBalance(MineBank plugin) {
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco

        Player player = (Player) sender;

        if (args.length > 1) {
            targetPlayerName = args[1];
        } else {
            targetPlayerName = null;
        }

        // Lógica para obtener el balance
        if (targetPlayerName == null) {
            yourBalance(player);
        } else {
            String uuid = getPlayerUUIDByName(targetPlayerName);
            if (uuid != null) {
                playerBalance(uuid, targetPlayerName, player);
            } else {
                notFoundPlayer(player);
            }
        }

        return true;
    }

    public void yourBalance(Player player) {
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración actualizada
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        int balance = bankConfig.getInt(playerPath + ".balance", 0);
        String message = languageManager.getMessage("bank.bal.yourBalance");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%balance%", String.valueOf(balance));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void playerBalance(String uuid, String targetPlayerName, Player player) {
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración actualizada
        String playerPath = "bank." + uuid + "." + targetPlayerName + ".balance";
        int balance = bankConfig.getInt(playerPath, 0);

        String message = languageManager.getMessage("bank.bal.playerBalance");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName).replaceAll("%balance%", String.valueOf(balance));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void notFoundPlayer(Player player) {
        String message = languageManager.getMessage("bank.notFoundPlayer");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName);
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private String getPlayerUUIDByName(String playerName) {
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración actualizada
        // Recorre todas las entradas en bank.yml para encontrar el UUID correspondiente al nombre del jugador
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            ConfigurationSection playerSection = bankConfig.getConfigurationSection("bank." + uuid);
            for (String name : playerSection.getKeys(false)) {
                if (name.equalsIgnoreCase(playerName)) {
                    return uuid;
                }
            }
        }
        return null; // Retorna null si no se encuentra el jugador
    }
}
