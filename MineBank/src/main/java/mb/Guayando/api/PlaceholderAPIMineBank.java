package mb.Guayando.api;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderAPIMineBank extends PlaceholderExpansion {

    // We get an instance of the plugin later.
    private final MineBank plugin;

    public PlaceholderAPIMineBank(MineBank plugin) {
        this.plugin = plugin;
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
        return "minebank"; // %minebank_XXXXX%
    }
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        plugin.getLanguageManager().reloadLanguage();
        plugin.getBankManager().reloadBank();
        plugin.reloadConfig();
        MessageUtils.updatePlayerBankData(player, plugin);

        if (player == null) {
            return "";
        }

        String playerName = player.getName();

// ----------------------------------------------------- USER ----------------------------------------------------- //

        // %minebank_player_name%
        if (identifier.equals("player_name")) {
            return playerName; // Retorna el nombre del jugador actual
        }

        // %minebank_player_bank_name%
        if (identifier.equals("player_bank_name")) {
            return MessageUtils.getPlayerBankName(); // Retorna el nombre del banco
        }

        // %minebank_player_bank_level%
        if (identifier.equals("player_bank_level")) {
            return String.valueOf(MessageUtils.getPlayerBankLevel()); // Retorna el nivel del banco
        }

        // %minebank_player_bank_next_level_price%
        if (identifier.equals("player_bank_next_level_price")) {
            return String.valueOf(MessageUtils.getPlayerLevelPrice()); // Retorna el precio del siguiente nivel
        }

        // %minebank_player_bank_max_level%
        if (identifier.equals("player_bank_max_level")) {
            return String.valueOf(MessageUtils.getPlayerBankMaxLevel()); // Retorna el nivel maximo del banco
        }

        // %minebank_player_bank_balance%
        if (identifier.equals("player_bank_balance")) {
            return String.valueOf(MessageUtils.getPlayerBankBalance()); // Retorna el dinero del banco
        }

        // %minebank_player_bank_max_balance%
        if (identifier.equals("player_bank_max_balance")) {
            return String.valueOf(MessageUtils.getPlayerBankMaxStorage()); // Retorna el dinero maximo del banco
        }

        // %minebank_player_bank_top_position%
        if (identifier.equals("player_bank_top_position")) {
            return String.valueOf(MessageUtils.getPlayerBankTop()); // Retorna la posicion del top del jugador
        }

        // %minebank_player_balance%
        if (identifier.equals("player_balance")) {
            return String.valueOf(MessageUtils.getPlayerBalance()); // Retorna el dinero de economia
        }


// ---------------------------------------------------- TARGET ---------------------------------------------------- //


// ----------------------------------------------------- TOP ------------------------------------------------------ //


// --------------------------------------------------- -------- --------------------------------------------------- //
        return null;
    }
}