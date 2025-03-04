package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.google.gson.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BankUtils {

    // ------------------------- GENERAL ------------------------- //

    public JsonObject getBankDataOfPlayerName(MineBank plugin, String playerName) throws IOException {
        // Leer player_data.json
        Reader playerReader = new FileReader(plugin.getFileManager().getPlayerDataFile());
        JsonObject playerData = new JsonParser().parse(playerReader).getAsJsonObject();
        playerReader.close();

        JsonArray players = playerData.getAsJsonArray("player");
        JsonObject playerInfo = null;

        for (JsonElement element : players) {
            JsonObject player_obj = element.getAsJsonObject();
            if (player_obj.get("name").getAsString().equals(playerName)) {
                playerInfo = player_obj;
                break;
            }
        }

        assert playerInfo != null;
        return playerInfo.getAsJsonArray("bank").get(0).getAsJsonObject();
    }

    public List<String> getPlayerNameOfBank(MineBank plugin) throws IOException {
        List<String> playerNames = new ArrayList<>();

        // Leer player_data.json
        Reader playerReader = new FileReader(plugin.getFileManager().getPlayerDataFile());
        JsonObject playerData = new JsonParser().parse(playerReader).getAsJsonObject();
        playerReader.close();

        JsonArray players = playerData.getAsJsonArray("player");

        // Obtener todos los nombres de jugadores que se han conectado
        for (JsonElement element : players) {
            JsonObject player_obj = element.getAsJsonObject();
            playerNames.add(player_obj.get("name").getAsString());
        }

        // Ordenar la lista alfabéticamente
        Collections.sort(playerNames);

        return playerNames;
    }

    public List<List<String>> getTopPlayerBanks(MineBank plugin, int amount) throws IOException {
        List<List<String>> bankInfoList = new ArrayList<>();

        // Leer player_data.json
        Reader playerReader = new FileReader(plugin.getFileManager().getPlayerDataFile());
        JsonObject playerData = new JsonParser().parse(playerReader).getAsJsonObject();
        playerReader.close();

        JsonArray players = playerData.getAsJsonArray("player");

        for (JsonElement element : players) {
            JsonObject playerObj = element.getAsJsonObject();
            JsonArray bankArray = playerObj.getAsJsonArray("bank");

            if (bankArray.size() > 0) {
                JsonObject bankObj = bankArray.get(0).getAsJsonObject();
                List<String> bankInfo = new ArrayList<>();
                bankInfo.add(playerObj.get("name").getAsString()); // Nombre del jugador
                bankInfo.add(bankObj.get("name").getAsString());   // Nombre del banco
                bankInfo.add(String.valueOf(bankObj.get("level").getAsInt())); // Nivel del banco
                bankInfo.add(String.valueOf(bankObj.get("balance").getAsInt())); // Balance del banco
                bankInfoList.add(bankInfo);
            }
        }

        // Ordenar por balance en orden descendente
        bankInfoList.sort((a, b) -> Integer.compare(Integer.parseInt(b.get(3)), Integer.parseInt(a.get(3))));

        // Devolver los 10 primeros o menos si hay menos de 10 jugadores
        return bankInfoList.size() > amount ? bankInfoList.subList(0, amount) : bankInfoList;
    }



    public JsonObject getBankData(MineBank plugin) throws IOException {
        // Leer banks.json
        Reader bankReader = new FileReader(plugin.getFileManager().getBanksFile());
        JsonObject banksData = new JsonParser().parse(bankReader).getAsJsonObject();
        bankReader.close();
        return banksData;
    }

    // ------------------------- PLAYER_DATA.JSON ------------------------- //

    public int getPlayerOfflineAccruedProfit(JsonObject player_obj) {
        return player_obj.get("offline_accrued_profit").getAsInt();
    }



    public int getPlayerBankTop(MineBank plugin, Player player) throws IOException {
        // Llamamos a getTopPlayerBanks para obtener el top de los bancos
        List<List<String>> topBanks = getTopPlayerBanks(plugin, Integer.MAX_VALUE);

        // Recorremos el topBanks para encontrar la posición del jugador
        for (int i = 0; i < topBanks.size(); i++) {
            List<String> bankInfo = topBanks.get(i);
            String playerName = bankInfo.get(0); // Nombre del jugador

            if (playerName.equals(player.getName())) {
                return i + 1; // Posición en el top (empezamos desde 1, no 0)
            }
        }

        // Si el jugador no está en el top, devolver -1
        return -1;
    }

    // ------------------------- BANKS.JSON ------------------------- //

    public List<String> getBankNames(MineBank plugin) throws IOException {
        // Obtener los datos de los bancos
        JsonObject banksData = getBankData(plugin);

        // Crear una lista para almacenar los nombres de los bancos
        List<String> bankNames = new ArrayList<>();

        // Iterar sobre las claves del objeto JSON
        for (Map.Entry<String, JsonElement> entry : banksData.entrySet()) {
            bankNames.add(entry.getKey());
        }

        return bankNames;
    }

    // ------------------------- INTERESTS_DATA.JSON ------------------------- //

    public int getAccruedInterestData(MineBank plugin) throws IOException {

        // Leer el JSON actual
        Reader reader = new FileReader(plugin.getFileManager().getInterestsDataFile());
        JsonObject data = new JsonParser().parse(reader).getAsJsonObject();

        // Verificar si existe el campo "accrued_interest"
        if (data.has("accrued_interest")) {
            // En lugar de tratarlo como JsonObject, lo obtenemos directamente como JsonPrimitive
            return data.get("accrued_interest").getAsInt();
        } else {
            // Si no existe, creamos el campo "accrued_interest" con valor 0 y lo devolvemos
            data.addProperty("accrued_interest", 0);

            // Guardar el archivo con los cambios
            try (Writer writer = new FileWriter(plugin.getFileManager().getInterestsDataFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            }

            return 0; // Devuelve 0 si el campo no existe y se crea
        }
    }

    // ------------------------- ECONOMY ------------------------- //

    public int getPlayerBalance(Player player, Economy economy) {
        return (int) economy.getBalance(player);
    }

    // ------------------------- CONFIG ------------------------- //

    // Config bank
    public boolean getBankAllowed(MineBank plugin) {
        return plugin.getConfig().getBoolean("config.bank-allowed", true);
    }

    public boolean getUpdateCheckerAllowed(MineBank plugin) {
        return plugin.getConfig().getBoolean("config.update-checker", true);
    }


    // Placeholders bank
    public String getChatPrefix(MineBank plugin) {
        return plugin.getConfig().getString("config.chat-prefix", "&4&l[&6&lMine&a&lBank&4&l]&f");
    }

    public String getBankDataType(MineBank plugin) {
        return plugin.getConfig().getString("bank.data.type", "JSON");
    }

    public String getMoneySymbol(MineBank plugin) {
        return plugin.getConfig().getString("bank.money.symbol", "$");
    }

    /*public String getMoneySymbolPosition(MineBank plugin) {
        //    # The position of the symbol (AFTER, BEFORE, NONE)
        //    position: BEFORE
        return plugin.getConfig().getString("bank.money.position", "BEFORE");
    }*/

    /*public String balanceWithSymbol(MineBank plugin, int balance) {
        String position = getMoneySymbolPosition(plugin);
        String symbol = getMoneySymbol(plugin);

        if (position.equals("BEFORE")) return symbol + balance;
        else if (position.equals("AFTER")) return balance + symbol;
        else return String.valueOf(balance);
    }*/

    public int getProfitIntervalInSeconds(MineBank plugin) {
        return plugin.getConfig().getInt("bank.profit.interval-in-seconds", -1);
    }

    public int getProfitMinBankBalanceToReceive(MineBank plugin) {
        return plugin.getConfig().getInt("bank.profit.min-bank-balance-to-receive", -1);
    }

    public double getProfitKeepInBankPercentage(MineBank plugin) {
        return plugin.getConfig().getDouble("bank.profit.keep-in-bank-percentage", 0);
    }

    public boolean getProfitMultiplyByBankLevel(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.profit.multiply-by-bank-level", false);
    }

    public boolean getProfitNotEnoughBalanceToReveiveMessage(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.profit.not-enough-balance-to-receive-message", true);
    }

    public int getTimesProfitsOffline(MineBank plugin) {
        return plugin.getConfig().getInt("bank.profit.times-profits-offline", -1);
    }


    // Start bank
    public String getBankStartBankName(MineBank plugin) {
        return plugin.getConfig().getString("bank.start.bank-name", "User");
    }

    public int getBankStartLevel(MineBank plugin) {
        return plugin.getConfig().getInt("bank.start.level", 1);
    }

    public int getBankStartBalance(MineBank plugin) {
        return plugin.getConfig().getInt("bank.start.balance", 0);
    }


    // Admin bank
    public boolean getBankAdminShouldHaveLastBank(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.admin-should-have-last-bank", false);
    }

    public boolean getBankSetTabCompleterOfflinePlayers(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.set-offline", true);

    }


    // Data bank
    public int getInterestMinBankBalanceToApply(MineBank plugin) {
        return plugin.getConfig().getInt("bank.interest.min-bank-balance-to-apply", -1);
    }

    public double getInterestWithdrawPercentage(MineBank plugin) {
        return plugin.getConfig().getDouble("bank.interest.withdraw-percentage", 0);
    }

    public boolean getInterestMultiplyByBankLevel(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.interest.multiply-by-bank-level", false);
    }


    // Plugin GUI
    public int getUpdateGUITicks(MineBank plugin) {
        return plugin.getConfig().getInt("gui.update-time", 40);
    }


    // Plugin exceptions
    public static boolean getSaveException(MineBank plugin) {
        return plugin.getConfig().getBoolean("exception.save", true);
    }

    public static boolean getDeleteOnStartExceptions(MineBank plugin) {
        return plugin.getConfig().getBoolean("exception.delete-on-start", false);
    }

    // ------------------------- SERVER ------------------------- //

    public boolean isPlayerOnline(String playerName) {
        for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers())
            if (onlinePlayer.getName().equalsIgnoreCase(playerName)) return true;
        return false;
    }

    // ------------------------- -- ------------------------- //
}
