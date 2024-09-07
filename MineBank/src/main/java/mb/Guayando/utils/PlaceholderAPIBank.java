/*package mb.Guayando.utils;

import mb.Guayando.MineBank;
import mb.Guayando.config.BankManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


public class PlaceholderAPIBank extends PlaceholderExpansion {

     // We get an instance of the plugin later.
    private MineBank plugin;
    private BankManager bankManager;

    public PlaceholderAPIBank(MineBank plugin) {
        this.plugin = plugin;
        this.bankManager = bankManager;
    }

     //
     // Because this is an internal class,
     // you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     // PlaceholderAPI is reloaded
     //
     // @return true to persist through reloads
     //
    @Override
    public boolean persist(){
        return true;
    }
     //
     // Since this expansion requires api access to the plugin "SomePlugin"
     // we must check if said plugin is on the server or not.
     //
     // @return true or false depending on if the required plugin is installed.
     //
    @Override
    public boolean canRegister(){
        return true;
    }

     //
     // The name of the person who created this expansion should go here.
     //
     // @return The name of the author as a String.
     //
    @Override
    public String getAuthor(){
        return "Guayando";
    }

     //
     // The placeholder identifier should go here.
     // <br>This is what tells PlaceholderAPI to call our onRequest
     // method to obtain a value if a placeholder starts with our
     // identifier.
     // <br>This must be unique and can not contain % or _
     //
     // @return The identifier in {@code %<identifier>_<value>%} as String.
     //
    @Override
    public String getIdentifier(){
        return "minebank";
    }

     //
     // This is the version of this expansion.
     // <br>You don't have to use numbers, since it is set as a String.
     //
     // @return The version as a String.
     //
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

     //
     // This is the method called when a placeholder with our identifier
     // is found and needs a value.
     // <br>We specify the value identifier in this method.
     // <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     //
     // @param  player
     //         A {@link Player; Player}.
     // @param  identifier
     //         A String containing the identifier/value.
     //
     // @return possibly-null String of the requested identifier.
     //



    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }

        String uuid = player.getUniqueId().toString();
        String playerName = player.getName();

        // %minebank_balance%
        if (identifier.equals("balance")) {
            String path = "bank." + uuid + "." + playerName + ".balance";
            return bankManager.getBank().getString(path, null); // Devuelve null si no se encuentra el valor
        }

        // %minebank_level%
        if (identifier.equals("level")) {
            String path = "bank." + uuid + "." + playerName + ".level";
            return bankManager.getBank().getString(path, null); // Devuelve null si no se encuentra el valor
        }

        // %minebank_balance_<player>%
        if (identifier.startsWith("balance_")) {
            String targetPlayerName = identifier.substring(8); // Obtener el nombre después de "balance_"
            uuid = getPlayerUUIDByName(targetPlayerName);
            if (uuid != null) {
                return getBalanceForPlayer(uuid, targetPlayerName);
            } else {
                return null;
            }
        }

        // %minebank_level_<player>%
        if (identifier.startsWith("level_")) {
            String targetPlayerName = identifier.substring(6); // Obtener el nombre después de "level_"
            uuid = getPlayerUUIDByName(targetPlayerName);
            if (uuid != null) {
                return getLevelForPlayer(uuid, targetPlayerName);
            } else {
                return null;
            }
        }


        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
        // was provided
        return null;
    }
    private String getBalanceForPlayer(String uuid, String playerName) {
        String path = "bank." + uuid + "." + playerName + ".balance";
        return bankManager.getBank().getString(path, null); // Devuelve null si no se encuentra el valor
    }

    private String getLevelForPlayer(String uuid, String playerName) {
        String path = "bank." + uuid + "." + playerName + ".level";
        return bankManager.getBank().getString(path, null); // Devuelve null si no se encuentra el valor
    }
    
     // Suponiendo que tienes un método que obtiene el UUID basado en el nombre del jugador
    private String getPlayerUUIDByName(String playerName) {
        // Recorre todas las entradas en bank.yml para encontrar el UUID correspondiente al nombre del jugador
        for (String uuid : bankManager.getBank().getConfigurationSection("bank").getKeys(false)) {
            ConfigurationSection playerSection = bankManager.getBank().getConfigurationSection("bank." + uuid);
            for (String name : playerSection.getKeys(false)) {
                if (name.equalsIgnoreCase(playerName)) {
                    return uuid;
                }
            }
        }
        return null; // Retorna null si no se encuentra el jugador
    }
}*/