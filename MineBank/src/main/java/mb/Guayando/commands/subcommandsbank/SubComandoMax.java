package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

public class SubComandoMax implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private int maxStorage;
    private int maxLevel;

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
            maxUsage(sender);
            return true;
        }

        String type = args[1];

        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco

        if (type.equalsIgnoreCase("bal") || type.equalsIgnoreCase("balance")) {
            int level = bankConfig.getInt(playerPath + ".level", 1);
            maxStorage = getMaxStorageAmount(level);
            maxStorage(player);
        } else if (type.equalsIgnoreCase("level") || type.equalsIgnoreCase("lvl")) {
            maxLevel = getMaxBankLevel();
            maxLevelBank(player);
        } else {
            maxUsage(player);
        }

        return true;
    }

    private void maxUsage(CommandSender sender) {
        String message = languageManager.getMessage("bank.usage.max");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void maxStorage(Player player) {
        String message = languageManager.getMessage("bank.max.maxStorage");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxStorage%", String.valueOf(maxStorage));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void maxLevelBank(Player player) {
        String message = languageManager.getMessage("bank.max.maxLevel");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxLevel%", String.valueOf(maxLevel));
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
}
