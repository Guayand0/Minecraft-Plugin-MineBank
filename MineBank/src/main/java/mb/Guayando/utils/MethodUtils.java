package mb.Guayando.utils;

import mb.Guayando.MineBank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class MethodUtils {

    // String bankName = bankConfig.getString(playerPath + ".bank-name");
    // MethodUtils.getMaxStorageAmount(plugin, bankName, level);
    public static int getMaxStorageAmount(MineBank plugin, String bankName, int level) {
        // Obtener el máximo almacenamiento del banco desde la configuración del plugin
        String levelData = plugin.getConfig().getString("bank." + bankName + ".level." + level);

        if (levelData != null && levelData.contains(";")) {
            String[] parts = levelData.split(";");
            if (parts.length > 0) {
                try {
                    return Integer.parseInt(parts[0]); // Retorna el primer valor como el máximo almacenamiento
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0; // Valor por defecto si no se puede obtener el almacenamiento máximo
    }

    public static int getLevelPrice(MineBank plugin, String bankName, int level) {
        // Obtener el máximo almacenamiento del banco desde la configuración del plugin
        String levelData = plugin.getConfig().getString("bank." + bankName + ".level." + level);

        if (levelData != null && levelData.contains(";")) {
            String[] parts = levelData.split(";");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1]); // Retorna el segundo valor como el precio del siguiente nivel
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    // String targetUUID = MethodUtils.getPlayerUUIDByName(plugin, targetPlayerName);
    public static String getPlayerUUIDByName(MineBank plugin, String playerName) {
        FileConfiguration bankConfig = plugin.getBankManager().getBank(); // Obtener la configuración actualizada
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

    public static int getMaxBankLevel(MineBank plugin, String bankName) {
        int maxLevel = 1;
        Set<String> levels = plugin.getConfig().getConfigurationSection("bank." + bankName + ".level").getKeys(false);
        for (String level : levels) {
            try {
                int levelNumber = Integer.parseInt(level);
                if (levelNumber > maxLevel) {
                    maxLevel = levelNumber;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return maxLevel;
    }

    public static int getInterestPercentage(MineBank plugin) {
        return plugin.getConfig().getInt("bank.interests.withdrawPercentage");
    }

    public static int getAmountReceived(int amount, int interestPercentage) {
        // Calcular el total a recibir con intereses
        double interest = amount * (interestPercentage / 100.0);
        return amount - (int) Math.round(interest);
    }

    // Método para obtener el profit redondeado
    public static int getRoundedProfit(MineBank plugin, Player player) {
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        int balance = plugin.getBankManager().getBank().getInt(playerPath + ".balance");
        int profitPercentage = plugin.getConfig().getInt("bank.profit.keepInBankPercentage");

        long profit = (long) Math.floor(balance * (profitPercentage / 100.0));
        return (int) profit; // Profit redondeado
    }

    // Método para obtener el porcentaje de profit
    public static int getProfitPercentage(MineBank plugin) {
        return plugin.getConfig().getInt("bank.profit.keepInBankPercentage");
    }

    // Método para obtener la minima cantidad para recibir profit
    public static int getMinAmountToWinProfit(MineBank plugin) {
        return plugin.getConfig().getInt("bank.profit.minBankBalanceToApply");
    }

    public static List<String> replacePlaceholders(List<String> lore, Player player, MineBank plugin) {
        // Aquí puedes obtener los valores necesarios, por ejemplo, desde el Economy o desde una configuración.
        List<String> updatedLore = new ArrayList<>();
        for (String line : lore) {
            // Reemplazar placeholders dinámicos como %playerName<1>%
            updatedLore.add(MessageUtils.applyPlaceholdersAndColor(player, null, replaceTopPlaceholders(line, plugin), plugin, 0));
        }
        return updatedLore;
    }

    public static String replaceTopPlaceholders(String line, MineBank plugin) {
        FileConfiguration bankConfig = plugin.getBankManager().getBank();
        Map<String, Integer> topBalances = new HashMap<>();
        Map<String, String> topBankNames = new HashMap<>(); // Para almacenar los nombres de los bancos

        // Recolectar balances de los jugadores y nombres de los bancos
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                String bankName = bankConfig.getString("bank." + uuid + "." + playerName + ".bank-name");
                topBalances.put(playerName, balance);
                topBankNames.put(playerName, bankName); // Guardar el nombre del banco
            }
        }

        // Ordenar los balances en orden descendente
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // Reemplazar placeholders para los primeros 50 tops
        for (int i = 1; i <= 50; i++) { // Se asume que quieres reemplazar hasta 50 tops
            String topBankPosition = "%bankTopPosition<" + i + ">%";
            String topBankName = "%bankTopName<" + i + ">%";
            String topBankBalance = "%bankTopBalance<" + i + ">%";
            String topBankBankName = "%bankTopBankName<" + i + ">%";

            if (line.contains(topBankName) || line.contains(topBankBalance) || line.contains(topBankPosition) || line.contains(topBankBankName)) {
                if (i <= sortedList.size()) {
                    Map.Entry<String, Integer> entry = sortedList.get(i - 1);
                    String topPlayerName = entry.getKey();
                    int topBankBalanceValue = entry.getValue();
                    String topBankBankNameValue = topBankNames.get(topPlayerName); // Obtener el nombre del banco

                    line = line.replace(topBankPosition, String.valueOf(i))
                            .replace(topBankName, topPlayerName)
                            .replace(topBankBalance, String.valueOf(topBankBalanceValue));

                    if (topBankBankNameValue != null && !topBankBankNameValue.isEmpty()) {
                        // Solo realiza substring si el valor no es nulo ni vacío
                        line = line.replace(topBankBankName, topBankBankNameValue.substring(0, 1).toUpperCase() + topBankBankNameValue.substring(1).toLowerCase());
                    } else {
                        // Si es nulo o vacío, reemplaza con un valor por defecto
                        line = line.replace(topBankBankName, "N/A");
                    }

                } else {
                    // Si el ranking solicitado está fuera del rango, reemplaza con vacío o mensaje por defecto
                    line = line.replace(topBankPosition, String.valueOf(i))
                            .replace(topBankName, "N/A")
                            .replace(topBankBalance, "0")
                            .replace(topBankBankName, "N/A");
                }
            }
        }

        return line;
    }

    public static int getPlayerTop(Player player, MineBank plugin) {
        FileConfiguration bankConfig = plugin.getBankManager().getBank();

        Map<String, Integer> topBalances = new HashMap<>();

        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                topBalances.put(playerName, balance);
            }
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getKey().equalsIgnoreCase(player.getName())) {
                return i + 1;
            }
        }
        return -1; // Si no está en el top
    }

    // Método que devuelve una lista de mensajes formateados para el top de balances
    public static List<String> getFormattedTopBalances(MineBank plugin, int topN) {
        FileConfiguration bankConfig = plugin.getBankManager().getBank();
        Map<String, Integer> topBalances = new HashMap<>();
        Map<String, String> playerBankNames = new HashMap<>(); // Mapa para almacenar el nombre del banco de cada jugador

        // Iterar sobre todos los jugadores y sus balances
        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                String bankName = bankConfig.getString("bank." + uuid + "." + playerName + ".bank-name");
                topBalances.put(playerName, balance);
                playerBankNames.put(playerName, bankName); // Almacenar el nombre del banco
            }
        }

        // Ordenar los balances en orden descendente
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // Lista para almacenar los mensajes formateados
        List<String> formattedMessages = new ArrayList<>();

        // Formatear los mensajes para los primeros N jugadores
        for (int i = 0; i < Math.min(topN, sortedList.size()); i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            String baltop = plugin.getLanguageManager().getMessage("bank.top.entry");

            if (baltop != null) {
                String playerName = entry.getKey();
                String bankName = playerBankNames.get(playerName); // Obtener el nombre del banco

                // Reemplazar los placeholders
                baltop = baltop.replace("%topPosition%", String.valueOf(i + 1))
                        .replace("%topName%", playerName)
                        .replace("%topBank%", bankName.substring(0, 1).toUpperCase() + bankName.substring(1).toLowerCase())
                        .replace("%topBalance%", String.valueOf(entry.getValue()));

                // Agregar el mensaje formateado a la lista
                formattedMessages.add(baltop);
            }
        }

        return formattedMessages;
    }
}