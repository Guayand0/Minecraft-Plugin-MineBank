package com.Guayand0.events;

import com.Guayand0.MineBank;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.InventoryUtils;
import com.Guayand0.utils.MessageUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final BankUtils BU = new BankUtils();
    private final InventoryUtils IU = new InventoryUtils();

    private Inventory bankInventory;
    private final Map<Player, Inventory> playerInventories; // Mapa para guardar el inventario abierto de cada jugador
    private final Set<Player> updatingPlayers; // Conjunto para jugadores a los que se debe actualizar el inventario
    private final Map<Player, String> playerInventoryNames; // Mapa para guardar el nombre del inventario abierto de cada jugador

    public BankInventoryEvent(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.playerInventories = new HashMap<>();
        this.updatingPlayers = new HashSet<>();
        this.playerInventoryNames = new HashMap<>(); // Inicializar el mapa
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Tarea periódica para actualizar inventarios
        int interval = BU.getUpdateGUITicks(plugin); // Cantidad de ticks para actualizar inventario
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateInventories, interval, interval);
    }

    // Tarea periódica para actualizar inventarios
    public void updateInventories() {
        for (Player player : updatingPlayers) if (player.isOnline()) updateBankInventory(player);
    }

    // -------------------------------------------------------------------- //

    // Creacion del inventario
    public void openBankInventory(Player player) {
        languageManager.reloadGui();
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
        bankInventory = createInventory(languageInventoryManager, player);
        loadItemsIntoInventory(languageInventoryManager, player);
        fillEmptySlots(languageInventoryManager, player);
        player.openInventory(bankInventory);
        playerInventories.put(player, bankInventory);
        String inventoryName = languageInventoryManager.getString("gui.main.name");
        inventoryName = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, inventoryName, plugin.placeholders);
        playerInventoryNames.put(player, inventoryName);
        updatingPlayers.add(player);
    }

    private Inventory createInventory(FileConfiguration languageInventoryManager, Player player) {
        int size = languageInventoryManager.getInt("gui.main.size", 6);
        String inventoryName = languageInventoryManager.getString("gui.main.name");
        return Bukkit.createInventory(null, size * 9, MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, inventoryName, plugin.placeholders));
    }

    private void loadItemsIntoInventory(FileConfiguration languageInventoryManager, Player player) {
        ConfigurationSection positionSlots = languageInventoryManager.getConfigurationSection("gui.main.position-slot");
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

    private ItemStack createItem(ConfigurationSection itemData, Player player) {
        String materialName = itemData.getString("item");
        Material material = Material.getMaterial(materialName.toUpperCase());
        int amount = itemData.getInt("amount");
        String name = itemData.getString("name");

        // Obtener placeholders específicos para este jugador
        Map<String, String> playerPlaceholders = plugin.playerPlaceholders.getOrDefault(player.getName(), new HashMap<>());

        name = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, name, playerPlaceholders);

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            List<String> lore = itemData.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) processedLore.add(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, line, playerPlaceholders));
                meta.setLore(processedLore);
            }

            item.setItemMeta(meta);
        }

        addHeadTexture(material, itemData, item);
        return item;
    }


    private void fillEmptySlots(FileConfiguration languageInventoryManager, Player player) {
        Map<String, Object> defaultItemConfig = languageInventoryManager.getConfigurationSection("gui.main.position-slot.default").getValues(false);
        ItemStack defaultItem = createDefaultItem(player, defaultItemConfig);
        for (int i = 0; i < bankInventory.getSize(); i++) if (bankInventory.getItem(i) == null) bankInventory.setItem(i, defaultItem);
    }

    private ItemStack createDefaultItem(Player player, Map<String, Object> defaultItemConfig) {
        String defaultMaterialName = (String) defaultItemConfig.get("item");
        Material defaultMaterial = Material.getMaterial(defaultMaterialName.toUpperCase());
        int defaultAmount = (int) defaultItemConfig.get("amount");
        String defaultName = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, (String) defaultItemConfig.get("name"), plugin.placeholders);

        ItemStack defaultItem = new ItemStack(defaultMaterial, defaultAmount);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        if (defaultMeta != null) {
            defaultMeta.setDisplayName(defaultName);
            defaultItem.setItemMeta(defaultMeta);
        }
        return defaultItem;
    }

    // -------------------------------------------------------------------- //

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

    // -------------------------------------------------------------------- //

    // Al clicar en el inventario
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && playerInventories.containsKey(player) && clickedInventory.equals(playerInventories.get(player))) handleInventoryClick(player, event);
    }

    private void handleInventoryClick(Player player, InventoryClickEvent event) {
        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
        String expectedInventoryName = languageInventoryManager.getString("gui.main.name");
        if (!event.getView().getTitle().equals(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, expectedInventoryName, plugin.placeholders))) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (!player.hasMetadata("bank_click_cooldown")) {
            player.setMetadata("bank_click_cooldown", new FixedMetadataValue(plugin, true));
            int slot = event.getSlot();
            ConfigurationSection positionSlots = languageInventoryManager.getConfigurationSection("gui.main.position-slot");
            if (positionSlots != null && positionSlots.contains(String.valueOf(slot))) {
                ConfigurationSection itemData = positionSlots.getConfigurationSection(String.valueOf(slot));
                if (itemData != null) handleItemCommand(player, itemData);
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
            return; // Salir del metodo si no tiene permiso
        }

        if (command != null && !command.isEmpty()) {
            if (command.contains("<amount>")) {
                boolean isWithdraw = command.contains("bank take");
                startConversation(player, isWithdraw);
            } else Bukkit.dispatchCommand(player, command);

        }
    }

    // -------------------------------------------------------------------- //

    // Para actualizar el inventario en directo (variables, texturas)
    private void updateBankInventory(Player player) {
        if (!playerInventories.containsKey(player)) return;

        Inventory inventory = playerInventories.get(player);
        if (inventory == null) return;

        FileConfiguration languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
        String newInventoryName = languageInventoryManager.getString("gui.main.name");
        newInventoryName = MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, newInventoryName, plugin.placeholders);
        if (!newInventoryName.equals(playerInventoryNames.get(player))) {
            player.closeInventory();
            openBankInventory(player);
            return;
        }

        loadItemsIntoInventory(languageInventoryManager, player);
        fillEmptySlots(languageInventoryManager, player);
        player.updateInventory();
    }

    // -------------------------------------------------------------------- //

    // Para establecer textura a una cabeza dependiendo de la version
    private void addHeadTexture (Material material, ConfigurationSection itemData, ItemStack item){
        // Detectar la versión de Minecraft
        String version = Bukkit.getVersion();
        boolean recentVersions = version.contains("1.20.4") || version.contains("1.20.5") || version.contains("1.20.6") || version.contains("1.21");
        if (recentVersions) {
            if (material == Material.PLAYER_HEAD) {
                String texture = itemData.getString("texture");
                if (texture != null && !texture.isEmpty()) setPlayerHeadTextureURL(item, texture);
            }

        } else {
            if (material == Material.PLAYER_HEAD) {
                String texture = itemData.getString("texture");
                if (texture != null && !texture.isEmpty()) setPlayerHeadTextureValue(item, texture);
            }
        }
    }

    // Establecer textura de cabezas en versiones recientes
    private void setPlayerHeadTextureURL(ItemStack head, String url) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        try {
            // Crear un nuevo perfil de jugador
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            // Asignar la URL de la skin en Base64 (codificado)
            textures.setSkin(new URL(url));

            // Asociar el perfil modificado con la cabeza de jugador
            assert skullMeta != null;
            skullMeta.setOwnerProfile(profile);
            head.setItemMeta(skullMeta);

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &4[ERROR] &cInvalid URL in PLAYER_HEAD check it in gui folder.", plugin.placeholders));
        }
    }

    // Para asignar textura a una cabeza
    private void setPlayerHeadTextureValue(ItemStack head, String texture) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        try {
            // Usar GameProfile para modificar el perfil de textura
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", texture));
            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            }
            head.setItemMeta(skullMeta);
        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &4[ERROR] &cInvalid VALUE in PLAYER_HEAD check it in gui folder.", plugin.placeholders));
        }
    }

    // -------------------------------------------------------------------- //

    // Para reemplazar <amount> de un comando
    private void startConversation(Player player, boolean isWithdraw) {
        player.closeInventory(); // Cerrar el inventario

        // Verificar si ya hay una conversación activa
        if (player.hasMetadata("conversation_active")) player.removeMetadata("conversation_active", plugin);

        // Marcar al jugador como que tiene una conversación activa
        player.setMetadata("conversation_active", new FixedMetadataValue(plugin, true));

        ConversationFactory conversationFactory = new ConversationFactory(plugin).withFirstPrompt(new AmountPrompt(isWithdraw, plugin)).withLocalEcho(false);

        Conversation conversation = conversationFactory.buildConversation(player);
        conversation.addConversationAbandonedListener(event -> {
            Player player1 = (Player) event.getContext().getForWhom();
            player1.removeMetadata("conversation_active", plugin);
            player1.removeMetadata("bank_click_cooldown", plugin);

            if (event.gracefulExit()) {
                String amount = (String) event.getContext().getSessionData("amount");
                if (amount != null && amount.matches("\\d+")) {
                    String command = isWithdraw ? "bank take " + amount : "bank add " + amount;
                    player1.performCommand(command);
                } else if (amount != null && amount.equalsIgnoreCase("exit")) {
                    for (String message : plugin.getLanguageManager().getAllMessage("bank.gui.transaction-canceled")) {
                        player1.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player1, message, plugin.placeholders));
                    }
                    return;
                } else {
                    for (String message : plugin.getLanguageManager().getAllMessage("bank.gui.invalid-amount")) {
                        player1.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player1, message, plugin.placeholders));
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                openBankInventory(player1);
                updateBankInventory(player1); // Actualiza el inventario del jugador
            });
        });

        conversation.begin();
    }

    private static class AmountPrompt extends StringPrompt {

        private final boolean isWithdraw;
        private final MineBank plugin;
        private final MessageUtils MU = new MessageUtils();

        public AmountPrompt(boolean isWithdraw, MineBank plugin) {
            this.isWithdraw = isWithdraw;
            this.plugin = plugin;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            Player player = (Player) context.getForWhom();

            if (isWithdraw) {
                for (String message : plugin.getLanguageManager().getAllMessage("bank.gui.amount-withdraw")) {
                    return MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders);
                }
            } else {
                for (String message : plugin.getLanguageManager().getAllMessage("bank.gui.amount-deposit")) {
                    return MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders);
                }
            }
            return "";
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
                for (String message : plugin.getLanguageManager().getAllMessage("bank.gui.amount-selected")) player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message + " " + input, plugin.placeholders));
                return Prompt.END_OF_CONVERSATION;
            } else {
                for (String message : plugin.getLanguageManager().getAllMessage("bank.gio.invalid-amount")) player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
                return this; // Volver a solicitar la entrada
            }
        }
    }

    // -------------------------------------------------------------------- //

    private void noPerm(Player player){
        for (String message : languageManager.getAllMessage("messages.no-perm")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }
}