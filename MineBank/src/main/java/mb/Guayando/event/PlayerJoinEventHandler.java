package mb.Guayando.event;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerJoinEventHandler implements Listener {

    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private FileConfiguration bankConfig;
    private final FileConfiguration config;
    private int profitPercentage, minAmountToWin, profitInterval, minAmountToLose, interestsWithdraw;
    private boolean bankUseEnabled;

    public PlayerJoinEventHandler(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.config = plugin.getConfig();
        updateConfig();
    }

    private void updateConfig() {
        this.minAmountToWin = plugin.getConfig().getInt("bank.profit.min-amount-to-win", 100);
        this.profitPercentage = plugin.getConfig().getInt("bank.profit.keep-in-bank", 1);
        this.profitInterval = plugin.getConfig().getInt("bank.profit.interval", 300);
        this.bankUseEnabled = plugin.getConfig().getBoolean("config.bank-use", true);
        this.minAmountToLose = plugin.getConfig().getInt("bank.interests.min-amount-to-lose", 500);
        this.interestsWithdraw = plugin.getConfig().getInt("bank.interests.withdraw", 2);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.reloadConfig();
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        updateConfig();

        // Obtener una nueva instancia de bankConfig después de recargar
        bankConfig = plugin.getBankManager().getBank();

        Player player = event.getPlayer();
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        if (!bankConfig.contains(playerPath)) {
            // Establecer saldo inicial y nivel de banco al unirse
            bankConfig.set(playerPath + ".balance", 0);
            bankConfig.set(playerPath + ".level", 1);
            plugin.getBankManager().saveBank(); // Guarda la configuración
        }
    }
}
