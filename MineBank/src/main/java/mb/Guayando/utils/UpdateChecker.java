package mb.Guayando.utils;

import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateChecker implements Listener{
    private final MineBank plugin;
    private final LanguageManager languageManager;
    public UpdateChecker(MineBank plugin){
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
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
                    languageManager.reloadLanguage(); // Recargar el archivo de idioma
                    if(!updateCheckerWork){
                        plugin.comprobarActualizaciones();
                    }
                    String messagePath = "config.update-checker";
                    MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0);
                }
            }
        }catch (NullPointerException e){
            Bukkit.getConsoleSender().sendMessage(e.getMessage());
            e.printStackTrace();
        }
    }
}