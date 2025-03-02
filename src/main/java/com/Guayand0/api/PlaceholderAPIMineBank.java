package com.Guayand0.api;

import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderAPIMineBank extends PlaceholderExpansion {

    // We get an instance of the plugin later.
    private final MineBank plugin;
    private final BankUtils BU = new BankUtils();

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
        return "Guayand0";
    }
    @Override
    public String getIdentifier() {
        return plugin.pluginName; // %minebank_XXXXX%
    }
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        if (player == null) {
            return "";
        }

// ----------------------------------------------------- USER ----------------------------------------------------- //

        // %minebank_player_name%
        if (identifier.equals("player_name")) {
            return player.getName(); // Retorna el nombre del jugador actual
        }

        // %minebank_player_bank_name%
        if (identifier.equals("player_bank_name")) {
            try {
                return BU.getPlayerBankName(BU.getBankDataOfPlayerName(plugin, player.getName())); // Retorna el nombre del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_level%
        if (identifier.equals("player_bank_level")) {
            try {
                return String.valueOf(BU.getPlayerBankLevel(BU.getBankDataOfPlayerName(plugin, player.getName()))); // Retorna el nivel del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_balance%
        if (identifier.equals("player_bank_balance")) {
            try {
                return String.valueOf(BU.getPlayerBankBalance(BU.getBankDataOfPlayerName(plugin, player.getName()))); // Retorna el dinero del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_next_level_cost%
        if (identifier.equals("player_bank_next_level_cost")) {
            try {
                return String.valueOf(BU.getBankUpgradeCostByLevel(plugin, player.getName())); // Retorna el precio del siguiente nivel
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_max_level%
        if (identifier.equals("player_bank_max_level")) {
            try {
                return String.valueOf(BU.getBankMaxLevel(plugin, player.getName())); // Retorna el nivel maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_max_balance%
        if (identifier.equals("player_bank_max_balance")) {
            try {
                return String.valueOf(BU.getBankMaxBalanceByLevel(plugin, player.getName())); // Retorna el dinero maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_top_position%
        if (identifier.equals("player_bank_top_position")) {
            try {
                return String.valueOf(BU.getPlayerBankTop(plugin, player)); // Retorna la posicion del top del jugador
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_economy_balance%
        if (identifier.equals("player_economy_balance")) {
            try {
                return String.valueOf(BU.getPlayerBalance(player, plugin.getEconomy())); // Retorna el dinero de economia
            } catch (Exception e) {
                return "NULL";
            }
        }


// ---------------------------------------------------- TARGET ---------------------------------------------------- //



// ----------------------------------------------------- TOP ------------------------------------------------------ //

        // %minebank_top_player_bank_data_<numero>%
        if (identifier.startsWith("top_player_bank_data_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(21)); // Extrae el número del identificador

                List<List<String>> topBanks = BU.getTopPlayerBanks(plugin, Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return "Player: " + bankInfo.get(0) + ", Bank: " + bankInfo.get(1) + ", Level: " + bankInfo.get(2) + ", Balance: " + bankInfo.get(3); // Retorna todos los datos
                } else {
                    return "NULL"; // Si el numero es demasiado alto
                }
            } catch (NumberFormatException e) {
                return "INVALID_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "ERROR"; // Si falla otra cosa
            }
        }

        // %minebank_top_player_name_<numero>%
        if (identifier.startsWith("top_player_name_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(16)); // Extrae el número del identificador

                List<List<String>> topBanks = BU.getTopPlayerBanks(plugin, Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(0); // Retorna el nombre del jugador
                } else {
                    return "NULL"; // Si el numero es demasiado alto
                }
            } catch (NumberFormatException e) {
                return "INVALID_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "ERROR"; // Si falla otra cosa
            }
        }

        // %minebank_top_player_bank_balance_<numero>%
        if (identifier.startsWith("top_player_bank_balance_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(24)); // Extrae el número del identificador

                List<List<String>> topBanks = BU.getTopPlayerBanks(plugin, Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(3); // Retorna el balance
                } else {
                    return "NULL"; // Si el numero es demasiado alto
                }
            } catch (NumberFormatException e) {
                return "INVALID_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "ERROR"; // Si falla otra cosa
            }
        }

        // %minebank_top_player_bank_level_<numero>%
        if (identifier.startsWith("top_player_bank_level_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(22)); // Extrae el número del identificador

                List<List<String>> topBanks = BU.getTopPlayerBanks(plugin, Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(2); // Retorna el nivel
                } else {
                    return "NULL"; // Si el numero es demasiado alto
                }
            } catch (NumberFormatException e) {
                return "INVALID_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "ERROR"; // Si falla otra cosa
            }
        }

        // %minebank_top_player_bank_name_<numero>%
        if (identifier.startsWith("top_player_bank_name_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(21)); // Extrae el número del identificador

                List<List<String>> topBanks = BU.getTopPlayerBanks(plugin, Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(1); // Retorna el nombre del banco
                } else {
                    return "NULL"; // Si el numero es demasiado alto
                }
            } catch (NumberFormatException e) {
                return "INVALID_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "ERROR"; // Si falla otra cosa
            }
        }

// --------------------------------------------------- -------- --------------------------------------------------- //
        return null;
    }
}