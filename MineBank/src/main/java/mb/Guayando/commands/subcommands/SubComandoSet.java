package mb.Guayando.commands.subcommands;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SubComandoSet implements CommandExecutor {

    private final MineBank plugin;
    private final BankManager bankManager;
    int amount, level;

    public SubComandoSet(MineBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        FileConfiguration bankConfig = bankManager.getBank();

        Player player = (Player) sender;

        if (args.length < 4) {
            bankSetUsage(player);
            return true;
        }

        String targetPlayerName = args[1];
        String type = args[2];
        String amountString = args[3];
        String[] changeBalanceString = (args.length > 4) ? new String[]{args[4]} : new String[0];

        String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);
        if (targetUUID == null) {
            notFoundPlayer(player, targetPlayerName);
            return true;
        }

        String playerPath = "bank." + targetUUID + "." + targetPlayerName;
        String bankName = bankConfig.getString(playerPath + ".bank-name");

        if (type.equalsIgnoreCase("bal") || type.equalsIgnoreCase("balance")) {
            try {
                level = bankConfig.getInt(playerPath + ".level", 1);
                int maxStorage = MethodUtils.getMaxStorageAmount(plugin, bankName, level);

                if (amountString.equalsIgnoreCase("max")) {
                    amount = maxStorage;
                } else if(amountString.equalsIgnoreCase("midmax")){
                    amount = maxStorage / 2;
                } else {
                    amount = Integer.parseInt(amountString);
                }

                if (amount > maxStorage) {
                    maxBalance(player, targetPlayerName);
                    return true;
                }

                bankConfig.set(playerPath + ".balance", amount);
                bankManager.saveBank();
                setBalanceSuccess(player, targetPlayerName);

            } catch (NumberFormatException e) {
                depositFailure(player, targetPlayerName);
            }
        } else if (type.equalsIgnoreCase("level")) {
            try {
                int maxLevel = MethodUtils.getMaxBankLevel(plugin, bankName);

                if (amountString.equalsIgnoreCase("max")) {
                    level = maxLevel;
                } else if(amountString.equalsIgnoreCase("midmax")){
                    level = maxLevel / 2;
                } else {
                    level = Integer.parseInt(amountString);
                }

                if (level > maxLevel) {
                    maxLevel(player, targetPlayerName);
                    return true;
                }

                // Obtener el nuevo maxStorage basado en el nivel que se va a establecer
                int maxStorage = MethodUtils.getMaxStorageAmount(plugin, bankName, level);

                // Cambiar el nivel del banco
                bankConfig.set(playerPath + ".level", level);
                bankManager.saveBank();
                setLevelSuccess(player, targetPlayerName);

                // Manejo de changeBalanceString
                if (changeBalanceString.length > 0) {
                    boolean changeBalance = Boolean.parseBoolean(changeBalanceString[0]);

                    if (changeBalance) {
                        // Si es true, establecer el balance al maxStorage del nuevo nivel
                        amount = maxStorage;
                        bankConfig.set(playerPath + ".balance", maxStorage);
                        bankManager.saveBank();
                        setBalanceSuccess(player, targetPlayerName);
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
                        setBalanceSuccess(player, targetPlayerName);
                    }
                }

            } catch (NumberFormatException e) {
                depositFailure(player, targetPlayerName);
            }
        } else {
            bankSetUsage(player);
        }
        return true;
    }

    private void notFoundPlayer(Player player, String targetPlayerName) {
        String messagePath = "bank.notFoundPlayer";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void depositFailure(Player player, String targetPlayerName) {
        String messagePath = "bank.set.depositFailure";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void maxBalance(Player player, String targetPlayerName) {
        String messagePath = "bank.set.maxBalance";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void maxLevel(Player player, String targetPlayerName) {
        String messagePath = "bank.set.maxLevel";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }

    private void setBalanceSuccess(Player player, String targetPlayerName) {
        String messagePath = "bank.set.setBalanceSuccess";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, amount, MineBank.getPlaceholderAPI());
    }

    private void setLevelSuccess(Player player, String targetPlayerName) {
        String messagePath = "bank.set.setLevelSuccess";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, targetPlayerName, messagePath, plugin, level, MineBank.getPlaceholderAPI());
    }

    private void bankSetUsage(Player player) {
        String messagePath = "bank.usage.set";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }
}
