package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class SubComandoTake implements CommandExecutor {

    private final MineBank plugin;
    private final Economy economy;
    private final BankManager bankManager;
    private int amount, interestPercentage, minAmountToLose;

    public SubComandoTake(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.bankManager = plugin.getBankManager();
    }
    public void updateConfig() {
        this.minAmountToLose = plugin.getConfig().getInt("bank.interests.min-amount-to-lose", 500);
        this.interestPercentage = plugin.getConfig().getInt("bank.interests.withdrawPercentage", 0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco
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
            String bankName = bankConfig.getString(playerPath + ".bank-name");
            int maxStorage = MethodUtils.getMaxStorageAmount(plugin, bankName, level);

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
            int amountReceived = amount - (int) Math.round(interest);

            // Verificar si el total a recibir es válido
            if (amount > maxStorage || amountReceived > maxStorage || amount > bankBalance || amount <= 0 || amountReceived <= 0) {
                withdrawExceeds(player);
                return true;
            }

            // Comprueba el minimo para tener intereses
            if(amount <= minAmountToLose) { amountReceived = amount; }

            // Realizar el retiro
            economy.depositPlayer(player, amountReceived);
            bankConfig.set(playerPath + ".balance", bankBalance - amount);
            bankManager.saveBank(); // Guardar los cambios en la configuración del banco

            withdrawSuccess(player);
        } catch (NumberFormatException e) {
            withdrawFailure(player);
        }
        return true;
    }

    private void takeUsage(Player player) {
        String messagePath = "bank.usage.take";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void withdrawFailure(Player player) {
        String messagePath = "bank.take.withdrawFailure";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void withdrawExceeds(Player player) {
        String messagePath = "bank.take.withdrawExceeds";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void withdrawSuccess(Player player) {
        String messagePath = "bank.take.withdrawSuccess";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, amount);
    }
}