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

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiHeadTexture {

    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(http[^\"]+)\"");

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

        if (supportsModernProfiles()) {
            applyModernTexture(item, texture);
        } else {
            applyLegacyTexture(item, texture);
        }
    }

    private String getTexture(ConfigurationSection section) {
        String texture = section.getString("texture");
        return (texture == null || texture.isEmpty()) ? null : texture;
    }

    private boolean supportsModernProfiles() {
        try {
            SkullMeta.class.getMethod("setOwnerProfile", PlayerProfile.class);
            Bukkit.class.getMethod("createPlayerProfile", UUID.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
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

        textures.setSkin(new URL(resolveTextureUrl(url)));
        profile.setTextures(textures);

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

        try {
            profile.getProperties().put("textures", new Property("textures", texture));

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

    private String resolveTextureUrl(String texture) {
        if (texture == null) {
            throw new IllegalArgumentException("Texture can not be null");
        }

        String trimmedTexture = texture.trim();
        if (trimmedTexture.startsWith("http://") || trimmedTexture.startsWith("https://")) {
            return trimmedTexture;
        }

        try {
            String decodedTexture = new String(Base64.getDecoder().decode(trimmedTexture), StandardCharsets.UTF_8);
            Matcher matcher = TEXTURE_URL_PATTERN.matcher(decodedTexture);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IllegalArgumentException ignored) {
            // Not a Base64 texture value, let the caller handle the invalid URL.
        }

        throw new IllegalArgumentException("Unsupported texture format: " + trimmedTexture);
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
