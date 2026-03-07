package com.Guayand0.api;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.zlib.PlayerUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class PAPIVariables extends PlaceholderExpansion {

    // We get an instance of the plugin later.
    private final MineBank plugin;
    private final Economy economy;
    private final DataStorage dataStorage;

    private final PlayerUtils PU = new PlayerUtils();

    public PAPIVariables(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.dataStorage = plugin.getStorage();
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

        String playerName = player.getName();

        int playerEconomyBalance = PU.getPlayerBalance(player, economy);

        // Cargar datos del jugador
        PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());

        String bankName = playerData.getBank().getName();
        int bankLevel = playerData.getBank().getLevel();
        int bankBalance = playerData.getBank().getBalance();
        int offlineProfitAccrued = playerData.getBank().getOffline().getAccrued_profit();
        int offlineProfitTimes = playerData.getBank().getOffline().getProfit_times();

        // Cargar datos del banco del jugador
        Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
        BankData bankData = bankDataMap.get(bankName);

        int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
        int upgradeCost = bankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();
        int bankMaxLevel = bankData.getLevels().size();

        int playerTop = plugin.getPlayerTopPosition(PU.getUUIDFromName(playerName));

// ----------------------------------------------------- USER ----------------------------------------------------- //

        // %minebank_p_name%
        if (identifier.equals("p_name")) {
            try {
                return playerName; // Retorna el nombre del jugador actual
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_name%
        if (identifier.equals("p_b_name")) {
            try {
                return bankName; // Retorna el nombre del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_lvl%
        if (identifier.equals("p_b_lvl")) {
            try {
                return String.valueOf(bankLevel); // Retorna el nivel del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_bal%
        if (identifier.equals("p_b_bal")) {
            try {
                return String.valueOf(bankBalance); // Retorna el dinero del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_next_lvl_cost%
        if (identifier.equals("p_b_next_lvl_cost")) {
            try {
                return String.valueOf(upgradeCost); // Retorna el precio del siguiente nivel
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_max_lvl%
        if (identifier.equals("p_b_max_lvl")) {
            try {
                return String.valueOf(bankMaxLevel); // Retorna el nivel maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_max_bal%
        if (identifier.equals("p_b_max_bal")) {
            try {
                return String.valueOf(bankMaxBalance); // Retorna el dinero maximo del banco
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_offline_profit_accrued%
        if (identifier.equals("p_b_offline_profit_accrued")) {
            try {
                return String.valueOf(offlineProfitAccrued); // Retorna el beneficio offline acumulado
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_offline_profit_times%
        if (identifier.equals("p_b_offline_profit_times")) {
            try {
                return String.valueOf(offlineProfitTimes); // Retorna la cantidad de veces obtenidas el veneficio offline
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_top_position%
        if (identifier.equals("p_b_top_position")) {
            try {
                return String.valueOf(playerTop); // Retorna la posición del top del jugador
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_economy_bal%
        if (identifier.equals("p_economy_bal")) {
            try {
                return String.valueOf(playerEconomyBalance); // Retorna el dinero de economía
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_is_max_lvl%
        if (identifier.equals("p_b_is_max_lvl")) {
            try {
                return String.valueOf(bankLevel >= bankMaxLevel);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_can_lvlup%
        if (identifier.equals("p_b_can_lvlup")) {
            try {
                return String.valueOf(bankBalance >= upgradeCost);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_lvlup_filled%
        if (identifier.equals("p_b_percent_lvlup_filled")) {
            try {
                return formatPercent((double) bankBalance / upgradeCost * 100);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_lvlup_remaining%
        if (identifier.equals("p_b_percent_lvlup_remaining")) {
            try {
                return formatPercent(100 - ((double) bankBalance / upgradeCost * 100));
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_bal_filled%
        if (identifier.equals("p_b_percent_bal_filled")) {
            try {
                return formatPercent((double) bankBalance / bankMaxBalance * 100);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_bal_free%
        if (identifier.equals("p_b_percent_bal_free")) {
            try {
                return formatPercent(100 - ((double) bankBalance / bankMaxBalance * 100));
            } catch (Exception e) {
                return "NULL";
            }
        }

// ---------------------------------------------------- TARGET ---------------------------------------------------- //

        // %minebank_p_name_<player>%
        if (identifier.startsWith("p_name_")) {
            try {
                String prefix = "p_name_";
                String targetPlayerName = identifier.substring(prefix.length()); // Extrae el nombre del target

                // Verifica si el jugador está en la lista de jugadores del banco
                if (dataStorage.getAllPlayerNames().contains(targetPlayerName)) {
                    return targetPlayerName; // Retorna el nombre del jugador si existe
                } else {
                    return "NULL"; // Retorna NULL si no está en la lista
                }
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_name_<player>%
        if (identifier.startsWith("p_b_name_")) {
            try {
                String prefix = "p_b_name_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                return targetData.getBank().getName();
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_lvl_<player>%
        if (identifier.startsWith("p_b_lvl_")) {
            try {
                String prefix = "p_b_lvl_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                int targetBankLevel = targetData.getBank().getLevel();
                return String.valueOf(targetBankLevel);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_bal_<player>%
        if (identifier.startsWith("p_b_bal_")) {
            try {
                String prefix = "p_b_bal_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                int targetBankBalance = targetData.getBank().getBalance();
                return String.valueOf(targetBankBalance);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_next_lvl_cost_<player>%
        if (identifier.startsWith("p_b_next_lvl_cost_")) {
            try {
                String prefix = "p_b_next_lvl_cost_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();

                // Cargar datos del banco del jugador
                Map<String, BankData> targetBankDataMap = dataStorage.loadBankData(targetBankName);
                BankData targetBankData = targetBankDataMap.get(targetBankName);
                int targetUpgradeCost = targetBankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();
                return String.valueOf(targetUpgradeCost);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_max_lvl_<player>%
        if (identifier.startsWith("p_b_max_lvl_")) {
            try {
                String prefix = "p_b_max_lvl_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();

                // Cargar datos del banco del jugador
                Map<String, BankData> targetBankDataMap = dataStorage.loadBankData(targetBankName);
                BankData targetBankData = targetBankDataMap.get(targetBankName);
                int targetBankMaxLevel = targetBankData.getLevels().size();
                return String.valueOf(targetBankMaxLevel);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_max_bal_<player>%
        if (identifier.startsWith("p_b_max_bal_")) {
            try {
                String prefix = "p_b_max_bal_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();

                // Cargar datos del banco del jugador
                Map<String, BankData> targetBankDataMap = dataStorage.loadBankData(targetBankName);
                BankData targetBankData = targetBankDataMap.get(targetBankName);
                int targetBankMaxBalance = targetBankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
                return String.valueOf(targetBankMaxBalance);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_offline_profit_accrued_<player>%
        if (identifier.startsWith("p_b_offline_profit_accrued_")) {
            try {
                String prefix = "p_b_offline_profit_accrued_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));

                int targetOfflineProfitAccrued = targetData.getBank().getOffline().getAccrued_profit();
                return String.valueOf(targetOfflineProfitAccrued);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_offline_profit_times_<player>%
        if (identifier.startsWith("p_b_offline_profit_times_")) {
            try {
                String prefix = "p_b_offline_profit_times_";
                String targetPlayerName = identifier.substring(prefix.length());

                // Cargar datos del jugador
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));

                int targetOfflineProfitTimes = targetData.getBank().getOffline().getAccrued_profit();
                return String.valueOf(targetOfflineProfitTimes);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_top_position_<player>%
        if (identifier.startsWith("p_b_top_position_")) {
            try {
                String prefix = "p_b_top_position_";
                String targetPlayerName = identifier.substring(prefix.length());
                int targetPlayerTop = plugin.getPlayerTopPosition(PU.getUUIDFromName(targetPlayerName));
                return String.valueOf(targetPlayerTop);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_economy_bal_<player>%
        if (identifier.startsWith("p_economy_bal_")) {
            try {
                String prefix = "p_economy_bal_";
                String targetPlayerName = identifier.substring(prefix.length());
                Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                int targetEconomyBalance = PU.getPlayerBalance(targetPlayer, economy);

                if (targetPlayer != null && targetPlayer.isOnline()) {
                    return String.valueOf(targetEconomyBalance);
                } else {
                    return "NOT_ONLINE";
                }
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_is_max_lvl_<player>%
        if (identifier.startsWith("p_b_is_max_lvl_")) {
            try {
                String prefix = "p_b_is_max_lvl_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                return String.valueOf(targetData.getBank().getLevel() >= targetBankData.getLevels().size());
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_can_lvlup_<player>%
        if (identifier.startsWith("p_b_can_lvlup_")) {
            try {
                String prefix = "p_b_can_lvlup_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                int cost = targetBankData.getLevels().get(String.valueOf(targetData.getBank().getLevel())).getUpgrade_cost();
                return String.valueOf(targetData.getBank().getBalance() >= cost);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_lvlup_filled_<player>%
        if (identifier.startsWith("p_b_percent_lvlup_filled_")) {
            try {
                String prefix = "p_b_percent_lvlup_filled_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                int cost = targetBankData.getLevels().get(String.valueOf(targetData.getBank().getLevel())).getUpgrade_cost();
                double percent = Math.min(100, (double) targetData.getBank().getBalance() / cost * 100);
                return formatPercent(percent);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_lvlup_remaining_<player>%
        if (identifier.startsWith("p_b_percent_lvlup_remaining_")) {
            try {
                String prefix = "p_b_percent_lvlup_remaining_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                int cost = targetBankData.getLevels().get(String.valueOf(targetData.getBank().getLevel())).getUpgrade_cost();
                double percent = 100 - ((double) targetData.getBank().getBalance() / cost * 100);
                percent = Math.max(0, percent);
                return formatPercent(percent);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_bal_filled_<player>%
        if (identifier.startsWith("p_b_percent_bal_filled_")) {
            try {
                String prefix = "p_b_percent_bal_filled_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                int max = targetBankData.getLevels().get(String.valueOf(targetData.getBank().getLevel())).getMax_balance();
                double percent = Math.min(100, (double) targetData.getBank().getBalance() / max * 100);
                return formatPercent(percent);
            } catch (Exception e) {
                return "NULL";
            }
        }

        // %minebank_p_b_percent_bal_free_<player>%
        if (identifier.startsWith("p_b_percent_bal_free_")) {
            try {
                String prefix = "p_b_percent_bal_free_";
                String targetPlayerName = identifier.substring(prefix.length());
                PlayerData targetData = dataStorage.loadPlayerData(PU.getUUIDFromName(targetPlayerName));
                String targetBankName = targetData.getBank().getName();
                BankData targetBankData = dataStorage.loadBankData(targetBankName).get(targetBankName);
                int max = targetBankData.getLevels().get(String.valueOf(targetData.getBank().getLevel())).getMax_balance();
                double percent = 100 - ((double) targetData.getBank().getBalance() / max * 100);
                percent = Math.max(0, percent);
                return formatPercent(percent);
            } catch (Exception e) {
                return "NULL";
            }
        }

// ----------------------------------------------------- TOP ------------------------------------------------------ //

        // %minebank_top_b_data_<numero>%
        if (identifier.startsWith("top_b_data_")) {
            try {
                String prefix = "top_b_data_";
                int rank = Integer.parseInt(identifier.substring(prefix.length())); // Extrae el número del identificador

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return "Player: " + bankInfo.get(0) + ", Bank: " + bankInfo.get(1) + ", Level: " + bankInfo.get(2) + ", Balance: " + bankInfo.get(3); // Retorna todos los datos
                } else {
                    return "NEGATIVE_VALUE"; // Si el numero es demasiado bajo
                }
            } catch (NumberFormatException e) {
                return "NOT_A_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "NULL"; // Si falla otra cosa
            }
        }

        // %minebank_top_p_name_<numero>%
        if (identifier.startsWith("top_p_name_")) {
            try {
                String prefix = "top_p_name_";
                int rank = Integer.parseInt(identifier.substring(prefix.length())); // Extrae el número del identificador

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(0); // Retorna el nombre del jugador
                } else {
                    return "NEGATIVE_VALUE"; // Si el numero es demasiado bajo
                }
            } catch (NumberFormatException e) {
                return "NOT_A_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "NULL"; // Si falla otra cosa
            }
        }

        // %minebank_top_b_name_<numero>%
        if (identifier.startsWith("top_b_name_")) {
            try {
                String prefix = "top_b_name_";
                int rank = Integer.parseInt(identifier.substring(prefix.length())); // Extrae el número del identificador

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(1); // Retorna el nombre del banco
                } else {
                    return "NEGATIVE_VALUE"; // Si el numero es demasiado bajo
                }
            } catch (NumberFormatException e) {
                return "NOT_A_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "NULL"; // Si falla otra cosa
            }
        }

        // %minebank_top_b_bal_<numero>%
        if (identifier.startsWith("top_b_bal_")) {
            try {
                String prefix = "top_b_bal_";
                int rank = Integer.parseInt(identifier.substring(prefix.length())); // Extrae el número del identificador

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(3); // Retorna el balance
                } else {
                    return "NEGATIVE_VALUE"; // Si el numero es demasiado bajo
                }
            } catch (NumberFormatException e) {
                return "NOT_A_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "NULL"; // Si falla otra cosa
            }
        }

        // %minebank_top_b_lvl_<numero>%
        if (identifier.startsWith("top_b_lvl_")) {
            try {
                String prefix = "top_b_lvl_";
                int rank = Integer.parseInt(identifier.substring(prefix.length())); // Extrae el número del identificador

                List<List<String>> topBanks = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE); // Obtén el top de jugadores
                if (rank <= topBanks.size() && rank > 0) {
                    List<String> bankInfo = topBanks.get(rank - 1); // Resta 1 porque el ranking es 1-based
                    return bankInfo.get(2); // Retorna el nivel
                } else {
                    return "NEGATIVE_VALUE"; // Si el numero es demasiado bajo
                }
            } catch (NumberFormatException e) {
                return "NOT_A_NUMBER"; // Si no es un numero
            } catch (Exception e) {
                return "NULL"; // Si falla otra cosa
            }
        }

// ----------------------------------------------------- --- ------------------------------------------------------ //



// --------------------------------------------------- -------- --------------------------------------------------- //
        return null;
    }

    // Función auxiliar
    private String formatPercent(double percent) {
        percent = Math.max(0, Math.min(100, percent)); // Asegura rango 0-100
        double rounded = Math.round(percent * 10.0) / 10.0; // Redondea a 1 decimal
        if (rounded == (int) rounded) { // Si no tiene decimal
            return String.format("%d%%", (int) rounded);
        } else {
            return String.format("%.1f%%", rounded);
        }
    }
}
