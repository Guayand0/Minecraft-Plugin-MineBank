package com.Guayand0;

import com.Guayand0.api.*;
import com.Guayand0.commands.*;
import com.Guayand0.converters.*;
import com.Guayand0.data.*;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionService;
import com.Guayand0.data.transactions.TransactionStorage;
import com.Guayand0.dbmigration.PendingMigration;
import com.Guayand0.dbmigration.StorageManager;
import com.Guayand0.dbmigration.StorageType;
import com.Guayand0.events.*;
import com.Guayand0.guis.TransactionGUI;
import com.Guayand0.managers.*;
import com.Guayand0.tasks.BankPermissionTask;
import com.Guayand0.tasks.ProfitBankTask;
import com.Guayand0.tasks.UpdateItemsGUI;
import com.Guayand0.utils.*;
import com.Guayand0.utils.gui.GuiMain;
import com.Guayand0.zlib.*;
import com.Guayand0.api.WebServer;
import com.Guayand0.api.WebTokenStore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MineBank extends JavaPlugin {

    public final String prefix = "&4&l[&6&lMine&a&lBank&4&l]&f";
    public final String pluginName = getDescription().getName().toLowerCase();
    public final String currentVersion = getDescription().getVersion();
    public String lastVersion;
    public boolean updateCheckerWork = true;
    public boolean PlaceholderAPIEnable = false;
    public boolean enablePlugin = true;
    public final List<String> pluginHooksList = new ArrayList<>(); // Lista de plugins conectados
    public final Map<String, String> placeholders = new HashMap<>();
    private final Map<UUID, PendingMigration> pendingMigrations = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingBankDeletions = new ConcurrentHashMap<>();

    public final static int spigotID = 119147;
    public final static int bstatsID = 23185;

    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final UpdateChecker UC = new UpdateChecker();
    private final GetValues GV = new GetValues();
    private final PlayerUtils PU = new PlayerUtils();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    private Object bankProfitTask, bankPermissionTask;
    private LanguageManager languageManager;
    private GuiMain guiMain;
    private SendMessage sendMessage;
    private DataStorage dataStorage;
    private StorageManager storageManager;
    private TransactionService transactionService;
    private TransactionGUI transactionGUI;
    private EventManager eventManager;
    private WebServer webServer;
    private WebTokenStore webTokenStore;
    private SchedulerCompat schedulerCompat;

    private Economy economy;

    @Override
    public void onEnable() {
        try {
            startServer();
            if (GV.getBoolean(this, "exception.delete-on-start", false)) Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText(EM.deleteLogFile(this), placeholders));

            if (enablePlugin) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText("&7<------------------------------------>"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &f- (&aVersion: &b" + currentVersion + "&f), &fBy &dGuayand0 &f- &6Thanks for downloading!"));
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText("&7<------------------------------------>"));
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError while enabling plugin"));
            getServer().getPluginManager().disablePlugin(this);
            enablePlugin = false;
            e.printStackTrace();
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
        }
    }

    @Override
    public void onDisable() {
        if (bankProfitTask != null) {
            schedulerCompat.cancelTask(bankProfitTask);
            bankProfitTask = null;
        }

        if (bankPermissionTask != null) {
            schedulerCompat.cancelTask(bankPermissionTask);
            bankPermissionTask = null;
        }

        if (eventManager != null) {
            eventManager.cancelAllEvents();
        }

        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (webTokenStore != null) {
            webTokenStore.clear();
        }
        Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fDisabled, (&aVersion: &b" + currentVersion + "&f)"));
    }

    public void startServer() {

        saveDefaultConfig();
        ConfigMessageKeyUpdater keyUpdater = new ConfigMessageKeyUpdater(this);
        keyUpdater.syncConfig();
        reloadConfig();

        languageManager = new LanguageManager(this);
        if (keyUpdater.syncMessages()) { languageManager.reloadMessages(); }
        eventManager = new EventManager();
        new FileManager(this);
        guiMain = new GuiMain(this);
        sendMessage = new SendMessage(this);
        schedulerCompat = new SchedulerCompat(this);

        new Update_4XX_501(this); // 4.x.x a 5.0.1
        new Update_501_511(this); // 5.0.1 a 5.1.1
        new Update_51X_521(this); // 5.1.x a 5.2.1
        new Update_5XX_523(this); // 5.x.x a 5.2.3

        if (!setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cVault or an economy manager plugin not found!"));
            getServer().getPluginManager().disablePlugin(this);
            enablePlugin = false;
            return;
        }

        // Usar variables PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPIEnable = true;
            schedulerCompat.runGlobalLater(() -> {
                try {
                    new PAPIVariables(this).register();
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fHooked into &aPlaceholderAPI&f!"));
                } catch (Exception e) {
                    e.printStackTrace();
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError registering placeholders: " + e.getMessage()));
                    if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
                }
            }, 40L); // espera 2 segundos (40 ticks)
        }

        setupStorages();
        getDataStorageType();
        setupTransactionStorage();
        lastVersion = currentVersion;

        webTokenStore = new WebTokenStore(this);

        registrarPluginPlaceholders();
        registrarComandos();
        registrarEventos();

        startWebServer();

        Metrics metrics = new Metrics(this, bstatsID); // Bstats
        metrics.addCustomChart(new Metrics.SimplePie("database_system", this::getBStatsDatabaseSystem));
        metrics.addCustomChart(new Metrics.SimplePie("web_panel", this::getBStatsWebEnabledStatus));

        UpdateItemsGUI updater = new UpdateItemsGUI(this);
        updater.start();

        // Ejecuta la tarea en el siguiente tick
        schedulerCompat.runGlobal(() -> {
            scheduleRegisterBankPermissionTask();
            scheduleBankProfitTask();
        });

        // Ejecutar comprobarActualizaciones() en bucle después de que el servidor haya iniciado completamente
        schedulerCompat.runGlobalTimer(this::checkUpdatesAsync, 100L, 576000L); // Cada 8 horas // 576000L
    }

    private void startWebServer() {
        try {
            boolean enabled = GV.getBoolean(this, "web.enabled", false);
            int port = GV.getInt(this, "web.port", 16104);
            if (!enabled) return;

            webServer = new WebServer(this, port);
            webServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
        }
    }

    public void restartWebServer() {
        try {
            if (webServer != null) {
                webServer.stop();
                webServer = null;
            }
            startWebServer();
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
        }
    }

    private void registrarComandos() {
        this.getCommand(pluginName).setExecutor(new MineBankCommand(this));
        this.getCommand("bank").setExecutor(new BankCommand(this));

        // TabComplete
        this.getCommand(pluginName).setTabCompleter(new TabComplete(this));
        this.getCommand("bank").setTabCompleter(new TabComplete(this));
    }

    private void registrarEventos() {
        getServer().getPluginManager().registerEvents(new CheckForUpdates(this), this);
        getServer().getPluginManager().registerEvents(new OnInventoryClick(this), this);
        getServer().getPluginManager().registerEvents(new OnInventoryClose(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerExit(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
    }

    // Registrar los placeholders del plugin para los mensajes
    public void registrarPluginPlaceholders() {
        placeholders.clear();

        placeholders.put("%plugin%", prefix);
        placeholders.put("%chatplugin%", GV.getString(this, "config.chat-prefix", "&4&l[&6&lMine&a&lBank&4&l]&f"));
        placeholders.put("%version%", currentVersion);
        placeholders.put("%latestversion%", lastVersion);
        placeholders.put("%link%", "https://www.spigotmc.org/resources/" + spigotID);
        placeholders.put("%author%", "Guayand0");
        placeholders.put("%datastorage%", GV.getString(this, "bank.data.type").toUpperCase());
        placeholders.put("%moneysymbol%", GV.getString(this, "bank.money.symbol", "$"));
        placeholders.put("%offlinemaxprofittimes%", GV.getString(this, "bank.profit.times-profits-offline"));
        placeholders.put("%webhost%", GV.getString(this, "web.host", "localhost"));
        placeholders.put("%webport%", String.valueOf(GV.getInt(this, "web.port", 16104)));

        // Lista de plugins conectados
        pluginHooksList.clear();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) { pluginHooksList.add("Vault"); }
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) { pluginHooksList.add("Essentials"); }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) { pluginHooksList.add("PlaceholderAPI"); }
        placeholders.put("%pluginhookslist%", resolvePluginHooksListPlaceholder());
    }

    public Map<String, String> buildPlayerPlaceholders(UUID uuid) {
        Map<String, String> ph = new HashMap<>(placeholders);
        if (uuid == null) return ph;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String playerName = offline.getName() != null ? offline.getName() : "Unknown";

        // Cargar datos del jugador
        PlayerData playerData = dataStorage.loadPlayerData(uuid);
        if (playerData == null) return ph;

        String bankName = playerData.getBank().getName();
        int bankLevel = playerData.getBank().getLevel();
        int bankBalance = playerData.getBank().getBalance();
        int offlineProfitAccrued = playerData.getBank().getOffline().getAccrued_profit();
        int offlineProfitTimes = playerData.getBank().getOffline().getProfit_times();

        Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
        if (bankDataMap == null || !bankDataMap.containsKey(bankName)) return ph;
        BankData bankData = bankDataMap.get(bankName);

        int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
        int upgradeCost = bankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();
        int bankMaxLevel = bankData.getLevels().size();

        int playerTop = getPlayerTopPosition(uuid);

        if (bankLevel >= bankMaxLevel) { upgradeCost = 0; }

        ph.put("%playerName%", playerName);
        ph.put("%playerBankName%", bankName);
        ph.put("%playerBankBalance%", BSP.format(this, String.valueOf(bankBalance)));
        ph.put("%playerBankLevel%", String.valueOf(bankLevel));
        ph.put("%offlineProfitAmount%", BSP.format(this, String.valueOf(offlineProfitAccrued)));
        ph.put("%offlineProfitTimes%", String.valueOf(offlineProfitTimes));

        ph.put("%playerBankMaxBalance%", BSP.format(this, String.valueOf(bankMaxBalance)));
        ph.put("%playerBankMaxLevel%", String.valueOf(bankMaxLevel));
        ph.put("%playerBankNextLevelCost%", BSP.format(this, String.valueOf(upgradeCost)));
        ph.put("%playerBankTop%", playerTop == -1 ? "-" : String.valueOf(playerTop));

        // Economía: si está online, usamos el Player real, si no, ponemos N/A
        if (offline.isOnline()) {
            Player onlinePlayer = offline.getPlayer();
            ph.put("%playerEconomyBalance%", String.valueOf(BSP.format(this, String.valueOf(PU.getPlayerBalance(onlinePlayer, getEconomy())))));
        } else {
            ph.put("%playerEconomyBalance%", "N/A");
        }

        // Rellenar top bancario
        int topAmount = 100;
        List<List<String>> topBanks = dataStorage.getTopPlayerBankData(topAmount);
        int position = 1;

        for (int i = 1; i <= topAmount; i++) {
            ph.put("%banktopbankposition_" + i + "%", String.valueOf(position));
            ph.put("%banktopplayername_" + i + "%", "-");
            ph.put("%banktopbankname_" + i + "%", "-");
            ph.put("%banktopbanklevel_" + i + "%", "-");
            ph.put("%banktopbankbalance_" + i + "%", "-");
            position++;
        }

        position = 1;
        for (List<String> bankInfo : topBanks) {
            ph.put("%banktopbankposition_" + position + "%", String.valueOf(position));
            ph.put("%banktopplayername_" + position + "%", bankInfo.get(0));
            ph.put("%banktopbankname_" + position + "%", bankInfo.get(1));
            ph.put("%banktopbanklevel_" + position + "%", bankInfo.get(2));
            ph.put("%banktopbankbalance_" + position + "%", BSP.format(this, bankInfo.get(3)));
            position++;
        }

        return ph;
    }

    // Resolver el placeholder para %pluginHooksList%
    private String resolvePluginHooksListPlaceholder() {
        StringBuilder hooksListString = new StringBuilder();
        for (String pluginName : pluginHooksList) {
            hooksListString.append("&6&l>> &e").append(pluginName).append("\n");
        }
        return hooksListString.toString();
    }

    public int getPlayerTopPosition(UUID targetUuid) {

        List<List<String>> top = dataStorage.getTopPlayerBankData(Integer.MAX_VALUE);
        String targetName = PU.getNameFromUUID(targetUuid);

        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).get(0).equalsIgnoreCase(targetName)) {
                return i + 1; // posiciones empiezan en 1
            }
        }

        return -1; // no está en el ranking
    }

    private void setupStorages() {
        // Inicializar StorageManager
        storageManager = new StorageManager();

        // Registrar JSON siempre
        JsonStorage json = new JsonStorage(getDataFolder());
        storageManager.register(StorageType.JSON, json);

        // Registrar MYSQL solo como "placeholder" o null
        storageManager.register(StorageType.MYSQL, null);
        //storageManager.register(StorageType.POSTGRESQL, null);
        storageManager.register(StorageType.SQLITE, null);
        //storageManager.register(StorageType.MONGODB, null);
    }

    private String getBStatsDatabaseSystem() {
        String type = GV.getString(this, "bank.data.type", "JSON");
        if (type == null || type.trim().isEmpty()) {
            return "JSON";
        }

        switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "MYSQL":
                return "MySQL";
            case "SQLITE":
                return "SQLite";
            case "JSON":
                return "JSON";
            default:
                return "OTHER";
        }
    }

    private String getBStatsWebEnabledStatus() {
        boolean enabled = GV.getBoolean(this, "web.enabled", false);
        return enabled ? "Enabled" : "Disabled";
    }

    // Metodo para obtener el tipo de almacenamiento de datos
    private void getDataStorageType() {
        String typeStr = GV.getString(this, "bank.data.type", "JSON").toUpperCase();

        // Validar contra StorageType enum
        StorageType storageType = StorageType.fromString(typeStr);

        if (storageType == null) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cInvalid data storage type. Using JSON by default"));
            storageType = StorageType.JSON;
        }

        try {
            if (storageType == StorageType.JSON) {
                dataStorage = storageManager.get(StorageType.JSON);

            } else if (storageType == StorageType.MYSQL) {
                String mysqlUri = getConfig().getString("bank.data.mysql-uri", "");
                MySQLStorage mysql;
                if (!mysqlUri.isEmpty()) {
                    mysql = new MySQLStorage(mysqlUri);
                } else {
                    mysql = new MySQLStorage(
                            getConfig().getString("bank.data.host", "localhost"),
                            getConfig().getInt("bank.data.port", 3306),
                            getConfig().getString("bank.data.database", "minebank"),
                            getConfig().getString("bank.data.user", "root"),
                            getConfig().getString("bank.data.password", ""),
                            getConfig().getString("bank.data.connection_params", "")
                    );
                }

                mysql.prepareTables();
                storageManager.register(StorageType.MYSQL, mysql);
                dataStorage = mysql;

            } else if (storageType == StorageType.SQLITE) {
                SQLiteStorage sqlite = new SQLiteStorage(getDataFolder());
                sqlite.prepareTables();
                storageManager.register(StorageType.SQLITE, sqlite);
                dataStorage = sqlite;

            }
            /*else if (storageType == StorageType.POSTGRESQL) {
                String pgUri = getConfig().getString("bank.data.postgresql-uri", "");
                PostgreSQLStorage pg;
                if (!pgUri.isEmpty()) {
                    pg = new PostgreSQLStorage(pgUri);
                } else {
                    pg = new PostgreSQLStorage(
                            getConfig().getString("bank.data.host", "localhost"),
                            getConfig().getInt("bank.data.port", 5432),
                            getConfig().getString("bank.data.database", "minebank"),
                            getConfig().getString("bank.data.user", "postgres"),
                            getConfig().getString("bank.data.password", ""),
                            getConfig().getString("bank.data.connection_params", "")
                    );
                }

                pg.prepareTables();
                storageManager.register(StorageType.POSTGRESQL, pg);
                dataStorage = pg;

            } else if (storageType == StorageType.MONGODB) {
                String mongoUri = getConfig().getString("bank.data.mongodb-uri", "");
                MongoDBStorage mongo;
                if (!mongoUri.isEmpty()) {
                    mongo = new MongoDBStorage(mongoUri);
                } else {
                    mongo = new MongoDBStorage(
                            getConfig().getString("bank.data.host", "localhost"),
                            getConfig().getInt("bank.data.port", 27017),
                            getConfig().getString("bank.data.database", "minebank"),
                            getConfig().getString("bank.data.user", ""),
                            getConfig().getString("bank.data.password", ""),
                            getConfig().getString("bank.data.connection_params", "")
                    );
                }

                mongo.prepareCollections();
                storageManager.register(StorageType.MONGODB, mongo);
                dataStorage = mongo;
            }*/

        } catch (Exception e) {
            // Fallback a JSON en cualquier fallo
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cFailed to initialize storage '" + storageType + "'. Using JSON by default!"));
            dataStorage = storageManager.get(StorageType.JSON);
        }
    }

    private void setupTransactionStorage() {
        TransactionStorage transactionStorage;

        if (dataStorage instanceof TransactionStorage) {
            transactionStorage = (TransactionStorage) dataStorage;
        } else {
            transactionStorage = new JsonStorage(getDataFolder());
        }

        transactionService = new TransactionService(this, transactionStorage);
        transactionService.initialize();
        transactionGUI = new TransactionGUI(this, transactionService);
    }

    public void scheduleBankProfitTask() {
        try {
            // Banco desactivado
            if (!GV.getBoolean(this, "config.bank-allowed", true)) return;

            // Cancelar si ya existe
            if (bankProfitTask != null) {
                schedulerCompat.cancelTask(bankProfitTask);
            }

            long intervalSeconds = GV.getInt(this, "bank.profit.interval-in-seconds", -1);

            // -1 = desactivado
            if (intervalSeconds < 0) return;

            long intervalTicks = intervalSeconds * 20L;

            // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
            ProfitBankTask task = new ProfitBankTask(this);
            bankProfitTask = schedulerCompat.runGlobalTimer(task::run, intervalTicks, intervalTicks);

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
        }
    }

    public void updateBankProfitTask() {
        scheduleBankProfitTask();
    }

    public void scheduleRegisterBankPermissionTask() {

        // Obtener el manejador de permisos
        PluginManager pluginManager = Bukkit.getPluginManager();

        if (bankPermissionTask != null) {
            schedulerCompat.cancelTask(bankPermissionTask);

            // Eliminar todos los permisos de los bancos antes de volver a registrarlos
            for (Permission permission : pluginManager.getPermissions()) {
                if (permission.getName().startsWith("minebank.bank.")) {
                    pluginManager.removePermission(permission);
                }
            }
        }

        try {
            // Crea un permiso con en nombre de cada banco
            for (String bankName : dataStorage.getAllBankNames()) {

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
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
        }

        // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
        BankPermissionTask task = new BankPermissionTask(this);
        bankPermissionTask = schedulerCompat.runGlobalTimer(task::run, 5L, 5L);
    }

    public void updateRegisterBankPermissionTask() {
        scheduleRegisterBankPermissionTask();
    }

    private void checkUpdatesAsync() {
        schedulerCompat.runAsync(() -> {
            try {
                String latest = UC.getLatestSpigotVersion(spigotID, 5000);  // Obtener la última versión desde la clase UpdateChecker
                schedulerCompat.runGlobal(() -> {
                    lastVersion = latest != null ? latest : currentVersion;
                    updateCheckerWork = true;
                    registrarPluginPlaceholders();
                    comprobarActualizaciones();
                });
            } catch (Exception ex) {
                String errorMessage = ex instanceof SocketTimeoutException
                        ? prefix + " &cConnection timed out. The version will be checked later"
                        : prefix + " &cError while checking update";
                schedulerCompat.runGlobal(() -> {
                    Bukkit.getConsoleSender().sendMessage(MU.getColoredText(errorMessage));
                    lastVersion = currentVersion;
                    updateCheckerWork = false;
                });
            }
        });
    }

    // Metodo para comprobar nuevas actualizaciones
    public void comprobarActualizaciones() {
        if (UC.compareVersions(currentVersion, lastVersion) < 0) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &fNew version available!", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("&fCurrent version: &c%version%&f, latest version: &a%latestVersion%&f!", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(""));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &eSpigotMC    -> &f%link%", placeholders));
            //Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &aModrinth   -> &fhttps://modrinth.com/plugin/minebank", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &bVoxel       -> &fhttps://voxel.shop/product/8153", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &9BuiltByBit  -> &fhttps://builtbybit.com/resources/100839", placeholders));
            //Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &9CourseForge -> &fhttps://legacy.curseforge.com/minecraft/bukkit-plugins/minebank-custom-gui-custom-banks-web-panel-papi-support", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(""));
            //Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &bSome updates may require you to change some things manually.", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &bRead changelog: &f%link%/updates", placeholders));
        } else {
            if (!updateCheckerWork) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &aYou are using the last version. &f(&b" + currentVersion + "&f)"));
            }
        }
        updateCheckerWork = true;
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

    // Comprobar si tiene PlaceholderAPI activado
    public boolean getPlaceholderAPI(){
        return PlaceholderAPIEnable;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GuiMain getMainGUI() {
        return guiMain;
    }

    public SendMessage getSendMessage() {
        return sendMessage;
    }

    public DataStorage getStorage() {
        return dataStorage;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public TransactionGUI getTransactionGUI() {
        return transactionGUI;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public WebTokenStore getWebTokenStore() {
        return webTokenStore;
    }

    public SchedulerCompat getSchedulerCompat() {
        return schedulerCompat;
    }

    public Map<UUID, PendingMigration> getPendingMigrations() {
        return pendingMigrations;
    }

    public Map<UUID, String> getPendingBankDeletions() {
        return pendingBankDeletions;
    }

}
