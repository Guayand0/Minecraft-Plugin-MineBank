package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class SubComandoLevelUp implements CommandExecutor {

    private final MineBank plugin;

    public SubComandoLevelUp(MineBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        FileConfiguration bankConfig = plugin.getBankManager().getBank(); // Obtener la configuración del banco
        FileConfiguration config = plugin.getConfig(); // Obtener la configuración principal

        // Ruta del jugador en la configuración del banco
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();

        // Obtener el nombre del banco y el nivel del jugador
        String bankName = bankConfig.getString(playerPath + ".bank-name");
        int level = bankConfig.getInt(playerPath + ".level", 1);

        // Obtener datos del nivel actual del jugador desde la configuración del plugin
        String levelData = config.getString("bank." + bankName + ".level." + level);
        // Obtener datos del siguiente nivel desde la configuración del plugin
        String nextLevelData = config.getString("bank." + bankName + ".level." + (level + 1));

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

        int unlockPrice;
        try {
            unlockPrice = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            unlockPriceError(player);
            return true;
        }

        // Obtener el balance del jugador
        int bankBalance = bankConfig.getInt(playerPath + ".balance", 0);

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
        String messagePath = "bank.levelup.success";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void notEnoughMoneyLevelUp(Player player) {
        String messagePath = "bank.levelup.notEnoughMoney";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void alreadyMaxLevel(Player player) {
        String messagePath = "bank.levelup.alreadyMax";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    private void unlockPriceError(Player player) {
        String messagePath = "bank.levelup.unlockPriceError";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);

    }
}
