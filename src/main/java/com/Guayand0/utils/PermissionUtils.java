//package com.Guayand0.utils;
//
//import com.Guayand0.MineBank;
//import org.bukkit.entity.Player;
//import org.bukkit.permissions.PermissionAttachment;
//
//import java.util.HashMap;
//import java.util.UUID;
//
//public class PermissionUtils {
//    private final MineBank plugin;
//    private final HashMap<UUID, PermissionAttachment> playerPermissions = new HashMap<>();
//
//    public PermissionUtils(MineBank plugin) {
//        this.plugin = plugin;
//    }
//
//    public void givePermission(Player player, String permission) {
//        PermissionAttachment attachment = playerPermissions.computeIfAbsent(player.getUniqueId(), uuid -> player.addAttachment(plugin));
//        attachment.setPermission(permission, true);
//    }
//
//    // Si el jugador tiene permiso "minebank.bank.<banco>" y el banco no existe en banks.json
//    public void removePermission(Player player, String permission) {
//        if (playerPermissions.containsKey(player.getUniqueId())) {
//            playerPermissions.get(player.getUniqueId()).unsetPermission(permission);
//        }
//    }
//}
