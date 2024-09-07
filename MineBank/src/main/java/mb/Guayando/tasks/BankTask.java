package mb.Guayando.tasks;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class BankTask extends BukkitRunnable {
    private final MineBank plugin;
    private FileConfiguration config;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private int profitPercentage, roundedProfit, minAmountToWin, profitInterval, minAmountToLose, interestsWithdraw;
    private boolean bankUseEnabled;

    public BankTask(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.config = plugin.getConfig();

        setConfig(plugin.getBankManager().getBank());
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

    private void executeBankTask() {
        languageManager.reloadLanguage();
        bankManager.reloadBank(); // Recargar la configuración del banco
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración del banco
        plugin.reloadConfig();
        updateConfig();

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
            int balance = bankManager.getBank().getInt(playerPath + ".balance", 0);
            int level = bankConfig.getInt(playerPath + ".level", 1);
            int maxStorage = getMaxStorageAmount(level);


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

    private void updateConfig() {
        this.minAmountToWin = plugin.getConfig().getInt("bank.profit.min-amount-to-win", 100);
        this.profitPercentage = plugin.getConfig().getInt("bank.profit.keep-in-bank", 1);
        this.profitInterval = plugin.getConfig().getInt("bank.profit.interval", 300);
        this.bankUseEnabled = plugin.getConfig().getBoolean("config.bank-use", true);
        this.minAmountToLose = plugin.getConfig().getInt("bank.interests.min-amount-to-lose", 500);
        this.interestsWithdraw = plugin.getConfig().getInt("bank.interests.withdraw", 2);
    }


    public void setConfig(FileConfiguration config) {
        this.config = config;
        updateConfig();
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

    public void receivedProfit(CommandSender sender) {
        String mensaje = languageManager.getMessage("bank.profit.received");
        if (mensaje != null) {
            mensaje = mensaje.replace("%plugin%", MineBank.prefix)
                    .replace("%profit%", String.valueOf(roundedProfit))
                    .replace("%percentage%", String.valueOf(profitPercentage));
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }

    public void maxStorageProfit(CommandSender sender) {
        String mensaje = languageManager.getMessage("bank.profit.max-storage");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }

    public void minStorageProfit(CommandSender sender) {
        String mensaje = languageManager.getMessage("bank.profit.min-storage");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix)
                    .replaceAll("%amount%", String.valueOf(minAmountToWin));
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
}