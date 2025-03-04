package com.Guayand0.Data;

public class BankData {
    private final String bankName;
    private final int bankLevel;
    private final int bankBalance;
    private final int offlineProfitAccrued;
    private final int offlineProfitTimes;

    private final int bankMaxBalance;
    private final int bankMaxLevel;
    private final int bankLevelUpgradeCost;

    public BankData(String bankName, int bankLevel, int bankBalance, int offlineProfitAccrued, int offlineProfitTimes, int bankMaxBalance, int bankMaxLevel, int bankLevelUpgradeCost) {
        this.bankName = bankName;
        this.bankLevel = bankLevel;
        this.bankBalance = bankBalance;
        this.offlineProfitAccrued = offlineProfitAccrued;
        this.offlineProfitTimes = offlineProfitTimes;
        this.bankMaxBalance = bankMaxBalance;
        this.bankMaxLevel = bankMaxLevel;
        this.bankLevelUpgradeCost = bankLevelUpgradeCost;
    }

    public String getBankName() { return bankName; }
    public int getBankLevel() { return bankLevel; }
    public int getBankBalance() { return bankBalance; }
    public int getOfflineProfitAccrued() { return offlineProfitAccrued; }
    public int getOfflineProfitTimes() { return offlineProfitTimes; }
    public int getBankMaxBalance() { return bankMaxBalance; }
    public int getBankMaxLevel() { return bankMaxLevel; }
    public int getBankLevelUpgradeCost() { return bankLevelUpgradeCost; }
}

/*
// Obtener datos del banco del jugador y maximos de nivel y balance
BankData bankData = BM.getBankData(plugin, playerName);
if (bankData != null) {
    bankName = bankData.getBankName();
    bankLevel = bankData.getBankLevel();
    bankBalance = bankData.getBankBalance();
    offlineProfitAccrued = bankData.getOfflineProfitAccrued();
    offlineProfitTimes = bankData.getOfflineProfitTimes();
    bankMaxLevel = bankData.getBankMaxLevel();
    bankMaxBalance = bankData.getBankMaxBalance();
    bankLevelUpgradeCost = bankData.getBankLevelUpgradeCost();
}
*/