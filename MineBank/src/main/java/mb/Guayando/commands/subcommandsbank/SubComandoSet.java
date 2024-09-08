package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;

public class SubComandoSet implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private String targetPlayerName;
    int amount, level, maxStorage, maxLevel;

    public SubComandoSet(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco
        
        if (args.length < 4) {
            bankSetUsage(sender);
            return true;
        }

        Player player = (Player) sender;
        targetPlayerName = args[1];
        String type = args[2];
        String amountString = args[3];
        String[] changeBalanceString = (args.length > 4) ? new String[]{args[4]} : new String[0];

        String uuid = getPlayerUUIDByName(targetPlayerName);
        if (uuid == null) {
            notFoundPlayer(player);
            return true;
        }

        String playerPath = "bank." + uuid + "." + targetPlayerName;

        if (type.equalsIgnoreCase("bal") || type.equalsIgnoreCase("balance")) {
            try {
                level = bankConfig.getInt(playerPath + ".level", 1);
                maxStorage = getMaxStorageAmount(level);

                if (amountString.equalsIgnoreCase("max")) {
                    amount = maxStorage;
                } else if(amountString.equalsIgnoreCase("midmax")){
                    amount = maxStorage / 2;
                } else {
                    amount = Integer.parseInt(amountString);
                }

                if (amount > maxStorage) {
                    maxBalance(player);
                    return true;
                }

                bankConfig.set(playerPath + ".balance", amount);
                bankManager.saveBank();
                setBalanceSuccess(player);

            } catch (NumberFormatException e) {
                depositFailure(player);
            }
        } else if (type.equalsIgnoreCase("level")) {
            try {
                maxLevel = getMaxBankLevel();

                if (amountString.equalsIgnoreCase("max")) {
                    level = maxLevel;
                } else if(amountString.equalsIgnoreCase("midmax")){
                    level = maxLevel / 2;
                } else {
                    level = Integer.parseInt(amountString);
                }

                if (level > maxLevel) {
                    maxLevel(player);
                    return true;
                }

                // Obtener el nuevo maxStorage basado en el nivel que se va a establecer
                maxStorage = getMaxStorageAmount(level);

                // Cambiar el nivel del banco
                bankConfig.set(playerPath + ".level", level);
                bankManager.saveBank();
                setLevelSuccess(player);

                // Manejo de changeBalanceString
                if (changeBalanceString.length > 0) {
                    boolean changeBalance = Boolean.parseBoolean(changeBalanceString[0]);

                    if (changeBalance) {
                        // Si es true, establecer el balance al maxStorage del nuevo nivel
                        amount = maxStorage;
                        bankConfig.set(playerPath + ".balance", maxStorage);
                        bankManager.saveBank();
                        setBalanceSuccess(player);
                    } else {
                        // Si es false, ajustar el balance si excede el maxStorage
                        int currentBalance = bankConfig.getInt(playerPath + ".balance", 0);
                        if (currentBalance > maxStorage) {
                            amount = maxStorage;
                            bankConfig.set(playerPath + ".balance", maxStorage);
                        } else {
                            amount = currentBalance; // Mantener el balance actual si no excede maxStorage
                        }
                        bankManager.saveBank();
                        setBalanceSuccess(player);
                    }
                }

            } catch (NumberFormatException e) {
                depositFailure(player);
            }
        } else {
            bankSetUsage(sender);
        }
        return true;
    }

    private void notFoundPlayer(Player player) {
        String message = languageManager.getMessage("bank.playerNotFound");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void depositFailure(Player player) {
        String message = languageManager.getMessage("bank.set.depositFailure");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void maxBalance(Player player) {
        String message = languageManager.getMessage("bank.set.maxBalance");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxStorage%", String.valueOf(maxStorage));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void maxLevel(Player player) {
        String message = languageManager.getMessage("bank.set.maxLevel");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxLevel%", String.valueOf(maxLevel));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void setBalanceSuccess(Player player) {
        String message = languageManager.getMessage("bank.set.setBalanceSuccess");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName).replace("%amount%", String.valueOf(amount));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void setLevelSuccess(Player player) {
        String message = languageManager.getMessage("bank.set.setLevelSuccess");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%player%", targetPlayerName).replace("%level%", String.valueOf(level));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void bankSetUsage(CommandSender sender) {
        String message = languageManager.getMessage("bank.usage.set");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(message));
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
