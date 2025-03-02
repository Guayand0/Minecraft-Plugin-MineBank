package com.Guayand0.converters;

import com.Guayand0.MineBank;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MessagesFolderFilesConverter {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();

    public MessagesFolderFilesConverter(MineBank plugin) {
        this.plugin = plugin;
    }

    public void convertMessagesFolder() {
        try {
            File langFolder = new File(plugin.getDataFolder(), "lang");
            File backupFolder = new File(plugin.getDataFolder(), "old-lang");

            // Verificar si la carpeta original existe
            if (!langFolder.exists()) return;

            // Crear copia de seguridad
            try {
                if (backupFolder.exists()) deleteFolder(backupFolder);
                Files.move(langFolder.toPath(), backupFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eBackup of the lang folder to old-lang."));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eThe new folder that replaces it is called messages."));
                
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cError creating the backup of lang: " + e.getMessage()));
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cAn error occurred while processing the player data migration"));
        }
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) for (File file : folder.listFiles()) deleteFolder(file);
        if (folder.delete()) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &ePrevious lang folder successfully deleted after conversion."));
        else Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCould not delete lang folder, delete it manually."));
    }
}
