package com.Guayand0.zlib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExceptionManager {

    /**
     * Saves the details of an exception to a log file inside the plugin's data folder.
     * The log file is stored in the "exceptions" folder and named with the current timestamp.
     *
     * @param e The exception to be logged.
     * @param plugin The plugin instance.
     * @return A message indicating the success or failure of saving the exception.
     */
    public String saveInLog(Exception e, Plugin plugin) {

        // Formato SEGURO para nombre de archivo (Windows compatible)
        String fileDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // Formato legible para el contenido
        String readableDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        File logDirectory = new File(plugin.getDataFolder(), "exceptions");
        if (!logDirectory.exists()) logDirectory.mkdirs();

        File logFile = new File(logDirectory, fileDate + ".txt");
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
            logWriter.write("Date: " + readableDate + "\n");
            logWriter.write("Plugin version: " + plugin.getDescription().getVersion() + "\n");
            logWriter.write("Minecraft version: " + Bukkit.getVersion() + "\n");
            logWriter.write("Server Software: " + Bukkit.getName() + "\n");
            logWriter.write("Java version: " + System.getProperty("java.version") + "\n");
            logWriter.write("OS: " + System.getProperty("os.name") + "\n");
            logWriter.write("Available memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB" + "\n");
            logWriter.write("Exception type: " + e.getClass().getName() + "\n");
            logWriter.write("Message: " + e.getMessage() + "\n");
            logWriter.write("StackTrace:\n");

            for (StackTraceElement element : e.getStackTrace()) {logWriter.write("\t" + element + "\n");}

            return " &aException saved in " + logFile.getPath();
        } catch (Exception ex) {
            ex.printStackTrace();
            return " &4&lERROR when trying to save exception to " + logFile.getPath();
        }
    }

    /**
     * Deletes all saved exception log files and the "exceptions" folder from the plugin's data folder.
     *
     * @param plugin The plugin instance.
     * @return A message indicating the success or failure of deletion.
     */
    public String deleteLogFile(Plugin plugin) {
        File logDirectory = new File(plugin.getDataFolder() + "/exceptions/");

        // Comprobar si la carpeta existe
        if (logDirectory.exists() && logDirectory.isDirectory()) {
            // Borrar todos los archivos dentro de la carpeta
            File[] files = logDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Borrar cada archivo dentro de la carpeta
                    if (!file.delete()) return "%plugin% &4&lERROR when trying to delete file: " + file.getName();
                }
            }

            // Borrar la carpeta después de eliminar los archivos
            if (!logDirectory.delete()) return "%plugin% &4&lERROR when trying to delete directory: " + logDirectory.getName();
            else return "%plugin% &eException folder deleted successfully.";
        } else return "";
    }
}
