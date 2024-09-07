package mb.Guayando.commands.subcommandsbank;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.utils.MessageUtils;
import mb.Guayando.config.LanguageManager;
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
            takeUsage(sender);
            return true;
        }

        String amountString = args[1];

        try {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();


            int bankBalance = bankConfig.getInt(playerPath + ".balance", 0);
            int level = bankConfig.getInt(playerPath + ".level", 1);
            maxStorage = getMaxStorageAmount(level);

            if (amountString.equalsIgnoreCase("max")) {
                // Calcula la máxima cantidad que puede retirarse sin exceder el maxStorage con intereses
                double maxWithdrawableAmount = bankBalance / (1 + (double) interestPercentage / 100);
                amount = (int) Math.floor(maxWithdrawableAmount); // Redondear hacia abajo para evitar exceder el límite

                if (amount <= 0) {
                    withdrawFailure(player);
                    return true;
                }
            } else if (amountString.equalsIgnoreCase("mid")) {
                // Calcula la mitad de la capacidad máxima del banco
                amount = maxStorage/2; // Asignar la mitad de la capacidad máxima

                if (amount > bankBalance) {
                    withdrawExceeds(player);
                    return true;
                }

                if (amount <= 0) {
                    withdrawFailure(player);
                    return true;
                }
            } else {
                amount = Integer.parseInt(amountString);
                if (amount <= 0) {
                    withdrawFailure(player);
                    return true;
                }
            }

            // Calcular el total a retirar incluyendo intereses
            double interest = amount * ((double) interestPercentage / 100);
            totalAmount = amount + (int) Math.round(interest);

            if (bankBalance < amount) {
                withdrawExceeds(player);
                return true;
            }

            // Verificar si el total a retirar excede el maxStorage
            if (totalAmount > maxStorage) {
                withdrawExceeds(player);
                return true;
            }

            // Verificar si el saldo después de la retirada sería negativo
            if (bankBalance - totalAmount < 0) {
                interestsWithdrawExceeds(player);
                return true;
            }

            // Verificar si el monto es menor o igual a minAmountToLose o si minAmountToLose es negativo
            if (amount <= minAmountToLose || minAmountToLose < 0){
                totalAmount = amount;
                economy.depositPlayer(player, amount);
                bankConfig.set(playerPath + ".balance", bankBalance - totalAmount);
                bankManager.saveBank(); // Guardar los cambios en la configuración del banco

                withdrawSuccess(player);
                return true;
            }

            // Realizar el retiro
            economy.depositPlayer(player, amount);
            bankConfig.set(playerPath + ".balance", bankBalance - totalAmount);
            bankManager.saveBank(); // Guardar los cambios en la configuración del banco

            withdrawSuccess(player);
        } catch (NumberFormatException e) {
            withdrawFailure(player);
        }
        return true;
    }

    private void takeUsage(CommandSender sender) {
        String message = languageManager.getMessage("bank.usage.take");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void withdrawFailure(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawFailure");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void withdrawExceeds(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawExceeds");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void interestsWithdrawExceeds(Player player) {
        String message = languageManager.getMessage("bank.take.interestsWithdrawExceeds");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix);
            player.sendMessage(MessageUtils.getColoredMessage(message));
        }
    }

    private void withdrawSuccess(Player player) {
        String message = languageManager.getMessage("bank.take.withdrawSuccess");
        if (message != null) {
            message = message.replaceAll("%plugin%", MineBank.prefix).replace("%amount%", String.valueOf(amount)).replace("%percentage%", String.valueOf(interestPercentage)).replace("%totalDeducted%", String.valueOf(totalAmount));
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
