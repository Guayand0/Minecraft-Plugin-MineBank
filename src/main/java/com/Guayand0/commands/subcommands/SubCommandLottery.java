//package com.Guayand0.commands.subcommands;
//
//import com.Guayand0.MineBank;
//import com.Guayand0.managers.FileManager;
//import com.Guayand0.managers.LanguageManager;
//import com.Guayand0.utils.BankUtils;
//import com.Guayand0.utils.MessageUtils;
//import com.google.gson.JsonObject;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//
//public class SubCommandLottery implements CommandExecutor {
//
//    private final MineBank plugin;
//    private final LanguageManager languageManager;
//    private final FileManager fileManager;
//
//    private final MessageUtils MU = new MessageUtils();
//    private final BankUtils BU = new BankUtils();
//
//    public SubCommandLottery(MineBank plugin) {
//        this.plugin = plugin;
//        this.languageManager = plugin.getLanguageManager();
//        this.fileManager = plugin.getFileManager();
//    }
//
//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//
//        Player player = (Player) sender;
//
//        // Si el comando tiene menos de 2 argumentos
//        if (args.length < 2) {
//            bankLotteryUsage(player); // Mensaje
//            return true;
//        }
//
//        try {
//
//            String playerName = player.getName();
//
//            // Obtener solo el banco del jugador una vez
//            JsonObject bank = BU.getBankDataOfPlayerName(plugin, playerName);
//
//            String playerBankName = BU.getPlayerBankName(bank);
//            int playerBankBalance = BU.getPlayerBankBalance(bank);
//            int playerBankLevel = BU.getPlayerBankLevel(bank);
//            int playerOfflineAccruedProfit = BU.getPlayerOfflineAccruedProfit(bank);
//            int playerOfflineProfitTimes = BU.getPlayerOfflineProfitTimes(bank);
//
//            int bankMaxBalanceByLevel = BU.getBankMaxBalanceByLevel(plugin, playerName);
//            int bankUpgradeLevelCost = BU.getBankUpgradeCostByLevel(plugin, playerName);
//            int bankMaxLevel = BU.getBankMaxLevel(plugin, playerName);
//            int accruedInterestData = BU.getAccruedInterestData(plugin);
//
//            boolean bankUseAllowed = BU.getBankAllowed(plugin);
//
//
//
//
//
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
//        }
//
//        return true;
//    }
//
//    private void bankLotteryUsage(Player player) {
//        for (String message : languageManager.getAllMessage("bank.lottery-usage")) {
//            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
//        }
//    }
//}
