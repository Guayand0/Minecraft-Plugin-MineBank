package com.Guayand0.api;

import com.Guayand0.Data.BankData;
import com.Guayand0.Data.BankManager;
import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderAPIMineBank extends PlaceholderExpansion {

    // We get an instance of the plugin later.
    private final MineBank plugin;
    private final BankUtils BU = new BankUtils();
    private final BankManager BM = new BankManager();

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
        return plugin.pluginName;
    }
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        if (player == null) return "";

// ----------------------------------------------------- USER ----------------------------------------------------- //

        // %minebank_player_name%
        if (identifier.equals("player_name")) {
            return player.getName(); // Retorna el nombre del jugador actual
        }

        // %minebank_player_bank_name%
        if (identifier.equals("player_bank_name")) {
            try {
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, player.getName());
                return bankData.getBankName(); // Retorna el nombre del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_level%
        if (identifier.equals("player_bank_level")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getBankLevel()); // Retorna el nivel del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_balance%
        if (identifier.equals("player_bank_balance")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getBankBalance()); // Retorna el dinero del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_next_level_cost%
        if (identifier.equals("player_bank_next_level_cost")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getBankLevelUpgradeCost()); // Retorna el precio del siguiente nivel
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_max_level%
        if (identifier.equals("player_bank_max_level")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getBankMaxLevel()); // Retorna el nivel maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_max_balance%
        if (identifier.equals("player_bank_max_balance")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getBankMaxBalance()); // Retorna el dinero maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_offline_profit_accrued%
        if (identifier.equals("player_bank_offline_profit_accrued")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getOfflineProfitAccrued()); // Retorna el beneficio offline acumulado
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_offline_profit_times%
        if (identifier.equals("player_bank_offline_profit_times")) {
            try {
                BankData bankData = BM.getBankData(plugin, player.getName());
                return String.valueOf(bankData.getOfflineProfitTimes()); // Retorna la cantidad de veces obtenidas el veneficio offline
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_top_position%
        if (identifier.equals("player_bank_top_position")) {
            try {
                return String.valueOf(BU.getPlayerBankTop(plugin, player.getName())); // Retorna la posición del top del jugador
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_economy_balance%
        if (identifier.equals("player_economy_balance")) {
            try {
                return String.valueOf(BU.getPlayerBalance(player, plugin.getEconomy())); // Retorna el dinero de economía
            } catch (Exception e) {
                return "NULL";
            }
        }

// ---------------------------------------------------- TARGET ---------------------------------------------------- //

        // %minebank_player_name_<target>%
        if (identifier.startsWith("player_name_")) {
            try {
                String targetPlayerName = identifier.substring(12); // Extrae el nombre del target

                // Verifica si el jugador está en la lista de jugadores del banco
                if (BU.getPlayerNameOfBank(plugin).contains(targetPlayerName)) {
                    return targetPlayerName; // Retorna el nombre del jugador si existe
                } else {
                    return "NULL"; // Retorna NULL si no está en la lista
                }
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_name_<target>%
        if (identifier.startsWith("player_bank_name_")) {
            try {
                String targetPlayerName = identifier.substring(17);
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return bankData.getBankName();
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_level_<player>%
        if (identifier.startsWith("player_bank_level_")) {
            try {
                String targetPlayerName = identifier.substring(18);
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getBankLevel());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_balance_<player>%
        if (identifier.startsWith("player_bank_balance_")) {
            try {
                String targetPlayerName = identifier.substring(20);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getBankBalance());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_next_level_cost_<player>%
        if (identifier.startsWith("player_bank_next_level_cost_")) {
            try {
                String targetPlayerName = identifier.substring(28);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getBankLevelUpgradeCost());
            } catch (Exception e) {
                return "NULL";
            }
        }
        // %minebank_player_bank_max_level_<player>%
        if (identifier.startsWith("player_bank_max_level_")) {
            try {
                String targetPlayerName = identifier.substring(22);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getBankMaxLevel());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_max_balance_<player>%
        if (identifier.startsWith("player_bank_max_balance_")) {
            try {
                String targetPlayerName = identifier.substring(24);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getBankMaxBalance());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_offline_profit_accrued_<player>%
        if (identifier.startsWith("player_bank_offline_profit_accrued_")) {
            try {
                String targetPlayerName = identifier.substring(35);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getOfflineProfitAccrued());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_offline_profit_times_<player>%
        if (identifier.startsWith("player_bank_offline_profit_times_")) {
            try {
                String targetPlayerName = identifier.substring(33);
                // Obtener datos del banco del jugador y maximos de nivel y balance
                BankData bankData = BM.getBankData(plugin, targetPlayerName);
                return String.valueOf(bankData.getOfflineProfitTimes());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_bank_top_position_<player>%
        if (identifier.startsWith("player_bank_top_position_")) {
            try {
                String targetPlayerName = identifier.substring(25);
                return String.valueOf(BU.getPlayerBankTop(plugin, targetPlayerName));
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_player_economy_balance_<player>%
        if (identifier.startsWith("player_economy_balance_")) {
            try {
                String targetPlayerName = identifier.substring(23);
                Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    return String.valueOf(BU.getPlayerBalance(targetPlayer, plugin.getEconomy()));
                }
                return "NOT_ONLINE";
            } catch (Exception e) {
                return "NULL";
            }
        }

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