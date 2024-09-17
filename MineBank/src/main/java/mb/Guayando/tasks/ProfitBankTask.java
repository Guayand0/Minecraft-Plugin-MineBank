package mb.Guayando.tasks;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ProfitBankTask extends BukkitRunnable {
    private final MineBank plugin;
    private final FileConfiguration config;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private int profitPercentage, roundedProfit, minAmountToWin, profitInterval, minAmountToLose, interestsWithdraw;
    private boolean bankUseEnabled;

    public ProfitBankTask(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.config = plugin.getConfig();

        updateConfig();
    }

    @Override
    public void run() {
        if (shouldRunTask()) {
            executeBankTask();
        }
    }

    private boolean shouldRunTask() {
        return bankUseEnabled;
    }

    private void updateConfig() {
        this.bankUseEnabled = config.getBoolean("config.bank-use", true);
        this.profitInterval = config.getInt("bank.profit.intervalSeconds", 300);
        this.minAmountToWin = config.getInt("bank.profit.minBankBalanceToApply", 500);
        this.profitPercentage = config.getInt("bank.profit.keepInBankPercentage", 1);
        this.minAmountToLose = config.getInt("bank.interests.minBankBalanceToApply", 100);
        this.interestsWithdraw = config.getInt("bank.interests.withdrawPercentage", 2);
    }

    private void executeBankTask() {
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        plugin.reloadConfig();
        updateConfig();

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            String bankName = bankManager.getBank().getString(playerPath + ".bank-name");
            int balance = bankManager.getBank().getInt(playerPath + ".balance");
            int level = bankManager.getBank().getInt(playerPath + ".level");
            int maxStorage = MethodUtils.getMaxStorageAmount(plugin, bankName, level);

            if (balance < 0) { balance = 0; }
            if (balance > maxStorage) { balance = maxStorage; }

            if (balance >= minAmountToWin) {
                long profit = (long) Math.floor(balance * (profitPercentage / 100.0));
                roundedProfit = (int) profit;

                if (balance + roundedProfit > maxStorage) {
                    maxStorageProfit(player);
                } else {
                    bankManager.getBank().set(playerPath + ".balance", balance + roundedProfit);
                    bankManager.saveBank();
                    receivedProfit(player);
                }
            } else {
                minStorageProfit(player);
            }
        });
    }

    public void receivedProfit(Player player) {
        String messagePath = "bank.profit.received";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void maxStorageProfit(Player player) {
        String messagePath = "bank.profit.max-storage";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }

    public void minStorageProfit(Player player) {
        String messagePath = "bank.profit.min-storage";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
    }
}