package com.Guayand0;

import com.Guayand0.api.*;
import com.Guayand0.commands.*;
import com.Guayand0.converters.*;
import com.Guayand0.events.*;
import com.Guayand0.managers.*;
import com.Guayand0.tasks.*;
import com.Guayand0.utils.*;
import com.google.gson.JsonObject;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.SocketTimeoutException;
import java.util.*;

public class MineBank extends JavaPlugin {

    public final String prefix = "&4&l[&6&lMine&a&lBank&4&l]&f";
    public final String pluginName = getDescription().getName().toLowerCase();
    public final String currentVersion = getDescription().getVersion();
    public String lastVersion;
    public boolean updateCheckerWork = true;
    public boolean PlaceholderAPIEnable = false;
    public boolean enablePlugin = true;
    public final List<String> pluginHooksList = new ArrayList<>(); // Lista de plugins conectados con MineBank
    public final Map<String, String> placeholders = new HashMap<>();
    public final Map<String, Map<String, String>> playerPlaceholders = new HashMap<>(); // Actualizar los datos de cada jugador en el gui

    public final static int spigotID = 119147;
    public final static int bstatsID = 23185;

    private final MessageUtils MU = new MessageUtils();
    private final UpdateChecker UC = new UpdateChecker();
    private final BankUtils BU = new BankUtils();

    private LanguageManager languageManager;
    private FileManager fileManager;
    private BankInventoryEvent bankInventoryEvent;
    private ProfitBankTask bankTask;
    private BankPermissionTask bankPermissionTask;

    private Economy economy;

