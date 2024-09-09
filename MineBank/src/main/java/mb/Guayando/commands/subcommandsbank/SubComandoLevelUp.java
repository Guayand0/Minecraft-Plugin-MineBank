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

public class SubComandoLevelUp implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    int unlockPrice, bankBalance, level;

    public SubComandoLevelUp(MineBank plugin) {
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
        FileConfiguration config = plugin.getConfig(); // Obtener la configuración principal

        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        level = bankConfig.getInt(playerPath + ".level", 1);
        String levelData = config.getString("bank.level." + level);

        // Check if the next level exists
        String nextLevelData = config.getString("bank.level." + (level + 1));
        if (nextLevelData == null) {
            alreadyMaxLevel(player);
            return true;
        }

        if (levelData == null) {
            unlockPriceError(player);
            return true;
        }

        String[] parts = levelData.split(";");
        if (parts.length < 2) {
            unlockPriceError(player);
            return true;
        }

        try {
            unlockPrice = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            unlockPriceError(player);
            return true;
        }

        bankBalance = bankConfig.getInt(playerPath + ".balance", 0);

        if (bankBalance >= unlockPrice) {
            // Deduct from bank balance
            bankConfig.set(playerPath + ".balance", bankBalance - unlockPrice);
            // Increase the player's bank level
            bankConfig.set(playerPath + ".level", level + 1);
            plugin.getBankManager().saveBank(); // Guardar la configuración del banco
            successLevelUp(player);
        } else {
            // Not enough money in bank balance
            notEnoughMoneyLevelUp(player);
        }

        return true;
    }

    private void successLevelUp(Player player) {
        String message = languageManager.getMessage("bank.levelup.success");
        if (message != null) {
            message = message.replace("%plugin%", MineBank.prefix).replaceAll("%newLevel%", String.valueOf(level + 1));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void notEnoughMoneyLevelUp(Player player) {
        String message = languageManager.getMessage("bank.levelup.notEnoughMoney");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%unlockPrice%", String.valueOf(unlockPrice).replaceAll("%balance%", String.valueOf(bankBalance)));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void alreadyMaxLevel(Player player) {
        String message = languageManager.getMessage("bank.levelup.alreadyMax");
        if (message != null) {
            message = message.replace("%plugin%", MineBank.prefix);
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void unlockPriceError(Player player) {
        String message = languageManager.getMessage("bank.levelup.unlockPriceError");
        if (message != null) {
            message = message.replace("%plugin%", MineBank.prefix).replaceAll("%unlockLevel%", String.valueOf(level + 1));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }
}
