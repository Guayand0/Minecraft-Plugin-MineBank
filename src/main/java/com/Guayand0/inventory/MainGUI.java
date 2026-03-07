package com.Guayand0.inventory;

import com.Guayand0.utils.GuiHolder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.Guayand0.MineBank;
import com.Guayand0.zlib.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class MainGUI {

    private final MineBank plugin;

    private static final Map<String, Enchantment> ENCHANT_CACHE = new HashMap<>();

    // Guardamos jugadores con gui abierto
    public static Set<UUID> openedPlayersGUI = new HashSet<>();

    private final MessageUtils MU = new MessageUtils();
    private final InventoryUtils IU = new InventoryUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    private FileConfiguration languageInventoryManager;

    public MainGUI(MineBank plugin) {
        this.plugin = plugin;
    }

    private Inventory createInventory(String guiId) {
        String title = getGuiTitle(guiId);
        int size = getGuiSize(guiId);
        return Bukkit.createInventory(new GuiHolder(guiId), size, title);
    }

    public void openInventory(Player player, String guiId) {
        loadGuiConfig();
        Inventory inventory = createInventory(guiId); // Cada GUI tiene su inventario local

        ConfigurationSection slots = languageInventoryManager.getConfigurationSection("gui." + guiId + ".position-slot");
        if (slots != null) {

            // 1. Cargar slots numéricos
            for (String key : slots.getKeys(false)) {
                if (!key.equalsIgnoreCase("default")) {
                    setSlottedItems(player, inventory, "gui." + guiId + ".position-slot", key);
                }
            }

            // 2. Cargar slots default
            setDefaultItems(player, inventory, guiId);
        }

        // 3. Abrir GUI
        player.openInventory(inventory);
        openedPlayersGUI.add(player.getUniqueId());
    }

    public void updateInventory(Player player, String guiId) {
        loadGuiConfig();
        if (player == null || player.getOpenInventory() == null) return;

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null) return;

        ConfigurationSection slots = languageInventoryManager.getConfigurationSection("gui." + guiId + ".position-slot");
        if (slots == null) return;

        Map<Integer, ItemStack> itemsToUpdate = new HashMap<>();

        // 1. Actualizar solo los slots definidos explícitamente
        for (String key : slots.getKeys(false)) {
            if (key.equalsIgnoreCase("default")) continue;

            int slotNumber;
            try {
                slotNumber = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            if (slotNumber < 0 || slotNumber >= inv.getSize()) continue;

            ItemStack updatedItem = createItem("gui." + guiId + ".position-slot." + key, plugin.buildPlayerPlaceholders(player.getUniqueId()));
            if (updatedItem != null) {
                itemsToUpdate.put(slotNumber, updatedItem);
            }
        }

        // Aplicar actualizaciones
        for (Map.Entry<Integer, ItemStack> entry : itemsToUpdate.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue());
        }

        // 2. Rellenar huecos vacíos con el default
        setDefaultItems(player, inv, guiId);
    }

    private void setDefaultItems(Player player, Inventory inventory, String guiId) {
        ItemStack def = createItem("gui." + guiId + ".position-slot.default", plugin.buildPlayerPlaceholders(player.getUniqueId()));
        if (def != null) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    inventory.setItem(i, def);
                }
            }
        }
    }

    private ItemStack createItem(String route, Map<String,String> ph) {

        ConfigurationSection section = languageInventoryManager.getConfigurationSection(route);
        if (section == null) return null;

        String materialName = section.getString("item");
        if (materialName == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }

        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // ---- NAME ----
        String name = section.getString("name");
        if (name != null) {
            meta.setDisplayName(MU.getColoredReplacePluginPlaceholdersText(name, ph));
        }

        // ---- LORE ----
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> finalLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                finalLore.add(MU.getColoredReplacePluginPlaceholdersText(line, ph));
            }
            meta.setLore(finalLore);
        }

        // ---- ENCHANTS ----
        List<String> enchants = section.getStringList("enchant");
        if (!enchants.isEmpty()) {

            for (String raw : enchants) {

                String[] parts = raw.split(":", 2);
                if (parts.length != 2) continue;

                String enchantName = parts[0].trim();
                String levelRaw = parts[1].trim();

                int level;
                try {
                    level = Integer.parseInt(levelRaw);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (level <= 0) continue;

                Enchantment enchant = resolveEnchantment(enchantName);
                if (enchant == null) continue;

                meta.addEnchant(enchant, level, true);
            }

            boolean hideEnchant = section.getBoolean("hide-enchant", false);
            if (hideEnchant) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        // aplicar meta antes de texturas
        item.setItemMeta(meta);

        // ---- HEAD TEXTURE ----
        addHeadTexture(material, section, item);

        return item;
    }

    private Enchantment resolveEnchantment(String rawName) {

        if (rawName == null) return null;

        String normalized = rawName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;

        // ⚡ buscar en cache
        Enchantment cached = ENCHANT_CACHE.get(normalized);
        if (cached != null) return cached;

        Enchantment enchant = null;

        // intentar por nombre Bukkit
        enchant = Enchantment.getByName(normalized.toUpperCase(Locale.ROOT));

        // intentar por key namespaced
        if (enchant == null) {
            enchant = Enchantment.getByKey(NamespacedKey.minecraft(normalized));
        }

        // guardar resultado (aunque sea null evitamos repetir búsqueda)
        ENCHANT_CACHE.put(normalized, enchant);

        return enchant;
    }

    private void setSlottedItems(Player player, Inventory inventory, String path, String slot) {
        String route = path + "." + slot;

        // Caso SLOT NUMÉRICO
        int slotNumber;
        try {
            slotNumber = Integer.parseInt(slot);
        } catch (NumberFormatException e) {
            return; // si no es número, ignoramos
        }

        if (slotNumber < 0 || slotNumber >= inventory.getSize()) return;

        ItemStack item = createItem(route, plugin.buildPlayerPlaceholders(player.getUniqueId()));
        if (item == null) return;

        inventory.setItem(slotNumber, item);
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
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &4[ERROR] &cInvalid URL in PLAYER_HEAD check it in gui folder", plugin.placeholders));
        }
    }

    // Para asignar textura a una cabeza
    private void setPlayerHeadTextureValue(ItemStack head, String texture) {
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        try {
            // Usar GameProfile para modificar el perfil de textura
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");

            // Crear un Set con la propiedad "textures"
            Set<Property> textures = new HashSet<>();
            textures.add(new Property("textures", texture));
            //profile.getProperties().put("textures", new Property("textures", texture));

            try {
                // Asignar el Set directamente al GameProfile
                Field propertiesField = GameProfile.class.getDeclaredField("properties");
                propertiesField.setAccessible(true);
                propertiesField.set(profile, textures);

                // Asignar el perfil al SkullMeta usando reflexión
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }

            head.setItemMeta(skullMeta);

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredReplacePluginPlaceholdersText("%plugin% &4[ERROR] &cInvalid VALUE in PLAYER_HEAD check it in gui folder", plugin.placeholders));
        }
    }

    public void reloadAllOpenInventories() {

        // 🧹 Limpiar jugadores offline o inválidos
        openedPlayersGUI.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        // Recorrer todos los jugadores con GUI abierta
        for (UUID uuid : openedPlayersGUI) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getOpenInventory() == null) continue;

            InventoryView view = player.getOpenInventory();
            if (!(view.getTopInventory().getHolder() instanceof GuiHolder)) continue;

            GuiHolder holder = (GuiHolder) view.getTopInventory().getHolder();
            String guiId = holder.getGuiId();

            // 1️⃣ Crear un nuevo inventario con el título actualizado
            Inventory newInv = createInventory(guiId);

            // 2️⃣ Llenar los items de slots numéricos y default
            ConfigurationSection slots = languageInventoryManager.getConfigurationSection("gui." + guiId + ".position-slot");
            if (slots != null) {
                for (String key : slots.getKeys(false)) {
                    if (!key.equalsIgnoreCase("default")) {
                        setSlottedItems(player, newInv, "gui." + guiId + ".position-slot", key);
                    }
                }
                setDefaultItems(player, newInv, guiId);
            }

            // ⚠️ Cerrar el inventario actual antes de abrir el nuevo
            player.closeInventory();

            // 3️⃣ Abrir el inventario actualizado al jugador
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.openInventory(newInv);
                openedPlayersGUI.add(player.getUniqueId());
            }, 1L);
        }
    }

    // -------------------------------------------------------------------- //

    public boolean guiExists(String guiId) {
        loadGuiConfig();
        ConfigurationSection section = languageInventoryManager.getConfigurationSection("gui");
        return section != null && section.contains(guiId);
    }

    public String getGuiTitle(String guiId) {
        loadGuiConfig();
        String path = "gui." + guiId + ".name";
        String name = languageInventoryManager.getString(path, guiId);
        return MU.getColoredReplacePluginPlaceholdersText(name, plugin.placeholders);
    }

    public int getGuiSize(String guiId) {
        loadGuiConfig();
        String path = "gui." + guiId + ".size";
        return languageInventoryManager.getInt(path, 6) * 9;
    }

    private void loadGuiConfig() {
        if (languageInventoryManager == null) {
            languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
        }
    }

    public void reloadGuiConfig() {
        languageInventoryManager = IU.getGuiConfig(plugin, "gui/" + IU.getGuiLangFile(plugin));
    }

    // -------------------------------------------------------------------- //

}
