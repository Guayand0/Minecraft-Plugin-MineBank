package mb.Guayando.events;

import mb.Guayando.MineBank;
import mb.Guayando.managers.BankManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;

public class OnPlayerJoin implements Listener {

    private final MineBank plugin;
    private final BankManager bankManager;

    public OnPlayerJoin(MineBank plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.reloadConfig();
        plugin.getLanguageManager().reloadLanguage();
        bankManager.reloadBank();

        // Obtener una nueva instancia de bankConfig después de recargar
        FileConfiguration bankConfig = bankManager.getBank();
        FileConfiguration config = plugin.getConfig(); // Configuración principal para obtener los nombres de los bancos

        Player player = event.getPlayer();
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();

        // Si el jugador no tiene el campo "bank-name", asignarlo
        if (!bankConfig.contains(playerPath + ".bank-name")) {
            // Buscar el primer banco disponible en la configuración
            String firstBankName = null;
            if (config.contains("bank")) {
                Set<String> bankNames = config.getConfigurationSection("bank").getKeys(false);
                if (!bankNames.isEmpty()) {
                    firstBankName = bankNames.iterator().next(); // Obtener el primer banco que aparece
                }
            }

            // Si no se encontró un banco en la configuración, asigna "NULL" como fallback
            if (firstBankName == null) {
                firstBankName = "NULL";
            }

            bankConfig.set(playerPath + ".bank-name", firstBankName);
        }

        // Si el jugador no tiene el campo "balance", asignar 0 por defecto
        if (!bankConfig.contains(playerPath + ".balance")) {
            bankConfig.set(playerPath + ".balance", 0);
        }

        // Si el jugador no tiene el campo "level", asignar 1 por defecto
        if (!bankConfig.contains(playerPath + ".level")) {
            bankConfig.set(playerPath + ".level", 1);
        }

        // Guardar la configuración del banco después de las asignaciones
        bankManager.saveBank();
    }
}