package mb.Guayando.managers;

import mb.Guayando.MineBank;
import mb.Guayando.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Enumeration;

public class LanguageManager {

    private MineBank plugin;
    private FileConfiguration languageConfig;

    public LanguageManager(MineBank plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }

    private void loadLanguageFile() {
        // Directorio donde se guardarán los archivos de idioma
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Obtener la ubicación del archivo JAR del plugin
        File jarFile;
        try {
            jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return; // Salir si hay un problema con la URI
        }

        // Leer los archivos del JAR
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("lang/") && entry.getName().endsWith(".yml")) {
                    File outFile = new File(langDir, entry.getName().substring("lang/".length()));

                    if (!outFile.exists()) {
                        // Crear el archivo de destino y copiar el contenido del JAR
                        outFile.getParentFile().mkdirs();
                        try (InputStream in = jar.getInputStream(entry);
                             FileOutputStream out = new FileOutputStream(outFile);
                             BufferedInputStream bufferedIn = new BufferedInputStream(in)) {

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = bufferedIn.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cargar el archivo de idioma seleccionado en la configuración
        reloadConfig();
    }

    public void reloadLanguage() {
        loadLanguageFile();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        // Obtener el idioma seleccionado de la configuración
        String selectedLanguage = plugin.getConfig().getString("config.message-language", "en");
        // Cargar el archivo de idioma seleccionado
        File langDir = new File(plugin.getDataFolder(), "lang");
        File selectedLangFile = new File(langDir, selectedLanguage + ".yml");

        // Verificar si el archivo de idioma existe
        if (!selectedLangFile.exists()) {
            plugin.getLogger().warning(MessageUtils.getColoredMessage(MineBank.prefix + " &4Language " + selectedLanguage + " does not exist in lang folder."));
            plugin.getLogger().warning(MessageUtils.getColoredMessage(MineBank.prefix + " &4Using en.yml by default."));
            // Opcional: Establecer un archivo de idioma predeterminado si el archivo seleccionado no existe
            selectedLangFile = new File(langDir, "en.yml"); // Suponiendo que "en.yml" es tu archivo predeterminado
        }

        // Cargar la configuración del idioma
        languageConfig = YamlConfiguration.loadConfiguration(selectedLangFile);
    }

    public String getMessage(String path) {
        return languageConfig.getString(path, MessageUtils.getColoredMessage(MineBank.prefix + " &4Message &c" + path + " &4not found."));
    }

    public List<String> getStringList(String path) {
        List<String> messages = languageConfig.getStringList(path);
        if (messages.isEmpty()) {
            return Arrays.asList(MessageUtils.getColoredMessage(MineBank.prefix + " &4Message &c" + path + " &4not found."));
        }
        return messages;
    }
}