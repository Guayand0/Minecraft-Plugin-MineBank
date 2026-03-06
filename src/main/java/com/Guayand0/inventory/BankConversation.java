package com.Guayand0.inventory;

import com.Guayand0.MineBank;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class BankConversation {

    private final MineBank plugin;
    private final SendMessage sendMessage;
    private final MainGUI mainGUI;

    public BankConversation(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
        this.mainGUI = plugin.getMainGUI();
    }

    public void startConversation(Player player, String guiIdOpened, boolean isWithdraw) {

        player.closeInventory(); // Cerrar el inventario

        // Evitar múltiples conversaciones activas
        if (player.hasMetadata("conversation_active")) player.removeMetadata("conversation_active", plugin);
        player.setMetadata("conversation_active", new FixedMetadataValue(plugin, true));

        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new AmountPrompt(isWithdraw, plugin)).withLocalEcho(false);

        Conversation conversation = factory.buildConversation(player);
        conversation.addConversationAbandonedListener(event -> {
            Player player1 = (Player) event.getContext().getForWhom();
            player1.removeMetadata("conversation_active", plugin);

            if (event.gracefulExit()) {
                Object sessionAmount = event.getContext().getSessionData("amount");
                if (sessionAmount != null) {
                    String input = sessionAmount.toString();

                    if (input.matches("\\d+")) {
                        String command = isWithdraw ? "bank take " + input : "bank add " + input;
                        player1.performCommand(command);
                    } else if (input.equalsIgnoreCase("exit")) {
                        sendMessage.send((CommandSender) player, "bank.gui.transaction-canceled", null); // Mensaje
                        return;
                    } else {
                        sendMessage.send((CommandSender) player, "bank.gui.invalid-amount", null); // Mensaje
                    }
                }
            }

            // Reabrir inventario
            Bukkit.getScheduler().runTask(plugin, () ->
                    mainGUI.openInventory(player1, guiIdOpened));
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
            String path = isWithdraw ? "bank.gui.amount-withdraw" : "bank.gui.amount-deposit";
            for (String msg : plugin.getLanguageManager().getAllMessage(path)) {
                return MU.getCheckAllPlaceholdersText(plugin.getPlaceholderAPI(), player, msg, plugin.placeholders);
            }
            return "";
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {

            if (input.equalsIgnoreCase("exit")) {
                context.setSessionData("amount", "exit");
                return Prompt.END_OF_CONVERSATION;
            }

            if (input.matches("\\d+")) {
                context.setSessionData("amount", input);
                return Prompt.END_OF_CONVERSATION;
            }

            return this;
        }
    }
}