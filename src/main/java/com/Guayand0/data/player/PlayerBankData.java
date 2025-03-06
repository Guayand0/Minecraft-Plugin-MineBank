package com.Guayand0.data.player;

public class PlayerBankData {

    private String bankName;
    private int level;
    private int balance;
    private int offlineAccruedProfit;
    private int offlineProfitTimes;

    public PlayerBankData(String name, int level, int balance,
                          int offlineAccruedProfit, int offlineProfitTimes) {
        this.bankName = name;
        this.level = level;
        this.balance = balance;
        this.offlineAccruedProfit = offlineAccruedProfit;
        this.offlineProfitTimes = offlineProfitTimes;
    }

    // Getters y Setters
    public String getName() { return bankName; }
    //public void setName(String name) { this.bankName = name; }
    public int getLevel() { return level; }
    //public void setLevel(int level) { this.level = level; }
    public int getBalance() { return balance; }
    //public void setBalance(int balance) { this.balance = balance; }
    public int getOfflineAccruedProfit() { return offlineAccruedProfit; }
    //public void setOfflineAccruedProfit(int offlineAccruedProfit) { this.offlineAccruedProfit = offlineAccruedProfit; }
    public int getOfflineProfitTimes() { return offlineProfitTimes; }
    //public void setOfflineProfitTimes(int offlineProfitTimes) { this.offlineProfitTimes = offlineProfitTimes; }
}
