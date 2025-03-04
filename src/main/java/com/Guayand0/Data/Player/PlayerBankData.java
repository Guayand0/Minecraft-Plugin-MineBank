package com.Guayand0.Data.Player;

public class PlayerBankData {

    private String name;
    private int level;
    private int balance;
    private int offlineAccruedProfit;
    private int offlineProfitTimes;

    public PlayerBankData(String name, int level, int balance, int offlineAccruedProfit, int offlineProfitTimes) {
        this.name = name;
        this.level = level;
        this.balance = balance;
        this.offlineAccruedProfit = offlineAccruedProfit;
        this.offlineProfitTimes = offlineProfitTimes;
    }

    public PlayerBankData() {}

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getOfflineAccruedProfit() {
        return offlineAccruedProfit;
    }

    public void setOfflineAccruedProfit(int offlineAccruedProfit) {
        this.offlineAccruedProfit = offlineAccruedProfit;
    }

    public int getOfflineProfitTimes() {
        return offlineProfitTimes;
    }

    public void setOfflineProfitTimes(int offlineProfitTimes) {
        this.offlineProfitTimes = offlineProfitTimes;
    }
}
