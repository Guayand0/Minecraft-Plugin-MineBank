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
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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

    public final static int spigotID = 119147;
    public final static int bstatsID = 23185;

    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();
    private final UpdateChecker UC = new UpdateChecker();
    private final GetValues GV = new GetValues();
    private final PlayerUtils PU = new PlayerUtils();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();

    private BukkitTask bankProfitTask, bankPermissionTask;
    private LanguageManager languageManager;
    private FileManager fileManager;
    private GuiMain guiMain;
    private SendMessage sendMessage;
    private DataStorage dataStorage;
    private StorageManager storageManager;
    private TransactionService transactionService;
    private TransactionGUI transactionGUI;

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
        // Cancelar la tarea del banco si está activa
        if (bankProfitTask != null) {
            bankProfitTask.cancel();
            bankProfitTask = null;
        }

        if (bankPermissionTask != null) {
            bankPermissionTask.cancel();
            bankPermissionTask = null;
        }
        Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &fDisabled, (&aVersion: &b" + currentVersion + "&f)"));
    }

    public void startServer() {

        saveDefaultConfig();
        ConfigMessageKeyUpdater keyUpdater = new ConfigMessageKeyUpdater(this);
        keyUpdater.syncConfig();
        reloadConfig();

        languageManager = new LanguageManager(this);
        if (keyUpdater.syncMessages()) {
            languageManager.reloadMessages();
        }
        fileManager = new FileManager(this);
        guiMain = new GuiMain(this);
        sendMessage = new SendMessage(this);

        new Update_4XX_501(this); // 4.x.x a 5.0.1
        new Update_501_511(this); // 5.0.1 a 5.1.1
        new Update_51X_521(this); // 5.1.x a 5.2.1
        new Update_5XX_523(this); // 5.x.x a 5.2.3

        if (!setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cVault/Economy plugin not found!"));
            getServer().getPluginManager().disablePlugin(this);
            enablePlugin = false;
            return;
        }

        // Usar variables PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPIEnable = true;
            Bukkit.getScheduler().runTaskLater(this, () -> {
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
        getLastVersion();

        registrarPluginPlaceholders();
        registrarComandos();
        registrarEventos();

        new Metrics(this, bstatsID); // Bstats

        UpdateItemsGUI updater = new UpdateItemsGUI(this);
        updater.start();

        // Ejecuta la tarea en el siguiente tick
        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleRegisterBankPermissionTask();
                scheduleBankProfitTask();
            }
        }.runTask(this);

        // Ejecutar comprobarActualizaciones() en bucle después de que el servidor haya iniciado completamente
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            getLastVersion();
            comprobarActualizaciones();
        }, 100L, 576000L); // Cada 8 horas // 576000L
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
        placeholders.put("%datastorage%", GV.getString(this, "bank.data.type", "---").toUpperCase());
        placeholders.put("%moneysymbol%", GV.getString(this, "bank.money.symbol", "$"));
        placeholders.put("%offlinemaxprofittimes%", GV.getString(this, "bank.profit.times-profits-offline"));

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
                // Usar JSON directamente
                dataStorage = storageManager.get(StorageType.JSON);

            } else if (storageType == StorageType.MYSQL) {
                // Crear MySQLStorage solo ahora
                MySQLStorage mysql = new MySQLStorage(
                        getConfig().getString("bank.data.host"),
                        getConfig().getInt("bank.data.port"),
                        getConfig().getString("bank.data.database"),
                        getConfig().getString("bank.data.user"),
                        getConfig().getString("bank.data.password"),
                        getConfig().getString("bank.data.connection_params")
                );
                mysql.prepareTables();
                storageManager.register(StorageType.MYSQL, mysql);
                dataStorage = mysql;

            } else {
                dataStorage = storageManager.get(storageType);
                if (dataStorage == null) {
                    throw new Exception("Storage not found in StorageManager");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(this, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + EM.saveInLog(e, this)));
            // Fallback a JSON en cualquier fallo
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cFailed to initialize storage '" + storageType + "'. Using JSON by default!"));
            dataStorage = storageManager.get(StorageType.JSON);
        }

        // Protección extra por si storageManager devuelve null
        if (dataStorage == null) {
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
                bankProfitTask.cancel();
            }

            long intervalSeconds = GV.getInt(this, "bank.profit.interval-in-seconds", -1);

            // -1 = desactivado
            if (intervalSeconds < 0) return;

            long intervalTicks = intervalSeconds * 20L;

            // Programar la tarea para que se ejecute repetidamente con el intervalo configurado
            bankProfitTask = new ProfitBankTask(this).runTaskTimer(this, intervalTicks, intervalTicks);

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
        bankPermissionTask = new BankPermissionTask(this).runTaskTimer(this, 5L, 5L);
    }

    public void updateRegisterBankPermissionTask() {
        scheduleRegisterBankPermissionTask();
    }

    // Metodo para obtener ultima version
    private void getLastVersion() {
        try {
            lastVersion = UC.getLatestSpigotVersion(spigotID, 5000);  // Obtener la última versión desde la clase UpdateChecker
        } catch (SocketTimeoutException ex) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cConnection timed out. The version will be checked later"));
            lastVersion = currentVersion;
            updateCheckerWork = false;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(prefix + " &cError while checking update"));
            lastVersion = currentVersion;
            updateCheckerWork = false;
        }
    }

    // Metodo para comprobar nuevas actualizaciones
    public void comprobarActualizaciones() {
        if (UC.compareVersions(currentVersion, lastVersion) < 0) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &fNew version available!", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("&fCurrent version: &c%version%&f, latest version: &a%latestVersion%&f!", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(""));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("   &eSpigotMC -> &f%link%", placeholders));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(""));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &bSome updates may require you to change some things manually.", placeholders));
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

    public Map<UUID, PendingMigration> getPendingMigrations() {
        return pendingMigrations;
    }

}
