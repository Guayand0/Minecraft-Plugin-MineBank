package com.Guayand0.zlib;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)#([0-9A-F]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<#([0-9A-F]{6}):#([0-9A-F]{6})>(.*?)</#>", Pattern.DOTALL);
    private static final Pattern GRADIENT_3_PATTERN = Pattern.compile("(?i)<#([0-9A-F]{6}):#([0-9A-F]{6}):#([0-9A-F]{6})>(.*?)</#>", Pattern.DOTALL);
    private static final boolean HEX_SUPPORTED = isHexSupported();

    private static boolean isHexSupported() {
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Translates color codes in the provided message using '&' as the color code symbol.
     *
     * @param message The message to be colored.
     * @return The colored message.
     */
    public String getColoredText(String message) {
        if (message == null) return null;
        String text = translateTripleGradients(message);
        text = translateGradients(text);
        return ChatColor.translateAlternateColorCodes('&', translateHexColors(text));
    }

    /**
     * Sets PlaceholderAPI placeholders for the player and translates color codes in the provided message.
     *
     * @param player The player to set placeholders for.
     * @param message The message containing placeholders and color codes.
     * @return The message with placeholders replaced and color codes applied.
     */
    public String getPAPIAndColoredText(Player player, String message) {
        if (message == null) return null;
        String text = PlaceholderAPI.setPlaceholders(player, message);
        text = translateTripleGradients(text);
        text = translateGradients(text);
        return ChatColor.translateAlternateColorCodes('&', translateHexColors(text));
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
        if (message == null || placeholders == null) return message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                Bukkit.getConsoleSender().sendMessage(getColoredText("&cWARNING: Placeholder with key " + key + " is null"));
                value = "";
            }

            // Escapar el placeholder (por si tiene % u otros símbolos)
            Pattern pattern = Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);

            // Escapar el valor (por si tiene $, \, etc)
            message = matcher.replaceAll(Matcher.quoteReplacement(value));
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
        String text = replacePlaceholdersText(message, placeholders);
        if (isPAPIEnabled) {
            return getPAPIAndColoredText(player, text);
        } else {
            return getColoredText(text);
        }
    }

    public String replaceColorCodeMOTD(String message) {
        if (message == null) return null;
        return translateHexColors(translateGradients(message)).replaceAll("&", "�");
    }

    private String translateHexColors(String message) {
        if (!HEX_SUPPORTED || message == null) return message;

        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hex).toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String translateGradients(String message) {
        if (!HEX_SUPPORTED || message == null) return message;

        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);

            String replacement = applyGradient(content, startHex, endHex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String translateTripleGradients(String message) {
        if (!HEX_SUPPORTED || message == null) return message;

        Matcher matcher = GRADIENT_3_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String midHex = matcher.group(2);
            String endHex = matcher.group(3);
            String content = matcher.group(4);

            String replacement = applyTripleGradient(content, startHex, midHex, endHex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String applyGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return "";

        int[] start = hexToRgb(startHex);
        int[] end = hexToRgb(endHex);

        int length = text.length();
        if (length == 1) {
            return net.md_5.bungee.api.ChatColor.of("#" + startHex) + text;
        }

        StringBuilder out = new StringBuilder(length * 14);
        for (int i = 0; i < length; i++) {
            double t = (double) i / (double) (length - 1);
            int r = (int) Math.round(start[0] + (end[0] - start[0]) * t);
            int g = (int) Math.round(start[1] + (end[1] - start[1]) * t);
            int b = (int) Math.round(start[2] + (end[2] - start[2]) * t);
            String hex = String.format("%02X%02X%02X", r, g, b);
            out.append(net.md_5.bungee.api.ChatColor.of("#" + hex)).append(text.charAt(i));
        }

        return out.toString();
    }

    private String applyTripleGradient(String text, String startHex, String midHex, String endHex) {
        if (text == null || text.isEmpty()) return "";

        int length = text.length();
        if (length == 1) {
            return net.md_5.bungee.api.ChatColor.of("#" + startHex) + text;
        }

        int split = length / 2;

        String firstHalf = text.substring(0, split);
        String secondHalf = text.substring(split);

        StringBuilder out = new StringBuilder(length * 14);

        // Primera mitad: start -> mid
        out.append(applyGradient(firstHalf, startHex, midHex));

        // Segunda mitad: mid -> end
        out.append(applyGradient(secondHalf, midHex, endHex));

        return out.toString();
    }

    private int[] hexToRgb(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }
}

