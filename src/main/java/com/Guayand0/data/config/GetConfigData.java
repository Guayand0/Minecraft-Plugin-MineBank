package com.Guayand0.data.config;

import com.Guayand0.MineBank;

public class GetConfigData {

    // Main bank config
    public boolean getBankAllowed(MineBank plugin) {
        return plugin.getConfig().getBoolean("config.bank-allowed", true);
    }

    public boolean getUpdateCheckerAllowed(MineBank plugin) {
        return plugin.getConfig().getBoolean("config.update-checker", true);
    }


    // Placeholders bank config
    public String getChatPrefix(MineBank plugin) {
        return plugin.getConfig().getString("config.chat-prefix", "&4&l[&6&lMine&a&lBank&4&l]&f");
    }

    public String getBankDataType(MineBank plugin) {
        return plugin.getConfig().getString("bank.data.type", "JSON");
    }

    public String getMoneySymbol(MineBank plugin) {
        return plugin.getConfig().getString("bank.money.symbol", "$");
    }


    // Profit bank config
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

    // Start bank config
    public String getBankStartBankName(MineBank plugin) {
        return plugin.getConfig().getString("bank.start.bank-name", "User");
    }

    public int getBankStartLevel(MineBank plugin) {
        return plugin.getConfig().getInt("bank.start.level", 1);
    }

    public int getBankStartBalance(MineBank plugin) {
        return plugin.getConfig().getInt("bank.start.balance", 0);
    }


    // Admin bank config
    public boolean getBankAdminShouldHaveLastBank(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.admin-should-have-last-bank", false);
    }

    public boolean getBankSetTabCompleterOfflinePlayers(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.set-offline", true);

    }


    // Interests bank config
    public int getInterestMinBankBalanceToApply(MineBank plugin) {
        return plugin.getConfig().getInt("bank.interest.min-bank-balance-to-apply", -1);
    }

    public double getInterestWithdrawPercentage(MineBank plugin) {
        return plugin.getConfig().getDouble("bank.interest.withdraw-percentage", 0);
    }

    public boolean getInterestMultiplyByBankLevel(MineBank plugin) {
        return plugin.getConfig().getBoolean("bank.interest.multiply-by-bank-level", false);
    }


    // Plugin GUI config
    public int getUpdateGUITicks(MineBank plugin) {
        return plugin.getConfig().getInt("gui.update-time", 40);
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
}
