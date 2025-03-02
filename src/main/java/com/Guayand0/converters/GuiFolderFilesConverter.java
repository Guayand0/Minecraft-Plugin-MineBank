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
import java.util.List;
import java.util.stream.Collectors;

public class GuiFolderFilesConverter {

    private final MineBank plugin;

    private final MessageUtils MU = new MessageUtils();

    public GuiFolderFilesConverter(MineBank plugin) {
        this.plugin = plugin;
    }

    public void convertGuiFolder() {
        try {
            File bankInventoryFolder = new File(plugin.getDataFolder(), "bankInventory");
            File backupFolder = new File(plugin.getDataFolder(), "old-bankInventory");

            // Verificar si la carpeta original existe
            if (!bankInventoryFolder.exists()) return;

            // Crear copia de seguridad
            try {
                if (backupFolder.exists()) deleteFolder(backupFolder);
                Files.move(bankInventoryFolder.toPath(), backupFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eBackup of the bankInventory folder to old-bankInventory."));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &eThe new folder that replaces it is called gui."));

            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cError creating the backup of bankInventory: " + e.getMessage()));
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cAn error occurred while processing the player data migration."));
        }
    }

    /*private void convertYamlFile(File oldFile, File newFile) throws IOException {
        List<String> lines = Files.readAllLines(oldFile.toPath());
        List<String> modifiedLines = lines.stream()
                .map(line -> line.startsWith("bank-inventory:") ? "gui:" : line)
                .collect(Collectors.toList());
        Files.write(newFile.toPath(), modifiedLines);
    }*/

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) for (File file : folder.listFiles()) deleteFolder(file);
        if (folder.delete()) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &ePrevious bankInventory folder successfully deleted after conversion."));
        else Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " &cCould not delete bankInventory folder, delete it manually."));
    }
}
