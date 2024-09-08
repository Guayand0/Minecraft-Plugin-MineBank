package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SubComandoAdd implements CommandExecutor {

    private final MineBank plugin;
    private final Economy economy;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    int amount, maxStorage;

    public SubComandoAdd(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco

        Player player = (Player) sender;
        if (args.length < 2) {
            bankAdd(sender);
            return true;
        }

        String amountString = args[1];

        try {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco

            int playerBalance = (int) economy.getBalance(player);
            int bankBalance = bankConfig.getInt(playerPath + ".balance", 0);
            int level = bankConfig.getInt(playerPath + ".level", 1);
            maxStorage = getMaxStorageAmount(level);

            if (amountString.equalsIgnoreCase("mid") || amountString.equalsIgnoreCase("midmax") || amountString.equalsIgnoreCase("max")) {
                // Asignar la cantidad basada en el tipo de comando
                if (amountString.equalsIgnoreCase("mid")) {
                    amount = bankBalance / 2; // Mitad del balance actual
                } else if (amountString.equalsIgnoreCase("max")) {
                    amount = maxStorage - bankBalance; // Espacio restante en el banco
                } else if (amountString.equalsIgnoreCase("midmax")) {
                    amount = maxStorage / 2; // Mitad del almacenamiento máximo
                }
                // Verificar si el jugador tiene suficiente dinero
                if (playerBalance < amount) {
                    amount = playerBalance; // Si no tiene suficiente, usar todo su dinero
                }
            } else {
                amount = Integer.parseInt(amountString); // Convertir el string a entero
                // Verificar que la cantidad calculada sea válida
                if (amount <= 0) {
                    depositFailure(player);
                    return true;
                }
                // Verificar si el jugador tiene suficiente dinero
                if (playerBalance < amount) {
                    notEnoughMoneyAdd(player);
                    return true;
                }
            }

            // Verificar si el balance actual más la cantidad a depositar excede el almacenamiento máximo
            if ((bankBalance + amount) > maxStorage || amount <= 0) {
                balanceExceeds(player);
                return true;
            }

            // Retirar el dinero del jugador
            economy.withdrawPlayer(player, amount);
            // Actualizar el saldo del banco del jugador
            bankConfig.set(playerPath + ".balance", (bankBalance + amount));
            bankManager.saveBank(); // Guardar los cambios en la configuración del banco

            depositSuccess(player);
        } catch (NumberFormatException e) {
            depositFailure(player);
        }
        return true;
    }

    public void bankAdd(CommandSender sender){
        String message = languageManager.getMessage("bank.usage.add");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void balanceExceeds(Player player) {
        String message = languageManager.getMessage("bank.add.balanceExceeds");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%maxStorage%", String.valueOf(maxStorage));
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void depositFailure(Player player) {
        String message = languageManager.getMessage("bank.add.depositFailure");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void notEnoughMoneyAdd(Player player) {
        String message = languageManager.getMessage("bank.add.notEnoughMoney");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void depositSuccess(Player player) {
        String message = languageManager.getMessage("bank.add.depositSuccess");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%amount%", String.valueOf(amount));
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
}
