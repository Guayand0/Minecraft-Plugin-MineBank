package mb.Guayando.managers;

import mb.Guayando.MineBank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class PermissionManager {

    private final MineBank plugin;

    public PermissionManager(MineBank plugin) {
        this.plugin = plugin;
    }

    public void registerBankPermissions() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        // Cargar los bancos desde config.yml
        Set<String> bankNames = plugin.getConfig().getConfigurationSection("bank").getKeys(false);

        for (String bankName : bankNames) {
            // Verificar si el banco tiene una sección "level"
            if (plugin.getConfig().contains("bank." + bankName + ".level")) {

                // Crear el nombre del permiso dinámicamente solo para los bancos con sección "level"
                String permissionName = "minebank.bank." + bankName;

                // Comprobar si el permiso ya está registrado
                if (pluginManager.getPermission(permissionName) == null) {
                    // Crear el permiso
                    Permission bankPermission = new Permission(permissionName);

                    // Registrar el permiso en el PluginManager
                    pluginManager.addPermission(bankPermission);
                }
            }
        }

        // Iniciar la verificación periódica del banco de los jugadores
        startBankCheckTask();
    }

    // Método que ejecuta la tarea para verificar los permisos de los jugadores cada 2 segundos
    private void startBankCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String correctBank;

                    if (player.isOp()) {
                        // Si el jugador es OP, asignar el último banco disponible
                        correctBank = getLastLevelBank();
                    } else {
                        // Si no es OP, verificar el banco basado en sus permisos
                        correctBank = getBankFromPermissions(player);
                    }

                    String currentBank = getCurrentPlayerBank(player);

                    if (correctBank != null && !correctBank.equals(currentBank)) {
                        // Si el banco no coincide con su permiso o condición, cambiar el banco
                        setPlayerBank(player, correctBank);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Ejecutar cada 40 ticks (2 segundos)
    }

    // Método que obtiene el banco actual del jugador desde su configuración
    private String getCurrentPlayerBank(Player player) {
        String path = "bank." + player.getUniqueId() + "." + player.getName() + ".bank-name";
        return plugin.getBankManager().getBank().getString(path);
    }

    // Método que obtiene los bancos en orden según su aparición en la configuración
    private Map<Integer, String> getOrderBanks() {
        Set<String> bankNames = plugin.getConfig().getConfigurationSection("bank").getKeys(false);
        Map<Integer, String> orderedBanks = new HashMap<>();
        int order = 1;

        for (String bankName : bankNames) {
            if (plugin.getConfig().contains("bank." + bankName + ".level")) {
                orderedBanks.put(order, bankName);
                order++;
            }
        }

        return orderedBanks;
    }

    // Método que obtiene el banco correcto basado en los permisos del jugador
    private String getBankFromPermissions(Player player) {
        Map<Integer, String> orderedBanks = getOrderBanks();
        String selectedBank = null;

        // Iterar sobre los bancos en orden
        for (String bankName : orderedBanks.values()) {
            String permissionName = "minebank.bank." + bankName;

            // Si el jugador tiene el permiso para un banco específico
            if (player.hasPermission(permissionName)) {
                selectedBank = bankName; // Actualizar el banco seleccionado
            }
        }

        // Si no tiene ningún permiso de banco, asignar el primer banco con la sección "level"
        return selectedBank != null ? selectedBank : getFirstLevelBank();
    }

    // Método para obtener el primer banco con la sección "level"
    private String getFirstLevelBank() {
        Set<String> bankNames = plugin.getConfig().getConfigurationSection("bank").getKeys(false);

        for (String bankName : bankNames) {
            if (plugin.getConfig().contains("bank." + bankName + ".level")) {
                return bankName;
            }
        }
        return null; // Si no hay bancos con "level", devolver null
    }

    // Método para obtener el banco con el nivel más alto (último banco con "level")
    private String getLastLevelBank() {
        Set<String> bankNames = plugin.getConfig().getConfigurationSection("bank").getKeys(false);
        String lastBank = null;

        for (String bankName : bankNames) {
            // Asignar solo bancos que tienen la sección "level"
            if (plugin.getConfig().contains("bank." + bankName + ".level")) {
                lastBank = bankName; // Actualizar al último banco encontrado
            }
        }

        return lastBank; // Devolver el último banco encontrado con la sección "level"
    }

    // Método para establecer el banco del jugador en la configuración
    private void setPlayerBank(Player player, String bankName) {
        String path = "bank." + player.getUniqueId() + "." + player.getName() + ".bank-name";
        plugin.getBankManager().getBank().set(path, bankName);
        plugin.getBankManager().saveBank(); // Guardar los cambios
    }
}
