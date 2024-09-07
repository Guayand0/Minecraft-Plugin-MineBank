package mb.Guayando;

import mb.Guayando.config.BankInventoryManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.event.BankInventoryEvent;
import mb.Guayando.tasks.BankTask;
import mb.Guayando.utils.*;
import mb.Guayando.event.PlayerJoinEventHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import mb.Guayando.commands.ComandoPrincipal;
import mb.Guayando.commands.ComandoBank;
import mb.Guayando.config.MainConfigManager;
import mb.Guayando.config.BankManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MineBank extends JavaPlugin implements Listener {

    private Economy economy;
    public static String prefix = "&4[&6&lMineBank&4] ";
    public String version = getDescription().getVersion();
    public String latestversion;
    public boolean updateCheckerWork = true;
    private MainConfigManager configManager;
    private LanguageManager languageManager;
    private BankManager bankManager;
    private BankInventoryManager bankInventoryManager;
    private BankTask bankTask;

    @Override
    public void onEnable() {
        // Verificar si existe un plugin de economia
        if (!setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&cVault or an economy plugin not found!"));
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&cPlease ensure Vault and an economy plugin are installed."));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&9<------------------------------------>"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&fEnabled, (&aVersion: &b" + version + "&f)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&6Thanks for using my plugin :)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&eMade by &dGuayando"));
        if (setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&aVault found and linked successfully."));
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&9<------------------------------------>"));
        // Crear variables PlaceholderAPI
        /*if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIBank(this).register();
        }*/
        saveDefaultConfig();
        this.configManager = new MainConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.bankManager = new BankManager(this);
        this.bankInventoryManager = new BankInventoryManager(this);
        // Crear inventario banco
        new BankInventoryEvent(this);
        // Programar la tarea del banco
        scheduleBankTask();
        registrarComandos();
        registrarEventos();
        // Ejecutar comprobarActualizaciones() después de que el servidor haya iniciado completamente
        new BukkitRunnable() {
            @Override
            public void run() {
                comprobarActualizaciones();
            }
        }.runTask(this); // Ejecuta la tarea en el siguiente tick
        // Bstats
        int pluginId = 23185; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);
    }
    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&fDisabled, (&aVersion: &b" + version + "&f)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&6Thanks for using my plugin :)"));
        if (bankTask != null) { bankTask.cancel(); } // Cancelar la tarea del banco al deshabilitar el plugin
    }

    public void registrarComandos() {
        this.getCommand("minebank").setExecutor(new ComandoPrincipal(this));
        this.getCommand("bank").setExecutor(new ComandoBank(this));

        // TabComplete
        this.getCommand("minebank").setTabCompleter(new TabComplete());
        this.getCommand("bank").setTabCompleter(new TabComplete());
    }
    public void registrarEventos() {
        getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventHandler(this), this);
    }

    public MainConfigManager getConfigManager(){
        return configManager;
    }
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    public BankManager getBankManager() {
        return bankManager;
    }
    public BankInventoryManager getBankInventoryManager() {
        return bankInventoryManager;
    }

    public void comprobarActualizaciones() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=119147").openConnection();
            int timed_out = 1250;
            con.setConnectTimeout(timed_out);
            con.setReadTimeout(timed_out);
            latestversion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            if (latestversion.length() <= 10) {
                if (!version.equals(latestversion)) {
                    Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&bThere is a new version available. &e(&7" + latestversion + "&e)"));
                    Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&bYou can download it at:&f https://www.spigotmc.org/resources/119147/"));
                    updateCheckerWork = true;
                }
                else{
                    Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&aYou are using the last version. &e(&7" + latestversion + "&e)"));
                }
            }
        } catch (Exception ex) {
            updateCheckerWork = false;
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&cError while checking update."));
        }
    }
    public boolean getUpdateCheckerWork(){
        return updateCheckerWork;
    }
    public String getVersion() {
        return this.version;
    }
    public String getLatestVersion() {
        return this.latestversion;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; } economy = rsp.getProvider(); return economy != null;
    }
    public Economy getEconomy() {
        return economy;
    }
    public BankTask getBankTask() {
        return bankTask;
    }
    public void scheduleBankTask() {
        // Cancelar la tarea existente si ya está programada
        if (bankTask != null) {
            bankTask.cancel();
        }
        // Crear una nueva instancia de BankTask
        bankTask = new BankTask(this);
        // Obtener el intervalo de tiempo desde la configuración y convertirlo en ticks
        long interval = getConfig().getLong("bank.profit.interval", 300) * 20L; // 300 es un valor predeterminado si no se encuentra en la configuración
        // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
        bankTask.runTaskTimer(this, interval, interval);
    }
}