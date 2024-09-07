package mb.Guayando.config;

import mb.Guayando.MineBank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainConfigManager {

    private MineBank plugin;
    private File configFile;
    private FileConfiguration config;

    public MainConfigManager(MineBank plugin) {
        this.plugin = plugin;
        createConfig();
        loadConfig();
    }

    private void createConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false); // Copia el archivo por defecto si no existe
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        } else {
        }
    }

    public void loadConfig() {
        FileConfiguration config = getConfig();
        int keepInBankPercentage = config.getInt("bank.profit.keep-in-bank");
        long profitInterval = config.getLong("bank.profit.interval") * 20L; // Convert seconds to ticks
        double withdrawInterest = config.getDouble("bank.interests.withdraw");
        boolean bankUse = config.getBoolean("config.bank-use"); // Nueva línea añadida
    }

}