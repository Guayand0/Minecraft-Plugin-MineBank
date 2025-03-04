package com.Guayand0.commands.subcommands;

import com.Guayand0.Data.BankData;
import com.Guayand0.Data.BankManager;
import com.Guayand0.Data.Player.JSON.SetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SubCommandAdd implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();
    private final BankManager BM = new BankManager();
    private final SetPlayerBankData SPBD = new SetPlayerBankData();

    private final Economy economy;

    private int amount = -1;
    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private String targetPlayerName;

    public SubCommandAdd(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        String playerName = player.getName();

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            bankAddUsageMessage(player); // Mensaje
            return true;
        }

        try {
            BankData bankData;

            List<String> playerNames = BU.getPlayerNameOfBank(plugin);
            targetPlayerName = args[1];

            if (playerNames.contains(targetPlayerName)) {
                bankData = BM.getBankData(plugin, targetPlayerName);

                // Obtener datos del banco del jugador y maximos de nivel y balance
                if (bankData != null) {
                    bankName = bankData.getBankName();
                    bankLevel = bankData.getBankLevel();
                    bankBalance = bankData.getBankBalance();
                    offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                    offlineProfitTimes = bankData.getOfflineProfitTimes();
                    bankMaxBalance = bankData.getBankMaxBalance();
                }

                if (!player.hasPermission(plugin.pluginName + ".admin")) {
                    noPermMessage(player); // Mensaje
                    return true;
                }

                // Caso: /bank add <player> <amount>
                if (args.length < 3) {
                    bankTargetDepositFailureMessage(player); // Mensaje
                    return true;
                }

                // Validar que <amount> sea un número
                try {
                    String playerAmountString = args[2];
                    amount = Integer.parseInt(playerAmountString);
                } catch (NumberFormatException e) {
                    bankTargetDepositFailureMessage(player); // Mensaje
                    return true;
                }

                // Verificar que la cantidad a recoger sea válida
                if (amount <= 0) {
                    bankTargetDepositFailureMessage(player); // Mensaje
                    return true;
                }

                // Verificar que el nuevo balance no exceda el máximo
                if ((bankBalance + amount) > bankMaxBalance) {
                    bankTargetBalanceExceedsMessage(player); // Mensaje
                    return true;
                }
                bankBalance = bankBalance + amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, targetPlayerName, newBankData);
                if (success) {
                    bankTargetDepositSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }

            } else {
                // /bank add {<amount>/half-balance/all/mid-max}
                bankData = BM.getBankData(plugin, playerName);

                // Obtener datos del banco del jugador y maximos de nivel y balance
                if (bankData != null) {
                    bankName = bankData.getBankName();
                    bankLevel = bankData.getBankLevel();
                    bankBalance = bankData.getBankBalance();
                    offlineProfitAccrued = bankData.getOfflineProfitAccrued();
                    offlineProfitTimes = bankData.getOfflineProfitTimes();
                    bankMaxBalance = bankData.getBankMaxBalance();
                }

                // Caso: /bank add {half-balance/all/mid-max} (sin <player>)
                String amountString = args[1];

                int playerEconomyBalance = BU.getPlayerBalance(player, economy);

                if (args[1].equalsIgnoreCase("half-balance")) {
                    amount = bankBalance / 2;

                    // Si el máximo de almacenamiento del banco es 500, deposita toda lo que tengo o toda hasta llegar al máximo
                } else if (args[1].equalsIgnoreCase("all")) {
                    amount = Math.min(playerEconomyBalance, (bankMaxBalance - bankBalance));

                    // Si el máximo de almacenamiento del banco es 500, deposita 250
                } else if (args[1].equalsIgnoreCase("mid-max")) {
                    amount = bankMaxBalance / 2;

                    // Caso: /bank add <amount>
                } else {

                    // Validar que <amount> sea un número
                    try {
                        amount = Integer.parseInt(amountString);
                    } catch (NumberFormatException e) {
                        bankDepositFailureMessage(player); // Mensaje
                        return true;
                    }
                }

                // Verificar que el jugador tenga suficiente dinero
                if (playerEconomyBalance < amount) {
                    playerNotEnoughtBalanceMessage(player); // Mensaje
                    return true;
                }

                // Verificar que el nuevo balance no exceda el máximo
                if ((bankBalance + amount) > bankMaxBalance) {
                    bankBalanceExceedsMessage(player); // Mensaje
                    return true;
                }

                // Restar dinero de la economía del jugador
                economy.withdrawPlayer(player, amount);

                bankBalance = bankBalance + amount;
                // Establecer nuevos valores de banco
                PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                if (success) {
                    bankDepositSuccessMessage(player); // Mensaje
                } else {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCan't update player bank data for " + playerName));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankAddUsageMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankBalanceExceedsMessage(Player player) {
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalance));

        for (String message : languageManager.getAllMessage("bank.add.deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerNotEnoughtBalanceMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositSuccessMessage(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(amount));

        for (String message : languageManager.getAllMessage("bank.add.deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetBalanceExceedsMessage(Player player) {
        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(bankMaxBalance));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositSuccessMessage(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(amount));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.target-deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPermMessage(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}

/*package com.Guayand0.commands.subcommands;

import com.Guayand0.Data.Bank.BankLevelData;
import com.Guayand0.Data.Bank.JSON.GetBankLevelData;
import com.Guayand0.Data.Player.JSON.GetPlayerBankData;
import com.Guayand0.Data.Player.JSON.SetPlayerBankData;
import com.Guayand0.Data.Player.PlayerBankData;
import com.Guayand0.MineBank;
import com.Guayand0.managers.FileManager;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import com.google.gson.JsonObject;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SubCommandAdd implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final FileManager fileManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();
    private final GetPlayerBankData GPBD = new GetPlayerBankData();
    private final SetPlayerBankData SPBD = new SetPlayerBankData();
    private final GetBankLevelData GBLD = new GetBankLevelData();

    private final Economy economy;

    private int amount = -1;

    private String bankName = "NULL";
    private int bankBalance = -1;
    private int bankLevel = -1;
    private int offlineProfitAccrued = -1;
    private int offlineProfitTimes = -1;
    private int bankMaxBalance = -1;
    private int bankMaxLevel = -1;
    private int bankLevelUpgradeCost = -1;
    private String playerName;

    private String targetPlayerName;
    private int targetAmountToAdd = -1;
    private int targetBankMaxBalance = -1;

    public SubCommandAdd(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.fileManager = plugin.getFileManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        playerName = player.getName();

        // Si el comando tiene menos de 2 argumentos, muestra el mensaje de uso
        if (args.length < 2) {
            bankAddUsage(player); // Mensaje
            return true;
        }

        try {
            PlayerBankData playerBankData;

            // Jugador del que se van a obtener los datos del banco
            if (args.length > 1) {
                targetPlayerName = args[1];
                String playerAmountString = args[2];

                // Si targetPlayerName está en la lista del banco
                if (BU.getPlayerNameOfBank(plugin).contains(targetPlayerName)) {
                    playerBankData = GPBD.getPlayerBankData(plugin, targetPlayerName);

                    if (!player.hasPermission(plugin.pluginName + ".admin")) {
                        noPerm(player); // Mensaje
                        return true;
                    }

                    // Caso: /bank add <player> <amount>
                    if (args.length < 3) {
                        bankTargetDepositFailure(player); // Mensaje
                        return true;
                    }

                    // Validar que <amount> sea un número
                    try {
                        targetAmountToAdd = Integer.parseInt(playerAmountString);
                    } catch (NumberFormatException e) {
                        bankTargetDepositFailure(player); // Mensaje
                        return true;
                    }

                    // Obtener datos del banco: nombre, nivel, dinero, beneficio offline
                    if (playerBankData != null) {
                        bankName = playerBankData.getName();
                        bankLevel = playerBankData.getLevel();
                        bankBalance = playerBankData.getBalance();
                        offlineProfitAccrued = playerBankData.getOfflineAccruedProfit();
                        offlineProfitTimes = playerBankData.getOfflineProfitTimes();
                    }

                    // Cargar los niveles de un banco específico
                    Map<Integer, BankLevelData> levels = GBLD.loadBankLevels(plugin, bankName);

                    // Verificar si levels contiene el nivel que buscamos
                    BankLevelData levelData = levels.get(bankLevel);

                    if (levelData != null) {
                        // Acceder a los datos del nivel
                        boolean bankIsFinalLevel = levelData.isFinalLevel();

                        // Obtener datos del banco: maximo de diner, maximo nivel, coste de nuevo niel
                        if (!bankIsFinalLevel) {
                            // Si NO es el ultimo nivel
                            bankMaxBalance = levelData.getMaxBalance();
                            bankLevelUpgradeCost = levelData.getUpgradeCost();

                            // Buscar el último nivel
                            for (Map.Entry<Integer, BankLevelData> entry : levels.entrySet()) {
                                if (entry.getValue().isFinalLevel()) {
                                    bankMaxLevel = entry.getKey(); // Asignamos el nivel final
                                    break; // Salir del bucle una vez encontrado
                                }
                            }

                            // Si no hay ultimo nivel
                            if (bankMaxLevel == -1) {
                                System.out.println("No se encontró un nivel final.");
                            }
                        } else {
                            // Si es el ultimo nivel
                            bankMaxBalance = 0;
                            bankLevelUpgradeCost = 0;
                            bankMaxLevel = bankLevel;
                        }
                    } else {
                        // Si el nivel no existe en el mapa
                        System.out.println("Nivel " + bankLevel + " no encontrado.");
                    }

                    // Verificar que la cantidad a recoger sea válida
                    if (targetAmountToAdd <= 0) {
                        bankTargetDepositFailure(player); // Mensaje
                        return true;
                    }

                    // Verificar que el nuevo balance no exceda el máximo
                    if ((bankBalance + targetAmountToAdd) > bankMaxBalance) {
                        bankTargetBalanceExceeds(player); // Mensaje
                        return true;
                    }

                    bankBalance = bankBalance + targetAmountToAdd;
                    // Establecer nuevos valores de banco
                    PlayerBankData newBankData = new PlayerBankData(bankName, bankLevel, bankBalance, offlineProfitAccrued, offlineProfitTimes);

                    boolean success = SPBD.setPlayerBankData(plugin, playerName, newBankData);
                    if (success) {
                        bankTargetDepositSuccess(player); // Mensaje
                    } else {
                        System.out.println("No se pudo actualizar la información del jugador.");
                    }

                } else {
                    // Si no está en la lista, obtener los datos del banco para el jugador actual
                    playerBankData = GPBD.getPlayerBankData(plugin, playerName);
                }
            } else {
                targetPlayerName = null;
                playerBankData = GPBD.getPlayerBankData(plugin, playerName);
            }

            // Obtener datos del banco: nombre, nivel, dinero, beneficio offline
            if (playerBankData != null) {
                bankName = playerBankData.getName();
                bankLevel = playerBankData.getLevel();
                bankBalance = playerBankData.getBalance();
                offlineProfitAccrued = playerBankData.getOfflineAccruedProfit();
                offlineProfitTimes = playerBankData.getOfflineProfitTimes();
            }

            // Cargar los niveles de un banco específico
            Map<Integer, BankLevelData> levels = GBLD.loadBankLevels(plugin, bankName);

            // Verificar si levels contiene el nivel que buscamos
            BankLevelData levelData = levels.get(bankLevel);

            if (levelData != null) {
                // Acceder a los datos del nivel
                boolean bankIsFinalLevel = levelData.isFinalLevel();

                // Obtener datos del banco: maximo de diner, maximo nivel, coste de nuevo niel
                if (!bankIsFinalLevel) {
                    // Si NO es el ultimo nivel
                    bankMaxBalance = levelData.getMaxBalance();
                    bankLevelUpgradeCost = levelData.getUpgradeCost();

                    // Buscar el último nivel
                    for (Map.Entry<Integer, BankLevelData> entry : levels.entrySet()) {
                        if (entry.getValue().isFinalLevel()) {
                            bankMaxLevel = entry.getKey(); // Asignamos el nivel final
                            break; // Salir del bucle una vez encontrado
                        }
                    }

                    // Si no hay ultimo nivel
                    if (bankMaxLevel == -1) {
                        System.out.println("No se encontró un nivel final.");
                    }
                } else {
                    // Si es el ultimo nivel
                    bankMaxBalance = 0;
                    bankLevelUpgradeCost = 0;
                    bankMaxLevel = bankLevel;
                }
            } else {
                // Si el nivel no existe en el mapa
                System.out.println("Nivel " + bankLevel + " no encontrado.");
            }


            // Caso: /bank add {half-balance/all/mid-max} (sin <player>)
            String amountString = args[1];

            // Obtener el banco del jugador
            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);

            int playerBankBalance = BU.getPlayerBankBalance(bank);
            bankMaxBalance = BU.getBankMaxBalanceByLevel(plugin, playerName);
            int playerEconomyBalance = BU.getPlayerBalance(player, economy);

            if (args[1].equalsIgnoreCase("half-balance")) {
                amount = playerBankBalance / 2;

                // Si el máximo de almacenamiento del banco es 500, deposita toda lo que tengo o toda hasta llegar al máximo
            } else if (args[1].equalsIgnoreCase("all")) {
                amount = Math.min(playerEconomyBalance, (bankMaxBalance - playerBankBalance));

                // Si el máximo de almacenamiento del banco es 500, deposita 250
            } else if (args[1].equalsIgnoreCase("mid-max")) {
                amount = bankMaxBalance / 2;

                // Caso: /bank add <amount>
            } else {

                // Validar que <amount> sea un número
                try {
                    amount = Integer.parseInt(amountString);
                } catch (NumberFormatException e) {
                    bankDepositFailure(player); // Mensaje
                    return true;
                }
            }

            // Verificar que el jugador tenga suficiente dinero
            if (playerEconomyBalance < amount) {
                playerNotEnoughtBalance(player); // Mensaje
                return true;
            }

            // Verificar que el nuevo balance no exceda el máximo
            if ((playerBankBalance + amount) > bankMaxBalance) {
                bankBalanceExceeds(player); // Mensaje
                return true;
            }

            // Restar dinero de la economía del jugador
            economy.withdrawPlayer(player, amount);

            // Añadir el monto al banco del jugador
            BU.setPlayerBankBalance(bank, playerBankBalance + amount);
            fileManager.updatePlayerInfo(bank, playerName);
            bankDepositSuccess(player); // Mensaje

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankAddUsage(Player player) {
        for (String message : languageManager.getAllMessage("bank.add-usage")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankBalanceExceeds(Player player) {
        plugin.placeholders.put("%playerbankmaxbalance%", String.valueOf(bankMaxBalance));

        for (String message : languageManager.getAllMessage("bank.add.deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void playerNotEnoughtBalance(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.not-enough-bank-balance")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankDepositSuccess(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(amount));

        for (String message : languageManager.getAllMessage("bank.add.deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetBalanceExceeds(Player player) {
        plugin.placeholders.put("%targetbankmaxbalance%", String.valueOf(targetBankMaxBalance));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-exceeds")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositSuccess(Player player) {
        plugin.placeholders.put("%amount%", String.valueOf(targetAmountToAdd));
        plugin.placeholders.put("%targetplayername%", String.valueOf(targetPlayerName));

        for (String message : languageManager.getAllMessage("bank.add.target-deposit-success")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTargetDepositFailure(Player player) {
        for (String message : languageManager.getAllMessage("bank.add.target-deposit-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void noPerm(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}
*/
