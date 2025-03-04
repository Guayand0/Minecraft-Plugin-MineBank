package com.Guayand0.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Utility class for handling messages with color and PlaceholderAPI placeholders.
 */
public class MessageUtils {

    /**
     * Translates color codes in the provided message using '&' as the color code symbol.
     *
     * @param message The message to be colored.
     * @return The colored message.
     */
    public String getColoredText(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Sets PlaceholderAPI placeholders for the player and translates color codes in the provided message.
     *
     * @param player The player to set placeholders for.
     * @param message The message containing placeholders and color codes.
     * @return The message with placeholders replaced and color codes applied.
     */
    public String getPAPIAndColoredText(Player player, String message) {
        return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message));
    }

    /**
     * Sets PlaceholderAPI placeholders for the player.
     *
     * @param player The player to set placeholders for.
     * @param message The message containing placeholders and color codes.
     * @return The message with placeholders replaced.
     */
    public String getPAPIText(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    /**
     * Replaces placeholders in the given message with their corresponding values.
     *
     * @param message The original message containing placeholders.
     * @param placeholders A map where keys are placeholders and values are their replacements.
     * @return The message with all placeholders replaced by their corresponding values.
     */
    public String replacePlaceholdersText(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getValue() == null) {
                Bukkit.getConsoleSender().sendMessage(getColoredText("&cWARNING: Placeholder with key " + entry.getKey() + " is null"));
            }
            String replacement = entry.getValue() != null ? entry.getValue() : "";

            // Usamos replaceAll con (?i) para ignorar mayúsculas y minúsculas
            message = message.replaceAll("(?i)" + entry.getKey(), replacement);
        }
        return message;
    }

    /**
     * Replaces placeholders in the given message and applies color formatting.
     *
     * @param message The original message containing placeholders.
     * @param placeholders A map where keys are placeholders and values are their replacements.
     * @return The formatted and colorized message with placeholders replaced.
     */
    public String getColoredReplacePluginPlaceholdersText(String message, Map<String, String> placeholders) {
        return getColoredText(replacePlaceholdersText(message, placeholders));
    }

    /**
     * Checks if PlaceholderAPI is enabled and returns the message with placeholders replaced and colored text applied.
     *
     * @param isPAPIEnabled A boolean indicating whether PlaceholderAPI is enabled.
     * @param player The player to set placeholders for.
     * @param message The message containing placeholders and color codes.
     * @return The message with placeholders replaced and color codes applied if PAPI is enabled, or just the colored message otherwise.
     */
    public String getCheckPAPIText(boolean isPAPIEnabled, Player player, String message) {
        if (isPAPIEnabled) {
            return getPAPIAndColoredText(player, message);
        } else {
            return getColoredText(message);
        }
    }

    /**
     * Checks if PlaceholderAPI is enabled and returns the message with PlaceholderAPI placeholders and Plugin placeholders replaced and colored text applied.
     *
     * @param isPAPIEnabled A boolean indicating whether PlaceholderAPI is enabled.
     * @param player The player to set placeholders for.
     * @param message The message containing placeholders and color codes.
     * @param placeholders A map where keys are placeholders and values are their replacements.
     * @return The message with placeholders replaced and color codes applied
     */
    public String getCheckAllPlaceholdersText(boolean isPAPIEnabled, Player player, String message, Map<String, String> placeholders) {
        if (isPAPIEnabled) {
            return getPAPIAndColoredText(player, replacePlaceholdersText(message, placeholders));
        } else {
            return getColoredText(replacePlaceholdersText(message, placeholders));
        }
    }
}
