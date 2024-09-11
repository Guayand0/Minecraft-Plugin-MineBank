package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class SubComandoTake implements CommandExecutor {

    private final MineBank plugin;
    private final Economy economy;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    int amount, maxStorage, interestPercentage, totalAmount;
    private int minAmountToLose;

    public SubComandoTake(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
    }
    public void updateConfig() {
        this.minAmountToLose = plugin.getConfig().getInt("bank.interests.min-amount-to-lose", 500);
        interestPercentage = plugin.getConfig().getInt("bank.interests.withdraw", 0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco
        plugin.reloadConfig();
        updateConfig();

        Player player = (Player) sender;

        if (args.length < 2) {
            takeUsage(player);
            return true;
        }

        String amountString = args[1];

        try {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            int bankBalance = bankConfig.getInt(playerPath + ".balance", 0);
            int level = bankConfig.getInt(playerPath + ".level", 1);
            maxStorage = getMaxStorageAmount(level);

            // Determinar la cantidad a retirar según el comando
            if (amountString.equalsIgnoreCase("mid")) {
                amount = bankBalance / 2;
            } else if (amountString.equalsIgnoreCase("max")) {
                amount = bankBalance;
            } else if (amountString.equalsIgnoreCase("midmax")) {
                amount = maxStorage / 2;
            } else {
                amount = Integer.parseInt(amountString);
                if (amount <= 0){
                    withdrawFailure(player);
                    return true;
                }
            }

            // Calcular el total a recibir con intereses
            double interest = amount * (interestPercentage / 100.0);
            totalAmount = amount - (int) Math.round(interest);

            // Verificar si el total a recibir es válido
            if (amount > maxStorage || totalAmount > maxStorage || amount > bankBalance || amount <= 0 || totalAmount <= 0) {
                withdrawExceeds(player);
                return true;
            }

            // Comprueba el minimo para tener intereses
            if(amount <= minAmountToLose) { totalAmount = amount; }

            // Realizar el retiro
            economy.depositPlayer(player, totalAmount);
            bankConfig.set(playerPath + ".balance", bankBalance - amount);
            bankManager.saveBank(); // Guardar los cambios en la configuración del banco

            withdrawSuccess(player);
        } catch (NumberFormatException e) {
            withdrawFailure(player);
        }
        return true;
    }

    private void takeUsage(Player player) {
        String message = languageManager.getMessage("bank.usage.take");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private void withdrawFailure(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawFailure");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private void withdrawExceeds(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawExceeds");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
        }
    }

    private void withdrawSuccess(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawSuccess");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replace("%amount%", String.valueOf(totalAmount)).replace("%percentage%", String.valueOf(interestPercentage)).replace("%totalDeducted%", String.valueOf(amount));
            player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
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
