package com.Guayand0.zlib;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerUtils {

    /**
     * Checks if a player is currently online.
     *
     * @param playerName The name of the player to check.
     * @return If the player is online or not.
     */
    public boolean isPlayerOnline(String playerName) {
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getServer().getOnlinePlayers())
            if (onlinePlayer.getName().equalsIgnoreCase(playerName)) return true;
        return false;
    }

    /**
     * Gets a player's UUID from their name (works for offline players too).
     *
     * @param playerName The player's name.
     * @return UUID of the player, or null if not found.
     */
    public UUID getUUIDFromName(String playerName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);

        if (offline == null) return null;
        return offline.getUniqueId();
    }

    /**
     * Gets a player's name from their UUID (works for offline players too).
     *
     * @param uuid The player's UUID.
     * @return The player's name, or null if never played before.
     */
    public String getNameFromUUID(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

        if (offline == null) return null;
        return offline.getName(); // null si nunca entró al servidor
    }

    /**
     * Gets an OfflinePlayer instance from a name and UUID.
     * Ensures the returned object contains both name and UUID when possible.
     *
     * @param name The player's name.
     * @param uuid The player's UUID.
     * @return OfflinePlayer containing name and UUID (even if the player is offline).
     */
    public OfflinePlayer getOfflinePlayer(String name, UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

        if (offline.getName() == null && name != null) {
            offline = Bukkit.getOfflinePlayer(name);
        }

        return offline;
    }

    /**
     * Gets the player's current balance from the Vault economy.
     *
     * @param player The player whose balance is to be retrieved.
     * @param economy The Vault economy instance.
     * @return The player balance.
     */
    public int getPlayerBalance(Player player, Economy economy) {
        return (int) economy.getBalance(player);
    }
}
