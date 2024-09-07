package mb.Guayando.config;

import mb.Guayando.MineBank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BankManager {

    private final MineBank plugin;
    private File bankFile;
    private FileConfiguration bankConfig;

    public BankManager(MineBank plugin) {
        this.plugin = plugin;
        initializeBankFile();
    }

    // Inicializa el archivo de banco, copia el archivo por defecto si no existe
    private void initializeBankFile() {
        bankFile = new File(plugin.getDataFolder(), "bank.yml");
        if (!bankFile.exists()) {
            // Copia el archivo por defecto desde los recursos del plugin
            plugin.saveResource("bank.yml", false);
        }
        reloadBank(); // Carga la configuración
    }

    // Devuelve la configuración actual del banco
    public FileConfiguration getBank() {
        return bankConfig;
    }

    // Guarda la configuración actual en el archivo
    public void saveBank() {
        if (bankConfig == null) {
            return;
        }
        try {
            bankConfig.save(bankFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recarga la configuración del banco desde el archivo
    public void reloadBank() {
        if (bankFile == null) {
            return;
        }
        // Carga el archivo de configuración
        bankConfig = YamlConfiguration.loadConfiguration(bankFile);

        // Cargar cualquier configuración predeterminada si existe
        InputStream defConfigStream = plugin.getResource("bank.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            bankConfig.setDefaults(defConfig);
        }
    }
}
