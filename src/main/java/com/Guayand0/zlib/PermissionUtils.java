package com.Guayand0.zlib;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

public class PermissionUtils {

    /**
     * Gives a permission to a player.
     *
     * @param plugin     plugin instance
     * @param player     target player
     * @param permission permission to grant
     */
    public void givePermission(Plugin plugin, Player player, String permission) {
        HashMap<UUID, PermissionAttachment> playerPermissions = new HashMap<>();
        PermissionAttachment attachment = playerPermissions.computeIfAbsent(player.getUniqueId(), uuid -> player.addAttachment(plugin));
        attachment.setPermission(permission, true);
    }

    /**
     * Removes a permission from a player.
     *
     * @param player     target player
     * @param permission permission to remove
     */
    public void removePermission(Plugin plugin, Player player, String permission) {
        HashMap<UUID, PermissionAttachment> playerPermissions = new HashMap<>();
        PermissionAttachment attachment = playerPermissions.computeIfAbsent(player.getUniqueId(), uuid -> player.addAttachment(plugin));
        attachment.setPermission(permission, false);
    }

    /**
     * Checks if a player has a permission.
     *
     * @param player     player to check
     * @param permission permission to verify
     * @return true if the player has the permission
     */
    public boolean checkPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
}