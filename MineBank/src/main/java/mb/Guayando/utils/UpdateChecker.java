package mb.Guayando.utils;

import mb.Guayando.config.LanguageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import mb.Guayando.MineBank;

public class UpdateChecker implements Listener{
    private final MineBank plugin;
    public UpdateChecker(MineBank plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void CheckUpdate(PlayerJoinEvent event) {
        try{
            Player player = event.getPlayer();
            boolean updateChecker = plugin.getConfig().getBoolean("config.update-checker");
            boolean isOutdatedVersion = !(plugin.getVersion().equals(plugin.getLatestVersion()));
            boolean updateCheckerWork = plugin.getUpdateCheckerWork();

            // Verificar si el mensaje de actualización debe enviarse
            if (updateChecker && isOutdatedVersion) {
                if (player.isOp() || player.hasPermission("minebank.updatechecker") || player.hasPermission("minebank.admin")) {
                    LanguageManager languageManager = plugin.getLanguageManager();
                    languageManager.reloadLanguage(); // Recargar el archivo de idioma
                    if(!updateCheckerWork){
                        plugin.comprobarActualizaciones();
                    }
                    String message = languageManager.getMessage("config.update-checker");
                    if (message != null) {
                        message = message.replaceAll("%plugin%", MineBank.prefix).replaceAll("%version%", plugin.getVersion()).replaceAll("%latestversion%", plugin.getLatestVersion()).replaceAll("%link%", "https://www.spigotmc.org/resources/119147/");
                        player.sendMessage(MessageUtils.getColoredMessage(me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message)));// Procesar placeholders de PlaceholderAPI
                    }
                }
            }
        }catch (NullPointerException e){
            Bukkit.getConsoleSender().sendMessage(e.getMessage());
            e.printStackTrace();
        }
    }
}