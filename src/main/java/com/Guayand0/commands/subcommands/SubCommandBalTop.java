package com.Guayand0.commands.subcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.player.JSON.JSONGetPlayerTopData;
import com.Guayand0.managers.LanguageManager;
import com.Guayand0.utils.BankUtils;
import com.Guayand0.utils.ExceptionManager;
import com.Guayand0.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class SubCommandBalTop implements CommandExecutor {

    private final MineBank plugin;
    private final LanguageManager languageManager;

    private final MessageUtils MU = new MessageUtils();
    private final JSONGetPlayerTopData GPTD = new JSONGetPlayerTopData();

    public SubCommandBalTop(MineBank plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        try {
            int amount = 10;

            // Si el comando tiene 2 o más argumentos
            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    bankTopFailureMessage(player);
                    return true;
                }
            }

            bankTopTitleMessage(player); // Mensaje
            bankTopEntryMessage(player, amount); // Mensaje

        } catch (Exception e) {
            e.printStackTrace();
            if (BankUtils.getSaveException(plugin)) Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + ExceptionManager.saveInLog(e, plugin)));
        }

        return true;
    }

    private void bankTopFailureMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.top.baltop-failure")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTopTitleMessage(Player player) {
        for (String message : languageManager.getAllMessage("bank.top.title")) {
            player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
        }
    }

    private void bankTopEntryMessage(Player player, int amount) throws IOException {
        List<List<String>> topBanks = GPTD.getTopPlayerBanks(plugin, amount);
        int position = 1;
        for (List<String> bankInfo : topBanks) {
            plugin.placeholders.put("%topbankposition%", String.valueOf(position));
            plugin.placeholders.put("%topplayername%", bankInfo.get(0));
            plugin.placeholders.put("%topbankname%", bankInfo.get(1));
            plugin.placeholders.put("%topbanklevel%", bankInfo.get(2));
            plugin.placeholders.put("%topbankbalance%", bankInfo.get(3));

            for (String message : languageManager.getAllMessage("bank.top.entry")) {
                player.sendMessage(MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, message, plugin.placeholders));
            }
            position++;
        }
    }
}
