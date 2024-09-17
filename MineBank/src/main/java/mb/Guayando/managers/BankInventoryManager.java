package mb.Guayando.managers;

import mb.Guayando.MineBank;
import mb.Guayando.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BankInventoryManager {

    private final MineBank plugin;
    private FileConfiguration bankInventoryConfig;

    public BankInventoryManager(MineBank plugin) {
        this.plugin = plugin;
        loadBankInventoryFiles();
    }

    private void loadBankInventoryFiles() {
        File bankInventoryDir = new File(plugin.getDataFolder(), "bankInventory");
        if (!bankInventoryDir.exists()) {
            bankInventoryDir.mkdirs(); // Crear el directorio si no existe
        }

        File jarFile;
        try {
            jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("bankInventory/") && !entry.isDirectory()) {
                    File outFile = new File(bankInventoryDir, entry.getName().substring("bankInventory/".length()));

                    if (!outFile.exists()) {
                        outFile.getParentFile().mkdirs();
                        try (InputStream in = jar.getInputStream(entry);
                             FileOutputStream out = new FileOutputStream(outFile);
                             BufferedInputStream bufferedIn = new BufferedInputStream(in)) {

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = bufferedIn.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        reloadConfig(); // Llamar a reloadConfig después de cargar los archivos
    }

    public void reloadInventory() {
        loadBankInventoryFiles();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        String selectedLanguage = plugin.getConfig().getString("config.inventory-language", "en");
        String inventoryFileName = selectedLanguage + ".yml";

        File invDir = new File(plugin.getDataFolder(), "bankInventory");
        File selectedInvFile = new File(invDir, inventoryFileName);

        if (!selectedInvFile.exists()) {
            plugin.getLogger().warning(MessageUtils.getColoredMessage(MineBank.prefix + " &4Inventory file " + inventoryFileName + " does not exist in bankInventory folder."));
            plugin.getLogger().warning(MessageUtils.getColoredMessage(MineBank.prefix + " &4Using en.yml by default."));
            selectedInvFile = new File(invDir, "en.yml");
        }

        try {
            bankInventoryConfig = YamlConfiguration.loadConfiguration(selectedInvFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading bank inventory config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public FileConfiguration getCustomConfig(String filePath) {
        File configFile = new File(plugin.getDataFolder(), filePath);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(filePath, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getBankInventoryConfig() {
        return bankInventoryConfig;
    }

    public String getBankInventoryFile() {
        String selectedLanguage = plugin.getConfig().getString("config.inventory-language", "en");
        return selectedLanguage + ".yml";
    }
}
