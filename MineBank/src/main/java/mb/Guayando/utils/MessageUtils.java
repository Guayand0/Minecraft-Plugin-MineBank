package mb.Guayando.utils;

import mb.Guayando.MineBank;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class MessageUtils {

    private static String playerName, playerBankName;
    private static int playerBankBalance, playerBankLevel, playerBankMaxStorage, playerBankMaxLevel, playerLevelPrice, playerBankTop, playerBalance;
    public static String getColoredMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String applyPAPIAndColorMessage(Player player, String message) {
        return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message));
    }

    public static String applyPlaceholdersAndColor(Player player, String targetPlayerName, String message, MineBank plugin, int amount) {
        FileConfiguration bankConfig = plugin.getBankManager().getBank(); // Obtener la configuración actualizada
        if (player != null) {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            playerName = player.getName();
            playerBankBalance = bankConfig.getInt(playerPath + ".balance");
            playerBankLevel = bankConfig.getInt(playerPath + ".level");
            playerBankName = bankConfig.getString(playerPath + ".bank-name");
            playerBankMaxStorage = MethodUtils.getMaxStorageAmount(plugin, playerBankName, playerBankLevel);
            playerBankMaxLevel = MethodUtils.getMaxBankLevel(plugin, playerBankName);
            playerLevelPrice = MethodUtils.getLevelPrice(plugin, playerBankName, playerBankLevel);
            playerBankTop = MethodUtils.getPlayerTop(player, plugin);
            playerBalance = (int) plugin.getEconomy().getBalance(player);
        }

        //Player targetPlayer = Bukkit.getPlayer(targetPlayerName); // Obtener el objeto Player desde el nombre
        String targetName = targetPlayerName; // Usar el nombre proporcionado en el comando
        int targetBankBalance = 0;
        int targetBankLevel = 1;
        int targetBankMaxStorage = 0;
        int targetBankMaxLevel = 0;
        String targetBankName = "NULL";
        int targetBalance = 0;
        //int targetBankTop = 0;
        String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetName);

        if (targetUUID != null) {
            // Obtén la configuración del jugador usando el UUID
            ConfigurationSection playerSection = bankConfig.getConfigurationSection("bank." + targetUUID);
            if (playerSection != null) {
                Set<String> keys = playerSection.getKeys(false);
                if (!keys.isEmpty()) {
                    // Asume que hay solo un nombre asociado con el UUID, usa el primer nombre encontrado
                    targetName = keys.iterator().next();
                    String targetPath = "bank." + targetUUID + "." + targetName;
                    targetBankBalance = bankConfig.getInt(targetPath + ".balance");
                    targetBankLevel = bankConfig.getInt(targetPath + ".level");
                    targetBankName = bankConfig.getString(targetPath + ".bank-name");
                    targetBankMaxStorage = MethodUtils.getMaxStorageAmount(plugin, targetBankName, targetBankLevel);
                    targetBankMaxLevel = MethodUtils.getMaxBankLevel(plugin, targetBankName);
                    targetBalance = (int) plugin.getEconomy().getBalance(targetName);
                    //targetBankTop = MethodUtils.getPlayerTop(targetPlayer, plugin);
                }
            }
        }

        int interestPercentage = MethodUtils.getInterestPercentage(plugin);
        int amountReceived = MethodUtils.getAmountReceived(amount,interestPercentage);
        int keepInBankProfit = MethodUtils.getRoundedProfit(plugin, player);
        int profitPercentage = MethodUtils.getProfitPercentage(plugin);
        int minAmountToWinProfit = MethodUtils.getMinAmountToWinProfit(plugin);

        // Reemplazar los placeholders
        message = message
                .replaceAll("%plugin%", MineBank.prefix)
                .replaceAll("%version%", plugin.getVersion())
                .replaceAll("%latestversion%", plugin.getLatestVersion())
                .replaceAll("%link%", "https://www.spigotmc.org/resources/119147/")
                .replaceAll("%author%", plugin.getDescription().getAuthors().toString())
                // ------------------------------------------ //
                .replaceAll("%playerName%", playerName)
                .replaceAll("%playerBankBalance%", String.valueOf(playerBankBalance))
                .replaceAll("%playerBankLevel%", String.valueOf(playerBankLevel))
                .replaceAll("%playerBankName%", playerBankName.substring(0, 1).toUpperCase() + playerBankName.substring(1).toLowerCase())
                .replaceAll("%playerBankMaxStorage%", String.valueOf(playerBankMaxStorage))
                .replaceAll("%playerBankMaxLevel%", String.valueOf(playerBankMaxLevel))
                .replaceAll("%playerBankTop%", String.valueOf(playerBankTop))
                .replaceAll("%playerBalance%", String.valueOf(playerBalance))

                .replaceAll("%unlockNextLevelPrice%", String.valueOf(playerLevelPrice))
                .replaceAll("%unlockNextLevel%", String.valueOf(playerBankLevel))
                // ------------------------------------------ //
                .replaceAll("%targetName%", targetName)
                .replaceAll("%targetBankBalance%", String.valueOf(targetBankBalance))
                .replaceAll("%targetBankLevel%", String.valueOf(targetBankLevel))
                .replaceAll("%targetBankName%", targetBankName.substring(0, 1).toUpperCase() + targetBankName.substring(1).toLowerCase())
                .replaceAll("%targetBankMaxStorage%", String.valueOf(targetBankMaxStorage))
                .replaceAll("%targetBankMaxLevel%", String.valueOf(targetBankMaxLevel))
                //.replaceAll("%targetBankTop%", String.valueOf(targetBankTop))
                .replaceAll("%targetBalance%", String.valueOf(targetBalance))

                // ------------------------------------------ //
                .replaceAll("%amount%", String.valueOf(amount))
                .replaceAll("%setBankLevel%", String.valueOf(amount))
                .replaceAll("%amountDeducted%", String.valueOf(amount))
                .replaceAll("%amountReceived%", String.valueOf(amountReceived))
                .replaceAll("%interestPercentage%", String.valueOf(interestPercentage))
                .replaceAll("%keepInBankProfit%", String.valueOf(keepInBankProfit))
                .replaceAll("%profitPercentage%", String.valueOf(profitPercentage))
                .replaceAll("%minAmountToWinProfit%", String.valueOf(minAmountToWinProfit))
                ;

        // Reemplazar los placeholders de los tops
        message = MethodUtils.replaceTopPlaceholders(message, plugin);

        // Aplicar color y placeholders con PlaceholderAPI
        return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message));
    }

    // String messagePath = "bank.bal.playerBalance";
    // MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    public static void sendMessageWithPlaceholdersAndColor(Player player, String targetPlayerName, String messagePath, MineBank plugin, int amount) {
        String message = plugin.getLanguageManager().getMessage(messagePath);
        if (player != null) {
            message = applyPlaceholdersAndColor(player, targetPlayerName, message, plugin, amount);
            player.sendMessage(message);
        }
    }

    // String messagePath = "bank.bal.playerBalance";
    // MessageUtils.sendMessageListWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    public static void sendMessageListWithPlaceholdersAndColor(Player player, String targetPlayerName, String messagePath, MineBank plugin, int amount) {
        List<String> messages = plugin.getLanguageManager().getStringList(messagePath);
        if (player != null) {
            for (String msg : messages) {
                String processedMessage = applyPlaceholdersAndColor(player, targetPlayerName, msg, plugin, amount);
                player.sendMessage(processedMessage);
            }
        }
    }

    // ---------------------------------- PAPIBANK ---------------------------------- //

    public static void updatePlayerBankData(Player player, MineBank plugin) {
        if (player != null) {
            FileConfiguration bankConfig = plugin.getBankManager().getBank();
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            playerBankBalance = bankConfig.getInt(playerPath + ".balance");
            playerBankLevel = bankConfig.getInt(playerPath + ".level");
            playerBankName = bankConfig.getString(playerPath + ".bank-name");
            playerBankMaxStorage = MethodUtils.getMaxStorageAmount(plugin, playerBankName, playerBankLevel);
            playerBankMaxLevel = MethodUtils.getMaxBankLevel(plugin, playerBankName);
            playerLevelPrice = MethodUtils.getLevelPrice(plugin, playerBankName, playerBankLevel);
            playerBankTop = MethodUtils.getPlayerTop(player, plugin);
            playerBalance = (int) plugin.getEconomy().getBalance(player);
        }
    }

    public static String getPlayerBankName() {
        // Capitalizar la primera letra
        return playerBankName.substring(0, 1).toUpperCase() + playerBankName.substring(1).toLowerCase();
    }
    public static int getPlayerBankLevel(){
        return playerBankLevel;
    }
    public static int getPlayerBankBalance(){
        return playerBankBalance;
    }
    public static int getPlayerBankMaxStorage(){
        return playerBankMaxStorage;
    }
    public static int getPlayerBankMaxLevel(){
        return playerBankMaxLevel;
    }
    public static int getPlayerLevelPrice(){
        return playerLevelPrice;
    }
    public static int getPlayerBankTop(){
        return playerBankTop;
    }
    public static int getPlayerBalance(){
        return playerBalance;
    }

}