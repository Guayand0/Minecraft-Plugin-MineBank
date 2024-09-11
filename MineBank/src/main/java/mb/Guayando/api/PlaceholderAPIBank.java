package mb.Guayando.api;

import mb.Guayando.MineBank;
import mb.Guayando.commands.subcommandsbank.SubComandoBalance;
import mb.Guayando.config.BankInventoryManager;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;


public class PlaceholderAPIBank extends PlaceholderExpansion {

     // We get an instance of the plugin later.
    private final MineBank plugin;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private final BankInventoryManager bankInventoryManager;

    public PlaceholderAPIBank(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.bankInventoryManager = plugin.getBankInventoryManager();
    }
    @Override
    public boolean persist(){
        return true;
    }
    @Override
    public boolean canRegister(){
        return true;
    }
    @Override
    public String getAuthor(){
        return "Guayando";
    }
    @Override
    public String getIdentifier() {
        return "minebank";
    } // %minebank_XXXXX%
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        languageManager.reloadLanguage();
        bankManager.reloadBank();
        plugin.reloadConfig();
        bankInventoryManager.reloadInventory();

        if (player == null) {
            return "";
        }

        String uuid = player.getUniqueId().toString();
        String playerName = player.getName();
// ---------------------------------------------------- GENERAL --------------------------------------------------- //
        // %minebank_maxLevel%
        if (identifier.equals("maxLevel")) {
            return String.valueOf(getMaxBankLevel());
        }
// ----------------------------------------------------- USER ----------------------------------------------------- //
        // %minebank_user_name%
        if (identifier.equals("user_name")) {
            return playerName; // Retorna el nombre del jugador actual
        }

        // %minebank_user_balance%
        if (identifier.equals("user_balance")) {
            String path = "bank." + uuid + "." + playerName + ".balance";
            return bankManager.getBank().getString(path, "NULL");
        }

        // %minebank_user_maxStorage%
        if (identifier.equals("user_maxStorage")) {
            String path = "bank." + uuid + "." + playerName + ".level";
            String levelString = bankManager.getBank().getString(path, "NULL"); // Obtener el nivel del jugador
            int level = 0;
            try {
                level = Integer.parseInt(levelString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return String.valueOf(getMaxStorageAmount(level));
        }

        // %minebank_user_level%
        if (identifier.equals("user_level")) {
            String path = "bank." + uuid + "." + playerName + ".level";
            return bankManager.getBank().getString(path, "NULL");
        }

// ---------------------------------------------------- TARGET ---------------------------------------------------- //
        // Solo funciona bien con subcomando balance
        // %minebank_target_name%
        /*if (identifier.equals("target_name")) {
            // Retorna el nombre del jugador objetivo almacenado
            String targetPlayerName = SubComandoBalance.getCurrentTargetPlayerName();
            return targetPlayerName != null ? targetPlayerName : "NULL";
        }

        // %minebank_target_balance%
        /*if (identifier.equals("target_balance")) {
            String targetPlayerName = SubComandoBalance.getCurrentTargetPlayerName();
            if (targetPlayerName != null) {
                String targetUUID = getPlayerUUIDByName(targetPlayerName); // Busca el UUID del jugador objetivo
                if (targetUUID != null) {
                    String path = "bank." + targetUUID + "." + targetPlayerName + ".balance";
                    return String.valueOf(bankManager.getBank().getString(path, "NULL")); // Retorna el balance del jugador objetivo
                } else {
                    return "NULL";
                }
            } else {
                return "NULL";
            }
        }

        // %minebank_target_level%
        if (identifier.equals("target_level")) {
            String targetPlayerName = SubComandoBalance.getCurrentTargetPlayerName();
            if (targetPlayerName != null) {
                String targetUUID = getPlayerUUIDByName(targetPlayerName); // Busca el UUID del jugador objetivo
                if (targetUUID != null) {
                    String path = "bank." + targetUUID + "." + targetPlayerName + ".level";
                    return bankManager.getBank().getString(path, "NULL"); // Retorna el nivel del jugador objetivo
                } else {
                    return "NULL";
                }
            } else {
                return "NULL";
            }
        }

        // %minebank_target_maxStorage%
        if (identifier.equals("target_maxStorage")) {
            String targetPlayerName = SubComandoBalance.getCurrentTargetPlayerName();
            if (targetPlayerName != null) {
                String targetUUID = getPlayerUUIDByName(targetPlayerName);
                if (targetUUID != null) {
                    String path = "bank." + targetUUID + "." + targetPlayerName + ".level";
                    String levelString = bankManager.getBank().getString(path, "NULL"); // Obtener el nivel del jugador objetivo
                    int level = 0;
                    try {
                        level = Integer.parseInt(levelString);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    return String.valueOf(getMaxStorageAmount(level)); // Retorna el almacenamiento máximo basado en el nivel
                } else {
                    return "NULL";
                }
            } else {
                return "NULL";
            }
        }*/
// ---------------------------------------------------- ------ ---------------------------------------------------- //

        // %minebank_amount%
        //if (identifier.equals("amount")) {
            //SubComandoAdd depositSuccess amount
            //return "";
        //}

        return null;
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
    private int getMaxBankLevel() {
        int maxLevel = 1; // Valor inicial, asegúrate de que sea válido en tu contexto
        ConfigurationSection levelsSection = plugin.getConfig().getConfigurationSection("bank.level");

        if (levelsSection != null) {
            Set<String> levels = levelsSection.getKeys(false);
            for (String level : levels) {
                try {
                    int levelNumber = Integer.parseInt(level);
                    if (levelNumber > maxLevel) {
                        maxLevel = levelNumber;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace(); // Maneja la excepción si el nivel no es un número válido
                }
            }
        } else {
            System.out.println("La sección de configuración 'bank.level' es nula.");
        }
        return maxLevel;
    }
    private String getPlayerUUIDByName(String playerName) {
        FileConfiguration bankConfig = bankManager.getBank(); // Obtener la configuración actualizada
        // Recorre todas las entradas en bank.yml para encontrar el UUID correspondiente al nombre del jugador
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            ConfigurationSection playerSection = bankConfig.getConfigurationSection("bank." + uuid);
            for (String name : playerSection.getKeys(false)) {
                if (name.equalsIgnoreCase(playerName)) {
                    return uuid;
                }
            }
        }
        return null; // Retorna null si no se encuentra el jugador
    }
}

// %minebank_balance_<player>%
        /*if (identifier.startsWith("balance_")) {
            String targetPlayerName = identifier.substring(8); // Obtener el nombre después de "balance_"
            uuid = getPlayerUUIDByName(targetPlayerName);
            if (uuid != null) {
                return getBalanceForPlayer(uuid, targetPlayerName);
            } else {
                return null;
            }
        }*/