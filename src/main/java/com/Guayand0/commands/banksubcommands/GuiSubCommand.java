package com.Guayand0.commands.banksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.inventory.MainGUI;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class GuiSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final MainGUI mainGUI;
    private final SendMessage sendMessage;

    private final GetValues GV = new GetValues();
    private final MessageUtils MU = new MessageUtils();
    private final ExceptionManager EM = new ExceptionManager();

    public GuiSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.mainGUI = plugin.getMainGUI();
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        Map<String,String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());

        try {
            if (args.length < 2) {
                sendMessage.send(sender, "bank.gui-usage", ph); // Mensaje
                return true;
            }

            String guiName = args[1].toLowerCase();

            if (!mainGUI.guiExists(guiName)) {
                sendMessage.send(sender, "bank.gui.not-found", ph); // Mensaje
                return true;
            }

            mainGUI.openInventory(player, guiName);
        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            sendMessage.send(sender, "messages.processing-command-error", ph);  // Mensaje
        }

        return true;
    }
}
