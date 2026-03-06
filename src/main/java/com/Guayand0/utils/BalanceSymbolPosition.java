package com.Guayand0.utils;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.GetValues;

public class BalanceSymbolPosition {

    GetValues GV = new GetValues();

    public String format(MineBank plugin, String value) {

        String symbol = GV.getString(plugin, "bank.money.symbol", "$");
        String position = GV.getString(plugin, "bank.money.symbol-position", "NONE");

        switch (position.toUpperCase()) {
            case "AFTER":
                return value + symbol;

            case "BEFORE":
                return symbol + value;

            case "NONE":
                return value;

            default:
                return value; // sin concatenar
        }
    }
}
