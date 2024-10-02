package mb.Guayando;

import mb.Guayando.api.*;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import mb.Guayando.events.*;
import mb.Guayando.commands.*;
import mb.Guayando.tasks.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MineBank extends JavaPlugin implements Listener {

    public static String prefix = "&4[&6&lMineBank&4]";
    public String version = getDescription().getVersion();
    public String latestversion;
    public boolean updateCheckerWork = true;
    public static boolean PlaceholderAPI = true;
    private Economy economy;
    private LanguageManager languageManager;
    private BankManager bankManager;
    private BankInventoryManager bankInventoryManager;
    private ProfitBankTask bankTask;
    PermissionManager permissionManager;

    @Override
    public void onEnable() {

        // Verificar si existe un plugin de economia
        if (!setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &cVault or an economy plugin not found!"));
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &cPlease ensure Vault and an economy plugin are installed."));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&9<------------------------------------>"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &fEnabled, (&aVersion: &b" + version + "&f)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &6Thanks for using my plugin :)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &eMade by &dGuayando"));
        if (setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &aVault found and linked successfully."));
        }

        saveDefaultConfig();
        this.languageManager = new LanguageManager(this);
        this.bankManager = new BankManager(this);
        this.bankInventoryManager = new BankInventoryManager(this);
        permissionManager = new PermissionManager(this);
        permissionManager.registerBankPermissions();
        registrarComandos();
        registrarEventos();
        startPlaceholderUpdateTask();

        // Crear variables PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &bPlaceholderAPI detected. Registering placeholders..."));
            try {
                new PlaceholderAPIMineBank(this).register();
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &aMineBank placeholders registered successfully."));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &cError registering placeholders: " + e.getMessage()));
                e.printStackTrace();
            }
        }  else {
            PlaceholderAPI = false;
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&9<------------------------------------>"));

        // Ejecutar comprobarActualizaciones() después de que el servidor haya iniciado completamente
        new BukkitRunnable() {
            @Override
            public void run() {
                comprobarActualizaciones();
                scheduleBankTask();
            }
        }.runTask(this); // Ejecuta la tarea en el siguiente tick

        Metrics metrics = new Metrics(this, 23185); // Bstats
    }

    @Override
    public void onDisable() {
        if (bankTask != null) { bankTask.cancel(); } // Cancelar la tarea del banco al deshabilitar el plugin
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &fDisabled, (&aVersion: &b" + version + "&f)"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &6Thanks for using my plugin :)"));
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
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
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

    // Método para comprobar ultima version
    public void comprobarActualizaciones() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=119147").openConnection();
            int timed_out = 5000;
            con.setConnectTimeout(timed_out);
            con.setReadTimeout(timed_out);
            latestversion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();

            if (compareVersions(version, latestversion) < 0) {
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&bThere is a new version available. &f(&7" + latestversion + "&f)"));
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&bYou can download it at:&f https://www.spigotmc.org/resources/119147/"));
                updateCheckerWork = true;
            } else {
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&aYou are using the last version. &f(&b" + version + "&f)"));
            }
        } catch (Exception ex) {
            updateCheckerWork = false;
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + "&cError while checking update."));
        }
    }
    // Método para comparar versiones
    public static int compareVersions(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
            int currentPart = Integer.parseInt(currentParts[i]);
            int latestPart = Integer.parseInt(latestParts[i]);
            if (latestPart > currentPart) {
                return -1; // La versión más reciente es mayor
            } else if (latestPart < currentPart) {
                return 1; // La versión actual es mayor
            }
        }
        return Integer.compare(latestParts.length, currentParts.length); // Comparar longitud si son iguales hasta el mínimo
    }
    // Por si falla la comprobación de actualización
    public boolean getUpdateCheckerWork(){
        return updateCheckerWork;
    }
    // Version actual del plugin
    public String getVersion() {
        return this.version;
    }
    // Ultima version según en spigot
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

    public void scheduleBankTask() {
        // Cancelar la tarea existente si ya está programada
        if (bankTask != null) {
            bankTask.cancel();
        }
        // Crear una nueva instancia de BankTask
        bankTask = new ProfitBankTask(this);
        // Obtener el intervalo de tiempo desde la configuración y convertirlo en ticks
        long interval = getConfig().getLong("bank.profit.intervalSeconds", 300) * 20L; // 300 es un valor predeterminado si no se encuentra en la configuración
        // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
        bankTask.runTaskTimer(this, interval, interval);
    }
    public void startPlaceholderUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    // Aquí llamamos al método que aplica los placeholders para cada jugador conectado
                    MessageUtils.applyPlaceholdersAndColor(player, null, null, this, 0, MineBank.getPlaceholderAPI());
                } catch (NullPointerException e) {
                    // Manejar el error y registrar la excepción para depuración
                    //Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &cError al aplicar placeholders para el jugador " + player.getName() + ": " + e.getMessage()));
                    //e.printStackTrace();
                } catch (Exception e) {
                    // Manejar cualquier otra excepción que pueda ocurrir
                    Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(prefix + " &cError MineBank startPlaceholderUpdateTask"));
                    e.printStackTrace();
                }
            }
        }, 0L, 40L); // 40 ticks son 2 segundos (20 ticks = 1 segundo)
    }

    public static boolean getPlaceholderAPI(){
        return PlaceholderAPI;
    }
}