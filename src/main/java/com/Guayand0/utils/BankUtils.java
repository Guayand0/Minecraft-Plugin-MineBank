package com.Guayand0.utils;

import com.Guayand0.MineBank;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BankUtils {

    // ------------------------- ECONOMY ------------------------- //

    public int getPlayerBalance(Player player, Economy economy) {
        return (int) economy.getBalance(player);
    }

    // ------------------------- CONFIG SAVE EXCEPTIONS ------------------------- //

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
