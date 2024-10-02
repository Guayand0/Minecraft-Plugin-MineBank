package mb.Guayando.events;

//import com.comphenix.protocol.ProtocolManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mb.Guayando.MineBank;
import mb.Guayando.managers.*;
import mb.Guayando.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private final BankInventoryManager bankInventoryManager;
    //private final ProtocolManager protocolManager;
    private Inventory bankInventory;
    private final Map<Player, Inventory> playerInventories; // Mapa para guardar el inventario abierto de cada jugador
    private final Set<Player> updatingPlayers; // Conjunto para jugadores a los que se debe actualizar el inventario
    private final Map<Player, String> playerInventoryNames; // Mapa para guardar el nombre del inventario abierto de cada jugador
    private String currentInventoryLang;

    public BankInventoryEvent(MineBank plugin) {
        this.plugin = plugin;
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

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Creacion del inventario
    public void openBankInventory(Player player) {
        bankInventoryManager.reloadInventory();
        FileConfiguration languageInventoryManager = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        bankInventory = createInventory(languageInventoryManager, player);
        loadItemsIntoInventory(languageInventoryManager, player);
        fillEmptySlots(languageInventoryManager, player);
        player.openInventory(bankInventory);
        playerInventories.put(player, bankInventory);
        String inventoryName = languageInventoryManager.getString("bank-inventory.main.name");
        inventoryName = MessageUtils.applyPlaceholdersAndColor(player, null, inventoryName, plugin, 0, MineBank.getPlaceholderAPI());
        playerInventoryNames.put(player, inventoryName);
        updatingPlayers.add(player);
    }

    private Inventory createInventory(FileConfiguration languageInventoryManager, Player player) {
        int size = languageInventoryManager.getInt("bank-inventory.main.size", 6);
        String inventoryName = languageInventoryManager.getString("bank-inventory.main.name");
        return Bukkit.createInventory(null, size * 9, MessageUtils.applyPlaceholdersAndColor(player,null, inventoryName, plugin, 0, MineBank.getPlaceholderAPI()));
    }

    private void loadItemsIntoInventory(FileConfiguration languageInventoryManager, Player player) {
        ConfigurationSection positionSlots = languageInventoryManager.getConfigurationSection("bank-inventory.main.position-slot");
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

    private void fillEmptySlots(FileConfiguration languageInventoryManager, Player player) {
        Map<String, Object> defaultItemConfig = languageInventoryManager.getConfigurationSection("bank-inventory.main.position-slot.default").getValues(false);
        ItemStack defaultItem = createDefaultItem(player, defaultItemConfig);
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
        String name = itemData.getString("name");
        name = MessageUtils.applyPlaceholdersAndColor(player, null, name, plugin, 0, MineBank.getPlaceholderAPI());
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = itemData.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.setLore(MethodUtils.replacePlaceholders(lore, player, plugin));
            }
            item.setItemMeta(meta);
        }
        addHeadTexture(material, itemData, item);
        return item;
    }

    private ItemStack createDefaultItem(Player player, Map<String, Object> defaultItemConfig) {
        String defaultMaterialName = (String) defaultItemConfig.get("item");
        Material defaultMaterial = Material.getMaterial(defaultMaterialName.toUpperCase());
        int defaultAmount = (int) defaultItemConfig.get("amount");
        String defaultName = MessageUtils.applyPlaceholdersAndColor(player, null, (String) defaultItemConfig.get("name"), plugin, 0, MineBank.getPlaceholderAPI());

        ItemStack defaultItem = new ItemStack(defaultMaterial, defaultAmount);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        if (defaultMeta != null) {
            defaultMeta.setDisplayName(defaultName);
            defaultItem.setItemMeta(defaultMeta);
        }
        return defaultItem;
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

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

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

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
        FileConfiguration languageInventoryManager = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        String expectedInventoryName = languageInventoryManager.getString("bank-inventory.main.name");
        if (!event.getView().getTitle().equals(MessageUtils.applyPlaceholdersAndColor(player, null, expectedInventoryName, plugin, 0, MineBank.getPlaceholderAPI()))) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (!player.hasMetadata("bank_click_cooldown")) {
            player.setMetadata("bank_click_cooldown", new FixedMetadataValue(plugin, true));
            int slot = event.getSlot();
            ConfigurationSection positionSlots = languageInventoryManager.getConfigurationSection("bank-inventory.main.position-slot");
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

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    // Para actualizar el inventario en directo (variables, texturas)
    private void updateBankInventory(Player player) {
        if (!playerInventories.containsKey(player)) {
            return;
        }
        Inventory inventory = playerInventories.get(player);
        if (inventory == null) {
            return;
        }
        FileConfiguration languageInventoryManager = bankInventoryManager.getCustomConfig("bankInventory/" + bankInventoryManager.getBankInventoryFile());
        String newInventoryName = languageInventoryManager.getString("bank-inventory.main.name");
        newInventoryName = MessageUtils.applyPlaceholdersAndColor(player, null, newInventoryName, plugin, 0, MineBank.getPlaceholderAPI());
        if (!newInventoryName.equals(playerInventoryNames.get(player))) {
            player.closeInventory();
            openBankInventory(player);
            return;
        }
        loadItemsIntoInventory(languageInventoryManager, player);
        fillEmptySlots(languageInventoryManager, player);
        player.updateInventory();
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

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
            //String message = plugin.getLanguageManager().getMessage("bank.inventory.errorTextureHead");
            //Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(message));
            Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(MineBank.prefix +" &4[ERROR] &cInvalid URL in PLAYER_HEAD check it in bankInventory folder."));
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
            player.removeMetadata("conversation_active", plugin);
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
                Player player = (Player) event.getContext().getForWhom();
                player.removeMetadata("conversation_active", plugin);
                player.removeMetadata("bank_click_cooldown", plugin);

                if (event.gracefulExit()) {
                    String amount = (String) event.getContext().getSessionData("amount");
                    if (amount != null && amount.matches("\\d+")) {
                        String command = isWithdraw ? "bank take " + amount : "bank add " + amount;
                        player.performCommand(command);
                    } else if (amount != null && amount.equalsIgnoreCase("exit")) {
                        player.sendMessage(ChatColor.RED + "Transaction cancelled."); // Enviar el mensaje antes de terminar
                        return;
                    } else {
                        //String message = plugin.getLanguageManager().getMessage("bank.inventory.invalidAmount");
                        //player.sendMessage(MessageUtils.getColoredMessage(message));
                        player.sendMessage(ChatColor.RED + "Invalid amount. Try again.");
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    openBankInventory(player);
                    updateBankInventory(player); // Actualiza el inventario del jugador
                });
            }
        });

        conversation.begin();
    }

    private static class AmountPrompt extends StringPrompt {

        private final boolean isWithdraw;
        private MineBank plugin;

        public AmountPrompt(boolean isWithdraw) {
            this.isWithdraw = isWithdraw;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            Player player = (Player) context.getForWhom();

            if (isWithdraw) {
                //String message = plugin.getLanguageManager().getMessage("bank.inventory.amountWithdraw");
                //return MessageUtils.getColoredMessage(message);
                return ChatColor.GREEN + "Insert amount to withdraw or type 'exit' to cancel:";
            } else {
                //String message = plugin.getLanguageManager().getMessage("bank.inventory.amountDeposit");
                //return MessageUtils.getColoredMessage(message);
                return ChatColor.GREEN + "Insert amount to deposit or type 'exit' to cancel:";
            }
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            Player player = (Player) context.getForWhom();

            if (input.equalsIgnoreCase("exit")) {
                context.setSessionData("amount", "exit"); // Guardar 'exit' en el contexto
                return Prompt.END_OF_CONVERSATION; // Terminar la conversación
            }

            if (input.matches("\\d+")) {
                context.setSessionData("amount", input); // Guardar la cantidad en el contexto
                player.sendMessage(ChatColor.GREEN + "Amount selected: " + input);
                return Prompt.END_OF_CONVERSATION;
            } else {
                player.sendMessage(ChatColor.RED + "Invalid amount. Try again.");
                return this; // Volver a solicitar la entrada
            }
        }
    }

    private void noPerm(Player player){
        String messagePath = "messages.no-perm";
        MessageUtils.sendMessageWithPlaceholdersAndColor(player, null, messagePath, plugin, 0, MineBank.getPlaceholderAPI());
    }
}