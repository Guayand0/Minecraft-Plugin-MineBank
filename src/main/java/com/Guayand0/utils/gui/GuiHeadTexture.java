package com.Guayand0.utils.gui;

import com.Guayand0.MineBank;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuiHeadTexture {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public GuiHeadTexture(MineBank plugin) {
        this.plugin = plugin;
    }

    public void addHeadTexture(Material material, ConfigurationSection itemData, ItemStack item) {

        if (material != Material.PLAYER_HEAD) return;

        String texture = getTexture(itemData);
        if (texture == null) return;

        if (isRecentVersion()) {
            applyModernTexture(item, texture);
        } else {
            applyLegacyTexture(item, texture);
        }
    }

    private String getTexture(ConfigurationSection section) {
        String texture = section.getString("texture");
        return (texture == null || texture.isEmpty()) ? null : texture;
    }

    private boolean isRecentVersion() {
        String version = Bukkit.getVersion();
        return version.contains("1.20.4") || version.contains("1.20.5") || version.contains("1.20.6") || version.contains("1.21");
    }

    /* =========================
       VERSIONES MODERNAS
       ========================= */

    private void applyModernTexture(ItemStack head, String url) {

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return;

        try {

            PlayerProfile profile = createProfile(url);

            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);

        } catch (Exception e) {
            handleError(e, "Invalid URL in PLAYER_HEAD check it in gui folder");
        }
    }

    private PlayerProfile createProfile(String url) throws Exception {

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        textures.setSkin(new URL(url));

        return profile;
    }

    /* =========================
       VERSIONES ANTIGUAS
       ========================= */

    private void applyLegacyTexture(ItemStack head, String texture) {

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return;

        try {

            GameProfile profile = createGameProfile(texture);
            applyProfileReflection(meta, profile);

            head.setItemMeta(meta);

        } catch (Exception e) {
            handleError(e, "Invalid VALUE in PLAYER_HEAD check it in gui folder");
        }
    }

    private GameProfile createGameProfile(String texture) {

        GameProfile profile = new GameProfile(UUID.randomUUID(), "");

        Set<Property> textures = new HashSet<>();
        textures.add(new Property("textures", texture));

        try {

            Field propertiesField = GameProfile.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);
            propertiesField.set(profile, textures);

        } catch (Exception e) {
            handleError(e, "Error setting GameProfile textures");
        }

        return profile;
    }

    private void applyProfileReflection(SkullMeta meta, GameProfile profile) {

        try {

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

        } catch (Exception e) {
            handleError(e, "Error applying skull profile");
        }
    }

    /* =========================
       ERROR HANDLING
       ========================= */

    private void handleError(Exception e, String message) {
        e.printStackTrace();
        if (GV.getBoolean(plugin, "exception.save", true)) {Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));}
        Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &4[ERROR] &c" + message, plugin.placeholders));
    }
}
