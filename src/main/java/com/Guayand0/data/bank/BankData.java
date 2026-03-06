package com.Guayand0.data.bank;

import java.util.Map;

public class BankData {

    private String bankName;
    private Map<String, Level> levels;

    public BankData() {}

    public BankData(String bankName, Map<String, Level> levels) {
        this.bankName = bankName;
        this.levels = levels;
    }

    // getters y setters
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Map<String, Level> getLevels() { return levels; }
    public void setLevels(Map<String, Level> levels) { this.levels = levels; }

    // Clase interna Level
    public static class Level {
        private int max_balance;
        private int upgrade_cost;

        public Level() {}
        public Level(int max_balance, int upgrade_cost) {
            this.max_balance = max_balance;
            this.upgrade_cost = upgrade_cost;
        }

        public int getMax_balance() { return max_balance; }
        public void setMax_balance(int max_balance) { this.max_balance = max_balance; }

        public int getUpgrade_cost() { return upgrade_cost; }
        public void setUpgrade_cost(int upgrade_cost) { this.upgrade_cost = upgrade_cost; }
    }
}