    @Override
    public void onEnable() {
        try {
            startServer();
            if (BankUtils.getDeleteOnStartExceptions(this)) Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(ExceptionManager.deleteLogFile(this), placeholders));

            if (enablePlugin) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText("&7<------------------------------------>"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fEnabled, (&aVersion: &e" + currentVersion + "&f)"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &bThanks for use my plugin :)"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &eMade by &dGuayand0"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText("&7<------------------------------------>"));
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError while enabling plugin."));
            getServer().getPluginManager().disablePlugin(this);
            enablePlugin = false;
            e.printStackTrace();
            if (BankUtils.getSaveException(this)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + ExceptionManager.saveInLog(e, this)));
        }
    }

    @Override
    public void onDisable() {
        if (bankTask != null) { bankTask.cancel(); } // Cancelar la tarea del banco al deshabilitar el plugin
        if (bankPermissionTask != null) { bankPermissionTask.cancel(); }
        Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fDisabled, (&aVersion: &b" + currentVersion + "&f)"));
    }

    public void startServer() {

        languageManager = new LanguageManager(this);
        fileManager = new FileManager(this);
        bankInventoryEvent = new BankInventoryEvent(this);

        Bukkit.getConsoleSender().sendMessage(MU.getColoredText("&7<------------------------------------>"));

        // Al actualizar de la 4.x.x a la 5.x.x
        new PlayerBankDataConverter(this).convertYamlToJson();
        new BanksConverter(this).convertYamlToJson();
        new GuiFolderFilesConverter(this).convertGuiFolder();
        new MessagesFolderFilesConverter(this).convertMessagesFolder();

        if (setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fVault found and economy manager hooked successfully."));

            // Verificar si Essentials está presente
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fEssentials economy hooked on Vault."));
            } else {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fUnknown economy hooked on Vault."));
            }
        } else {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cVault or an economy manager plugin not found!"));
            getServer().getPluginManager().disablePlugin(this);
            enablePlugin = false;
            return;
        }

        // Usar variables PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPIEnable = true;
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fPlaceholderAPI detected. Registering placeholders..."));
            try {
                new PlaceholderAPIMineBank(this).register();
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fMineBank placeholders registered successfully."));
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError registering MineBank placeholders: " + e.getMessage()));
                if (BankUtils.getSaveException(this)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + ExceptionManager.saveInLog(e, this)));
            }
        }

        saveDefaultConfig();
        getLastVersion();
        registrarPluginPlaceholders();

        registrarComandos();
        registrarEventos();
        updatePlaceholdersTask();

        new Metrics(this, bstatsID);// Bstats

        // Ejecuta la tarea en el siguiente tick
        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleRegisterBankPermissions();
                scheduleBankProfitTask();
            }
        }.runTask(this);

        // Ejecutar comprobarActualizaciones() en bucle después de que el servidor haya iniciado completamente
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            getLastVersion();
            comprobarActualizaciones();
        }, 0, 576000L); // Cada 8 horas // 576000L
    }

    private void registrarComandos() {
        this.getCommand(pluginName).setExecutor(new CommandPrincipal(this));
        this.getCommand("bank").setExecutor(new CommandBank(this));

        // TabComplete
        this.getCommand(pluginName).setTabCompleter(new TabComplete(this));
        this.getCommand("bank").setTabCompleter(new TabComplete(this));
    }

    private void registrarEventos() {
        getServer().getPluginManager().registerEvents(new CheckForUpdates(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new BankInventoryEvent(this), this);
    }

    // Registrar los placeholders del plugin
    public void registrarPluginPlaceholders() {
        placeholders.clear();

        placeholders.put("%plugin%", prefix);
        placeholders.put("%chatplugin%", BU.getChatPrefix(this));
        placeholders.put("%version%", currentVersion);
        placeholders.put("%latestversion%", lastVersion);
        placeholders.put("%link%", "https://www.spigotmc.org/resources/" + spigotID);
        placeholders.put("%author%", "Guayand0");
        placeholders.put("%moneysymbol%", "\\" + BU.getMoneySymbol(this));
        placeholders.put("%datastorage%", BU.getBankDataType(this));

        // Lista de plugins conectados con MineBank
        pluginHooksList.clear();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) { pluginHooksList.add("Vault"); }
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) { pluginHooksList.add("Essentials"); }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) { pluginHooksList.add("PlaceholderAPI"); }
        placeholders.put("%pluginhookslist%", resolvePluginHooksListPlaceholder());
    }

    // Registrar/Actualizar placeholders de inventario a cada jugador
    public void updatePlaceholdersTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                for (Player player : Bukkit.getOnlinePlayers()) {

                    String playerName = player.getName();

                    // Obtener datos del banco del jugador
                    JsonObject bank = BU.getBankDataOfPlayerName(this, playerName);

                    // Crear un mapa de placeholders para cada jugador
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%playername%", playerName);
                    placeholders.put("%playerbankname%", BU.getPlayerBankName(bank));
                    placeholders.put("%playerbankbalance%", String.valueOf(BU.getPlayerBankBalance(bank)));
                    placeholders.put("%playerbanklevel%", String.valueOf(BU.getPlayerBankLevel(bank)));
                    placeholders.put("%playerbanktop%", String.valueOf(BU.getPlayerBankTop(this, player)));
                    placeholders.put("%playerofflineaccruedprofit%", String.valueOf(BU.getPlayerOfflineAccruedProfit(bank)));
                    placeholders.put("%playerbankmaxbalance%", String.valueOf(BU.getBankMaxBalanceByLevel(this, playerName)));
                    placeholders.put("%playerbanknextlevelcost%", String.valueOf(BU.getBankUpgradeCostByLevel(this, playerName)));
                    placeholders.put("%playerbankmaxlevel%", String.valueOf(BU.getBankMaxLevel(this, playerName)));
                    placeholders.put("%playereconomybalance%", String.valueOf(BU.getPlayerBalance(player, economy)));

                    placeholders.put("%moneysymbol%", "\\" + BU.getMoneySymbol(this));

                    // Aplicar reemplazo de top placeholders
                    List<List<String>> topBanks = BU.getTopPlayerBanks(this, 100);
                    int position = 1;

                    for (List<String> bankInfo : topBanks) {
                        placeholders.put("%banktopbankposition_" + position + "%", String.valueOf(position));
                        placeholders.put("%banktopplayername_" + position + "%", bankInfo.get(0));
                        placeholders.put("%banktopbankname_" + position + "%", bankInfo.get(1));
                        placeholders.put("%banktopbanklevel_" + position + "%", bankInfo.get(2));
                        placeholders.put("%banktopbankbalance_" + position + "%", bankInfo.get(3));
                        position++;
                    }

                    // Guardar los placeholders en el mapa general con el nombre del jugador
                    playerPlaceholders.put(playerName, placeholders);
                }
            } catch (Exception ignored) {}

        }, 0, 20L);
    }

    // Resolver el placeholder para %pluginHooksList%
    private String resolvePluginHooksListPlaceholder() {
        StringBuilder hooksListString = new StringBuilder();
        for (String pluginName : pluginHooksList) {
            hooksListString.append("&6&l>> &e").append(pluginName).append("\n");
        }
        return hooksListString.toString();
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public BankInventoryEvent getBankInventoryEvent() {
        return bankInventoryEvent;
    }

    // Metodo para obtener ultima version
    private void getLastVersion() {
        try {
            lastVersion = UC.getLatestSpigotVersion(spigotID, 5000);  // Obtener la última versión desde la clase UpdateChecker
        } catch (SocketTimeoutException ex) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cConnection timed out. The version will be checked later."));
            lastVersion = currentVersion;
            updateCheckerWork = false;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError while checking update."));
            lastVersion = currentVersion;
            updateCheckerWork = false;
        }
    }

    // Metodo para comprobar nuevas actualizaciones
    public void comprobarActualizaciones() {
        if (UC.compareVersions(currentVersion, lastVersion) < 0) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(prefix + " &bThere is a new version available. &f(&e%latestVersion%&f).", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(prefix + " &bDownload it here: &f%link%", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(prefix + " &bSome updates may require you to change some things manually.", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(prefix + " &bRead changelog: &f%link%/updates", placeholders));
        } else {
            if (!updateCheckerWork) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &aYou are using the last version. &f(&b" + currentVersion + "&f)"));
            }
        }
        updateCheckerWork = true;
    }

    // Comprobar si tiene PlaceholderAPI activado
    public boolean getPlaceholderAPI(){
        return PlaceholderAPIEnable;
    }

    // Usar economia de vault
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; } economy = rsp.getProvider(); return economy != null;
    }

    public Economy getEconomy() {
        return this.economy;
    }

    // Task del banco
    private void scheduleBankProfitTask() {

        // Si el banco está desactivado
        if (!BU.getBankAllowed(this)) {
            return;
        }

        // Cancelar la tarea existente si ya está programada
        if (bankTask != null) {
            bankTask.cancel();
        }

        // Obtener el intervalo de tiempo desde la configuración y convertirlo en ticks
        long interval = BU.getProfitIntervalInSeconds(this) * 20L;

        // Si es -1 esta desactivado
        if (interval < 0) {
            return;
        }

        // Crear una nueva instancia de BankTask
        bankTask = new ProfitBankTask(this);

        // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
        bankTask.runTaskTimer(this, interval, interval);
    }

    public void updateBankProfitTask() {
        scheduleBankProfitTask();
    }

    // Registrar un permiso para cada banco
    private void scheduleRegisterBankPermissions() {

        // Obtener el manejador de permisos
        PluginManager pluginManager = Bukkit.getPluginManager();

        // Cancelar la tarea existente si ya está programada
        if (bankPermissionTask != null) {
            bankPermissionTask.cancel();

            // Eliminar todos los permisos de los bancos antes de volver a registrarlos
            for (Permission permission : pluginManager.getPermissions()) {
                if (permission.getName().startsWith("minebank.bank.")) {
                    pluginManager.removePermission(permission);
                }
            }
        }

        try {

            // Crea un permiso con en nombre de cada banco
            for (String bankName : BU.getBankNames(this)) {

                // Crear el nombre del permiso dinámicamente
                String permissionName = "minebank.bank." + bankName.toLowerCase();

                // Comprobar si el permiso ya está registrado
                if (pluginManager.getPermission(permissionName) == null) {

                    // Crear el permiso
                    Permission bankPermission = new Permission(permissionName);

                    // Registrar el permiso en el PluginManager
                    pluginManager.addPermission(bankPermission);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(this)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + ExceptionManager.saveInLog(e, this)));
        }

        // Crear una nueva instancia de BankPermissionTask
        bankPermissionTask = new BankPermissionTask(this);

        // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
        bankPermissionTask.runTaskTimer(this, 40L, 40L);
    }

    public void updateRegisterBankPermissions () {
        scheduleRegisterBankPermissions();
    }
}
