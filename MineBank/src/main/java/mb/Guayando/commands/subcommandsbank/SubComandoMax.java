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

import java.util.Set;

public class SubComandoMax implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private int maxStorage;
    private int maxLevel;
    private String targetPlayerName;

    public SubComandoMax(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco

        Player player = (Player) sender;

        if (args.length < 2) {
            maxUsage(player);
            return true;
        }

        String type = args[1];
        String playerPath;
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco

        if (type.equalsIgnoreCase("bal") || type.equalsIgnoreCase("balance")) {
            if (args.length >= 3) {
                targetPlayerName = args[2];
            } else {
                targetPlayerName = null;
            }
            // Lógica para obtener el balance
            if (targetPlayerName == null) {
                playerPath = "bank." + player.getUniqueId() + "." + player.getName();
                int level = bankConfig.getInt(playerPath + ".level", 1);
                maxStorage = getMaxStorageAmount(level);
                yourMaxStorage(player);
            } else {
                String uuid = getPlayerUUIDByName(targetPlayerName);
                if (uuid != null) {
                    playerPath = "bank." + uuid + "." + targetPlayerName;
                    int level = bankConfig.getInt(playerPath + ".level", 1);
                    maxStorage = getMaxStorageAmount(level);
                    playerMaxStorage(targetPlayerName, player);
                } else {
                    notFoundPlayer(player);
                }
            }
        } else if (type.equalsIgnoreCase("level")) {
            maxLevel = getMaxBankLevel();
            maxLevelBank(player);
        } else {
            maxUsage(player);
        }

        return true;
    }

    private void maxUsage(Player player) {
        String message = languageManager.getMessage("bank.usage.max");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void yourMaxStorage(Player player) {
        String message = languageManager.getMessage("bank.max.yourMaxStorage");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxStorage%", String.valueOf(maxStorage));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void playerMaxStorage(String targetPlayerName, Player player) {
        String message = languageManager.getMessage("bank.max.playerMaxStorage");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName).replaceAll("%maxStorage%", String.valueOf(maxStorage));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void maxLevelBank(Player player) {
        String message = languageManager.getMessage("bank.max.maxLevel");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxLevel%", String.valueOf(maxLevel));
            message = PlaceholderAPI.setPlaceholders(player, message); // Procesar placeholders de PlaceholderAPI
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private int getMaxStorageAmount(int level) {
        String levelData = plugin.getConfig().getString("bank.level." + level);

        if (levelData != null && levelData.contains(";")) {
            String[] parts = levelData.split(";");
            if (parts.length > 0) {
                try {
                    return Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    private int getMaxBankLevel() {
        int maxLevel = 1;
        Set<String> levels = plugin.getConfig().getConfigurationSection("bank.level").getKeys(false);
        for (String level : levels) {
            try {
                int levelNumber = Integer.parseInt(level);
                if (levelNumber > maxLevel) {
                    maxLevel = levelNumber;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return maxLevel;
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
