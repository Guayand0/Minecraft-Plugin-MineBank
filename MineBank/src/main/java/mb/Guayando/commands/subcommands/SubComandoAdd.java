package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.utils.*;
import mb.Guayando.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SubComandoAdd implements CommandExecutor {

    private final MineBank plugin;
    private final Economy economy;
    private final BankManager bankManager;
    int amount;

    public SubComandoAdd(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        if (args.length < 2) {
            bankAdd(player);
            return true;
        }

        String amountString = args[1];

        try {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco

            int playerBalance = (int) economy.getBalance(player);
            int bankBalance = bankConfig.getInt(playerPath + ".balance", 0);
            int level = bankConfig.getInt(playerPath + ".level", 1);
            String bankName = bankConfig.getString(playerPath + ".bank-name");
            int maxStorage = MethodUtils.getMaxStorageAmount(plugin, bankName, level);

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

    public void bankAdd(Player player){
        String messagePath = "bank.usage.add";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void balanceExceeds(Player player) {
        String messagePath = "bank.add.balanceExceeds";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void depositFailure(Player player) {
        String messagePath = "bank.add.depositFailure";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void notEnoughMoneyAdd(Player player) {
        String messagePath = "bank.add.notEnoughMoney";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void depositSuccess(Player player) {
        String messagePath = "bank.add.depositSuccess";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, amount, MineBank.getPlaceholderAPI());
    }
}
