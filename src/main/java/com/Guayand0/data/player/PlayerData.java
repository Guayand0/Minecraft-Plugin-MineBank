package com.Guayand0.data.player;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.GetValues;

public class PlayerData {

    private Bank bank;

    public PlayerData() {}

    public PlayerData(Bank bank) {
        this.bank = bank;
    }

    public Bank getBank() { return bank; }
    public void setBank(Bank bank) { this.bank = bank; }

    // Datos por defecto
    public static PlayerData defaultData(MineBank plugin) {
        GetValues GV = new GetValues();
        Offline offline = new Offline(0, 0);
        Bank bank = new Bank(GV.getString(plugin, "bank.start.bank-name", "User"), GV.getInt(plugin, "bank.start.level", 1), GV.getInt(plugin, "bank.start.balance", 0), offline);
        return new PlayerData(bank);
    }

    // Clase interna Bank
    public static class Bank {
        private String name;
        private int level;
        private int balance;
        private Offline offline;

        public Bank() {}
        public Bank(String name, int level, int balance, Offline offline) {
            this.name = name;
            this.level = level;
            this.balance = balance;
            this.offline = offline;
        }

        // getters y setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public int getBalance() { return balance; }
        public void setBalance(int balance) { this.balance = balance; }

        public Offline getOffline() { return offline; }
        public void setOffline(Offline offline) { this.offline = offline; }
    }

    // Clase interna Offline
    public static class Offline {
        private int accrued_profit;
        private int profit_times;

        public Offline() {}
        public Offline(int accrued_profit, int profit_times) {
            this.accrued_profit = accrued_profit;
            this.profit_times = profit_times;
        }

        public int getAccrued_profit() { return accrued_profit; }
        public void setAccrued_profit(int accrued_profit) { this.accrued_profit = accrued_profit; }

        public int getProfit_times() { return profit_times; }
        public void setProfit_times(int profit_times) { this.profit_times = profit_times; }
    }
}
