package com.Guayand0.data.bank;

import java.util.List;

public class MYSQLBankLevelData {

    private int max_balance;
    private int upgrade_cost;

    private int bank_top_position;

    private List<String> bank_names;

    public int getMaxBalance() { return max_balance; }
    public int getUpgradeCost() { return upgrade_cost; }

    public int getBankTopPosition() { return bank_top_position; }

    public List<String> getBankNames() { return bank_names; }
}
