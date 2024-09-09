package mb.Guayando.event;

//import com.comphenix.protocol.ProtocolManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mb.Guayando.MineBank;
import mb.Guayando.commands.ComandoBank;
import mb.Guayando.config.BankInventoryManager;
import mb.Guayando.config.BankManager;
import mb.Guayando.config.LanguageManager;
import mb.Guayando.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class BankInventoryEvent implements Listener {

    private final MineBank plugin;
    private final Economy economy;
    private final LanguageManager languageManager;
    private final BankManager bankManager;
    private final BankInventoryManager bankInventoryManager;
    //private final ProtocolManager protocolManager;
    private Inventory bankInventory;
    private Map<Player, Inventory> playerInventories; // Mapa para guardar el inventario abierto de cada jugador
    private Set<Player> updatingPlayers; // Conjunto para jugadores a los que se debe actualizar el inventario
    private Map<Player, String> playerInventoryNames; // Mapa para guardar el nombre del inventario abierto de cada jugador
    private String currentInventoryLang;

    public BankInventoryEvent(MineBank plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.languageManager = plugin.getLanguageManager();
        this.bankManager = plugin.getBankManager();
        this.bankInventoryManager = plugin.getBankInventoryManager();
        //this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.playerInventories = new HashMap<>();
        this.updatingPlayers = new HashSet<>();
        this.playerInventoryNames = new HashMap<>(); // Inicializar el mapa
        this.currentInventoryLang = plugin.getConfig().getString("config.inventory-language", "en");
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Tarea periódica para actualizar inventarios
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateInventories, 0L, 40L); // Cada 2 segundos (40 ticks)
    }

    // Tarea periódica para actualizar inventarios
    private void updateInventories() {
        checkConfigChange();
        for (Player player : updatingPlayers) {
            if (player.isOnline()) {
                updateBankInventory(player);
            }
        }
    }


    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    // Creacion del inventario
    public void openBankInventory(Player player) {
        bankInventoryManager.reloadInventory();
        FileConfiguration enConfig = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        bankInventory = createInventory(enConfig);
        loadItemsIntoInventory(enConfig, player);
        fillEmptySlots(enConfig);
        player.openInventory(bankInventory);
        playerInventories.put(player, bankInventory);
        playerInventoryNames.put(player, enConfig.getString("bank-inventory.main.name"));
        updatingPlayers.add(player);
    }
    private Inventory createInventory(FileConfiguration enConfig) {
        int size = enConfig.getInt("bank-inventory.main.size", 6);
        String inventoryName = enConfig.getString("bank-inventory.main.name");
        return Bukkit.createInventory(null, size * 9, MessageUtils.getColoredMessage(inventoryName));
    }
    private void loadItemsIntoInventory(FileConfiguration enConfig, Player player) {
        ConfigurationSection positionSlots = enConfig.getConfigurationSection("bank-inventory.main.position-slot");
        if (positionSlots != null) {
            for (String slotKey : positionSlots.getKeys(false)) {
                if (!"default".equalsIgnoreCase(slotKey)) {
                    int slot = Integer.parseInt(slotKey);
                    ConfigurationSection itemData = positionSlots.getConfigurationSection(slotKey);
                    if (itemData != null) {
                        ItemStack item = createItem(itemData, player);
                        bankInventory.setItem(slot, item);
                    }
                }
            }
        }
    }
    private void fillEmptySlots(FileConfiguration enConfig) {
        Map<String, Object> defaultItemConfig = enConfig.getConfigurationSection("bank-inventory.main.position-slot.default").getValues(false);
        ItemStack defaultItem = createDefaultItem(defaultItemConfig);
        for (int i = 0; i < bankInventory.getSize(); i++) {
            if (bankInventory.getItem(i) == null) {
                bankInventory.setItem(i, defaultItem);
            }
        }
    }
    private ItemStack createItem(ConfigurationSection itemData, Player player) {
        String materialName = itemData.getString("item");
        Material material = Material.getMaterial(materialName.toUpperCase());
        int amount = itemData.getInt("amount");
        String name = MessageUtils.getColoredMessage(itemData.getString("name"));

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = itemData.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.setLore(replacePlaceholders(lore, player));
            }
            item.setItemMeta(meta);
        }
        addHeadTexture(material, itemData, item);
        return item;
    }
    private ItemStack createDefaultItem(Map<String, Object> defaultItemConfig) {
        String defaultMaterialName = (String) defaultItemConfig.get("item");
        Material defaultMaterial = Material.getMaterial(defaultMaterialName.toUpperCase());
        int defaultAmount = (int) defaultItemConfig.get("amount");
        String defaultName = MessageUtils.getColoredMessage((String) defaultItemConfig.get("name"));

        ItemStack defaultItem = new ItemStack(defaultMaterial, defaultAmount);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        if (defaultMeta != null) {
            defaultMeta.setDisplayName(defaultName);
            defaultItem.setItemMeta(defaultMeta);
        }
        return defaultItem;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Cerrarel inventario
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();

        // Verificar si el inventario cerrado es el que está en el mapa de inventarios del jugador
        if (playerInventories.containsKey(player) && closedInventory.equals(playerInventories.get(player))) {
            updatingPlayers.remove(player); // Eliminar del conjunto de actualización
            playerInventories.remove(player); // Eliminar el inventario del jugador
            playerInventoryNames.remove(player); // Remover el nombre del inventario del jugador
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Al clicar en el inventario
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && playerInventories.containsKey(player) && clickedInventory.equals(playerInventories.get(player))) {
            handleInventoryClick(player, event);
        }
    }
    private void handleInventoryClick(Player player, InventoryClickEvent event) {
        checkConfigChange();
        FileConfiguration enConfig = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        String expectedInventoryName = enConfig.getString("bank-inventory.main.name");
        if (!event.getView().getTitle().equals(MessageUtils.getColoredMessage(expectedInventoryName))) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (!player.hasMetadata("bank_click_cooldown")) {
            player.setMetadata("bank_click_cooldown", new FixedMetadataValue(plugin, true));
            int slot = event.getSlot();
            ConfigurationSection positionSlots = enConfig.getConfigurationSection("bank-inventory.main.position-slot");
            if (positionSlots != null && positionSlots.contains(String.valueOf(slot))) {
                ConfigurationSection itemData = positionSlots.getConfigurationSection(String.valueOf(slot));
                if (itemData != null) {
                    handleItemCommand(player, itemData);
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.removeMetadata("bank_click_cooldown", plugin), 5L);
        }
    }
    private void handleItemCommand(Player player, ConfigurationSection itemData) {
        String command = itemData.getString("command", "");
        String permission = itemData.getString("permission", ""); // Obtener el permiso de la configuración

        // Verificar si el jugador tiene el permiso
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            noPerm(player);
            return; // Salir del método si no tiene permiso
        }

        if (command != null && !command.isEmpty()) {
            if (command.contains("<amount>")) {
                boolean isWithdraw = command.contains("bank take");
                startConversation(player, isWithdraw);
            } else {
                Bukkit.dispatchCommand(player, command);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Para actualizar el inventario en directo (variables, texturas)
    private void updateBankInventory(Player player) {
        if (!playerInventories.containsKey(player)) {
            return;
        }
        Inventory inventory = playerInventories.get(player);
        if (inventory == null) {
            return;
        }
        FileConfiguration enConfig = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        String newInventoryName = enConfig.getString("bank-inventory.main.name");
        if (!newInventoryName.equals(playerInventoryNames.get(player))) {
            player.closeInventory();
            openBankInventory(player);
            return;
        }
        loadItemsIntoInventory(enConfig, player);
        fillEmptySlots(enConfig);
        player.updateInventory();
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Comprobar si se cambia el lenguaje del inventario
    private void checkConfigChange() {
        // Recargar el archivo de configuración
        plugin.reloadConfig(); // Esto recarga el archivo de configuración del plugin

        // Obtener el nuevo idioma de la configuración
        FileConfiguration config = plugin.getConfig();
        String newInventoryLang = config.getString("config.inventory-language", "en");

        // Verificar si el idioma ha cambiado
        if (!newInventoryLang.equals(currentInventoryLang)) {
            currentInventoryLang = newInventoryLang; // Actualizar el idioma actual

            // Actualizar los inventarios abiertos
            for (Player player : playerInventories.keySet()) {
                if (player.isOnline()) {
                    player.closeInventory(); // Cerrar el inventario actual
                    openBankInventory(player); // Reabrir con la nueva configuración
                }
            }
        }
    }

    // Para asignar textura a una cabeza
    private void setPlayerHeadTextureValue(ItemStack head, String texture) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (head.getType() != Material.PLAYER_HEAD || skullMeta == null) {
            return;
        }

        // Usar GameProfile para modificar el perfil de textura
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", texture));
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        head.setItemMeta(skullMeta);
    }
    private void setPlayerHeadTextureURL(ItemStack head, String url) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (head.getType() != Material.PLAYER_HEAD || skullMeta == null) {
            return;
        }

        try {
            // Crear un nuevo perfil de jugador
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            // Asignar la URL de la skin en Base64 (codificado)
            textures.setSkin(new URL(url));

            // Asociar el perfil modificado con la cabeza de jugador
            skullMeta.setOwnerProfile(profile);
            head.setItemMeta(skullMeta);

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(MineBank.prefix +"&4[ERROR] &cInvalid URL in PLAYER_HEAD check it in bankInventory folder."));
            //e.printStackTrace();
        }
    }
    // Para establecer textura a una cabeza dependiendo de la version
    private void addHeadTexture (Material material, ConfigurationSection itemData, ItemStack item){
        // Detectar la versión de Minecraft
        String version = Bukkit.getVersion();
        boolean beforeVersion = version.contains("1.20.3") || version.contains("1.20.4") || version.contains("1.20.5") || version.contains("1.20.6") || version.contains("1.21");
        if (!beforeVersion) {
            if (material == Material.PLAYER_HEAD) {
                String texture = itemData.getString("texture");
                if (texture != null && !texture.isEmpty()) {
                    setPlayerHeadTextureValue(item, texture);
                }
            }
        } else {
            if (material == Material.PLAYER_HEAD) {
                String texture = itemData.getString("texture");
                if (texture != null && !texture.isEmpty()) {
                    setPlayerHeadTextureURL(item, texture);
                }
            }
        }
    }

    // Para reemplazar <amount> de un comando
    private void startConversation(Player player, boolean isWithdraw) {
        player.closeInventory(); // Cerrar el inventario

        // Verificar si ya hay una conversación activa
        if (player.hasMetadata("conversation_active")) {
            player.sendMessage(ChatColor.RED + "Ya tienes una conversación activa.");
            return;
        }

        // Marcar al jugador como que tiene una conversación activa
        player.setMetadata("conversation_active", new FixedMetadataValue(plugin, true));

        ConversationFactory conversationFactory = new ConversationFactory(plugin)
                .withFirstPrompt(new AmountPrompt(isWithdraw))
                .withLocalEcho(false);

        Conversation conversation = conversationFactory.buildConversation(player);
        conversation.addConversationAbandonedListener(new ConversationAbandonedListener() {
            @Override
            public void conversationAbandoned(ConversationAbandonedEvent event) {
                Player p = (Player) event.getContext().getForWhom();
                p.removeMetadata("conversation_active", plugin);
                p.removeMetadata("bank_click_cooldown", plugin);

                if (event.gracefulExit()) {
                    String amount = (String) event.getContext().getSessionData("amount");
                    if (amount != null && amount.matches("\\d+")) {
                        String command = isWithdraw ? "bank take " + amount : "bank add " + amount;
                        p.performCommand(command);
                    } else if (amount != null && amount.equalsIgnoreCase("exit")) {
                        return;
                    } else {
                        p.sendMessage(ChatColor.RED + "Cantidad inválida. Intenta de nuevo.");
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    openBankInventory(p);
                    updateBankInventory(p); // Actualiza el inventario del jugador
                });
            }
        });

        conversation.begin();
    }
    private static class AmountPrompt extends StringPrompt {

        private final boolean isWithdraw;

        public AmountPrompt(boolean isWithdraw) {
            this.isWithdraw = isWithdraw;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            Player player = (Player) context.getForWhom();
            return ChatColor.GREEN + "Introduce la cantidad a " + (isWithdraw ? "retirar" : "añadir") + " o escribe 'exit' para cancelar:";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase("exit")) {
                player.sendMessage(ChatColor.RED + "Operación cancelada.");
                return Prompt.END_OF_CONVERSATION;
            }

            if (input.matches("\\d+")) {
                context.setSessionData("amount", input); // Guardar la entrada en el contexto
                player.sendMessage(ChatColor.GREEN + "Cantidad recibida: " + input);
                return Prompt.END_OF_CONVERSATION;
            } else {
                player.sendMessage(ChatColor.RED + "Cantidad inválida. Intenta de nuevo.");
                return this; // Volver a solicitar la entrada
            }
        }
    }

    // Para reemplazar variables %xx%
    private List<String> replacePlaceholders(List<String> lore, Player player) {
        // Aquí puedes obtener los valores necesarios, por ejemplo, desde el Economy o desde una configuración.
        double playerBalance = economy.getBalance(player); // Balance del jugador en el economy
        int bankBalance = getBankBalance(player); // Implementa este método para obtener el balance del banco del jugador
        int maxStorage = getMaxStorageAmount(player); // Implementa este método para obtener el almacenamiento máximo del banco
        int level = getBankLevel(player); // Implementa este método para obtener el nivel del banco
        int maxLevel = getMaxBankLevel(); // Implementa este método para obtener el nivel máximo del banco

        // Puedes obtener estos valores desde configuraciones si son estáticos
        int nextLevelBalance = getNextLevelBalance(player); // Implementa este método para obtener el balance para el siguiente nivel del banco
        int playerTop = getPlayerTop(player); // Implementa este método para obtener el puesto del jugador en el top
        String playerName = player.getName(); // Nombre del jugador

        List<String> updatedLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("%playerBalance%", String.valueOf((int) playerBalance))
                    .replace("%bankBalance%", String.valueOf(bankBalance))
                    .replace("%maxStorage%", String.valueOf(maxStorage))
                    .replace("%level%", String.valueOf(level))
                    .replace("%maxLevel%", String.valueOf(maxLevel))
                    .replace("%nextLevelBalance%", String.valueOf(nextLevelBalance))
                    .replace("%playerTop%", String.valueOf(playerTop))
                    .replace("%playerName%", playerName);

            // Reemplazar placeholders dinámicos como %playerName<1>%
            line = replaceTopPlaceholders(MessageUtils.getColoredMessage(line));

            updatedLore.add(MessageUtils.getColoredMessage(line));
        }
        return updatedLore;
    }
    // Para reemplazar variables %xx<1>%
    private String replaceTopPlaceholders(String line) {
        FileConfiguration bankConfig = bankManager.getBank();
        Map<String, Integer> topBalances = new HashMap<>();

        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                topBalances.put(playerName, balance);
            }
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        for (int i = 1; i <= 10; i++) { // Se asume que quieres reemplazar hasta 3 tops
            String playerNamePlaceholder = "%playerName<" + i + ">%";
            String bankBalancePlaceholder = "%bankBalance<" + i + ">%";

            if (line.contains(playerNamePlaceholder) || line.contains(bankBalancePlaceholder)) {
                if (i <= sortedList.size()) {
                    Map.Entry<String, Integer> entry = sortedList.get(i - 1);
                    String topPlayerName = entry.getKey();
                    int topBankBalance = entry.getValue();

                    line = line.replace(playerNamePlaceholder, topPlayerName)
                            .replace(bankBalancePlaceholder, String.valueOf(topBankBalance));
                } else {
                    // Si el ranking solicitado está fuera del rango, reemplaza con vacio o mensaje por defecto
                    line = line.replace(playerNamePlaceholder, "N/A")
                            .replace(bankBalancePlaceholder, "0");
                }
            }
        }

        return line;
    }

    // Para reemplazar variables %xx%
    private int getMaxStorageAmount(int level) {
        String levelData = plugin.getConfig().getString("bank.level." + level);

        if (levelData != null && levelData.contains(";")) {
            String[] parts = levelData.split(";");
            if (parts.length > 0) {
                try {
                    return Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
    private int getMaxStorageAmount(Player player) {
        int level = getBankLevel(player);
        return getMaxStorageAmount(level);
    }
    private int getNextLevelBalance(int level) {
        String levelData = plugin.getConfig().getString("bank.level." + level);

        if (levelData != null && levelData.contains(";")) {
            String[] parts = levelData.split(";");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
    private int getNextLevelBalance(Player player) {
        int level = getBankLevel(player);
        return getNextLevelBalance(level);
    }
    private int getMaxBankLevel() {
        int maxLevel = 1;
        Set<String> levels = plugin.getConfig().getConfigurationSection("bank.level").getKeys(false);
        for (String level : levels) {
            try {
                int levelNumber = Integer.parseInt(level);
                if (levelNumber > maxLevel) {
                    maxLevel = levelNumber;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return maxLevel;
    }
    private int getBankBalance(Player player) {
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        return bankManager.getBank().getInt(playerPath + ".balance", 0);
    }
    private int getBankLevel(Player player) {
        String playerPath = "bank." + player.getUniqueId() + "." + player.getName();
        return bankManager.getBank().getInt(playerPath + ".level", 1);
    }
    private int getPlayerTop(Player player) {
        FileConfiguration bankConfig = bankManager.getBank();

        Map<String, Integer> topBalances = new HashMap<>();

        for (String uuid : bankConfig.getConfigurationSection("bank").getKeys(false)) {
            for (String playerName : bankConfig.getConfigurationSection("bank." + uuid).getKeys(false)) {
                int balance = bankConfig.getInt("bank." + uuid + "." + playerName + ".balance");
                topBalances.put(playerName, balance);
            }
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(topBalances.entrySet());
        sortedList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getKey().equalsIgnoreCase(player.getName())) {
                return i + 1;
            }
        }
        return -1; // Si no está en el top
    }

    private void noPerm(CommandSender sender){
        String mensaje = languageManager.getMessage("messages.no-perm");
        if (mensaje != null) {
            mensaje = mensaje.replaceAll("%plugin%", MineBank.prefix);
            sender.sendMessage(MessageUtils.getColoredMessage(mensaje));
        }
    }
}