package com.Guayand0.commands.minebanksubcommands;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.JsonStorage;
import com.Guayand0.data.MySQLStorage;
import com.Guayand0.data.SQLiteStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.utils.SendMessage;
import com.Guayand0.zlib.ExceptionManager;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BankSubCommand implements CommandExecutor {

    private final MineBank plugin;
    private final SendMessage sendMessage;

    private final MessageUtils MU = new MessageUtils();
    private final GetValues GV = new GetValues();
    private final ExceptionManager EM = new ExceptionManager();

    public BankSubCommand(MineBank plugin) {
        this.plugin = plugin;
        this.sendMessage = plugin.getSendMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;
        Map<String, String> ph = plugin.buildPlayerPlaceholders(player.getUniqueId());
        String usageKey = "bank.manage.usage-admin";

        if (args.length < 2) {
            sendMessage.send(sender, usageKey, ph);
            return true;
        }

        try {
            DataStorage storage = plugin.getStorage();
            String action = args[1].toLowerCase();

            switch (action) {
                case "create":
                    return handleCreate(sender, ph, storage, args);

                case "add":
                    return handleAddLevel(sender, ph, storage, args);

                case "modify":
                    return handleModify(sender, ph, storage, args);

                case "rename":
                    return handleRename(sender, ph, storage, args);

                case "delete":
                    return handleDelete(sender, ph, storage, args);

                default:
                    sendMessage.send(sender, usageKey, ph);
                    return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (GV.getBoolean(plugin, "exception.save", true)) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + EM.saveInLog(e, plugin)));
            }
            sendMessage.send(sender, "messages.processing-command-error", ph);
        }

        return true;
    }

    private boolean handleCreate(CommandSender sender, Map<String, String> ph, DataStorage storage, String[] args) {
        if (args.length != 3) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        String bankName = args[2];
        if (!isValidBankName(bankName)) {
            sendMessage.send(sender, "bank.manage.invalid-bank-name", ph);
            return true;
        }
        if (bankExists(storage, bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-exists", ph);
            return true;
        }

        int defaultMaxBalance = resolveDefaultMaxBalance(storage);
        LinkedHashMap<String, BankData.Level> levels = new LinkedHashMap<>();
        levels.put("1", new BankData.Level(defaultMaxBalance, 0));

        BankData data = new BankData(bankName, levels);
        Map<String, BankData> wrapper = new LinkedHashMap<>();
        wrapper.put(bankName, data);

        List<String> ordered = new ArrayList<>(storage.getAllBankNames());
        ordered.add(bankName);

        storage.saveBankData(bankName, wrapper, ordered.size());
        updateBankPriorities(storage, ordered);

        plugin.updateRegisterBankPermissionTask();

        ph.put("%bankName%", bankName);
        sendMessage.send(sender, "bank.manage.bank-created", ph);
        return true;
    }

    private boolean handleAddLevel(CommandSender sender, Map<String, String> ph, DataStorage storage, String[] args) {
        if (args.length != 9) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        String bankName = args[2];
        if (!"level".equalsIgnoreCase(args[3]) || !"max_balance".equalsIgnoreCase(args[5]) || !"upgrade_cost".equalsIgnoreCase(args[7])) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        Integer newLevelNumberArg = parsePositiveInt(args[4]);
        Integer maxBalance = parsePositiveInt(args[6]);
        Integer upgradeCost = parseNonNegativeInt(args[8]);

        if (newLevelNumberArg == null || maxBalance == null || upgradeCost == null) {
            sendMessage.send(sender, "bank.not-positive-integer", ph);
            return true;
        }
        if (upgradeCost > maxBalance) {
            sendMessage.send(sender, "bank.manage.upgrade-cost-too-high", ph);
            return true;
        }

        if (!bankExists(storage, bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        bankName = resolveExistingBankName(storage, bankName);
        Map<String, BankData> bankDataMap = storage.loadBankData(bankName);
        if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        BankData bankData = bankDataMap.get(bankName);
        LinkedHashMap<String, BankData.Level> levels = normalizeLevels(bankData.getLevels());

        int expectedNewLevelNumber = levels.size() + 1;
        if (newLevelNumberArg != expectedNewLevelNumber) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }
        int newLevelNumber = expectedNewLevelNumber;
        levels.put(String.valueOf(newLevelNumber), new BankData.Level(maxBalance, upgradeCost));

        bankData.setLevels(levels);
        int priority = resolvePriority(storage, bankName);
        storage.saveBankData(bankName, bankDataMap, priority);

        ph.put("%bankName%", bankName);
        ph.put("%level%", String.valueOf(newLevelNumber));
        sendMessage.send(sender, "bank.manage.level-added", ph);
        return true;
    }

    private boolean handleModify(CommandSender sender, Map<String, String> ph, DataStorage storage, String[] args) {
        if (args.length < 5) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        String bankName = args[2];
        String type = args[3].toLowerCase();

        if ("priority".equals(type)) {
            if (args.length != 5) {
                sendMessage.send(sender, "bank.manage.usage-admin", ph);
                return true;
            }
            Integer newPriority = parsePositiveInt(args[4]);
            if (newPriority == null) {
                sendMessage.send(sender, "bank.not-positive-integer", ph);
                return true;
            }
            if (!bankExists(storage, bankName)) {
                ph.put("%bankName%", bankName);
                sendMessage.send(sender, "bank.manage.bank-not-found", ph);
                return true;
            }

            bankName = resolveExistingBankName(storage, bankName);
            List<String> ordered = new ArrayList<>(storage.getAllBankNames());
            final String bankNameFinal = bankName;
            ordered.removeIf(name -> name.equalsIgnoreCase(bankNameFinal));
            int position = Math.max(1, Math.min(newPriority, ordered.size() + 1));
            ordered.add(position - 1, bankName);

            updateBankPriorities(storage, ordered);
            plugin.updateRegisterBankPermissionTask();

            ph.put("%bankName%", bankName);
            ph.put("%priority%", String.valueOf(position));
            sendMessage.send(sender, "bank.manage.priority-updated", ph);
            return true;
        }

        if (!"level".equals(type) || args.length != 7) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        Integer levelNumber = parsePositiveInt(args[4]);
        if (levelNumber == null) {
            sendMessage.send(sender, "bank.not-positive-integer", ph);
            return true;
        }

        String field = args[5].toLowerCase();
        Integer value = "upgrade_cost".equalsIgnoreCase(field)
                ? parseNonNegativeInt(args[6])
                : parsePositiveInt(args[6]);
        if (value == null) {
            sendMessage.send(sender, "bank.not-positive-integer", ph);
            return true;
        }

        if (!bankExists(storage, bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        bankName = resolveExistingBankName(storage, bankName);
        Map<String, BankData> bankDataMap = storage.loadBankData(bankName);
        if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        BankData bankData = bankDataMap.get(bankName);
        Map<String, BankData.Level> rawLevels = bankData.getLevels();
        BankData.Level target = rawLevels != null ? rawLevels.get(String.valueOf(levelNumber)) : null;
        if (target == null) {
            ph.put("%bankName%", bankName);
            ph.put("%level%", String.valueOf(levelNumber));
            sendMessage.send(sender, "bank.manage.level-not-found", ph);
            return true;
        }

        if ("max_balance".equals(field)) {
            if (value < target.getUpgrade_cost()) {
                sendMessage.send(sender, "bank.manage.upgrade-cost-too-high", ph);
                return true;
            }
            target.setMax_balance(value);
        } else if ("upgrade_cost".equals(field)) {
            if (value > target.getMax_balance()) {
                sendMessage.send(sender, "bank.manage.upgrade-cost-too-high", ph);
                return true;
            }
            target.setUpgrade_cost(value);
        } else {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        LinkedHashMap<String, BankData.Level> normalized = normalizeLevels(rawLevels);
        bankData.setLevels(normalized);
        int priority = resolvePriority(storage, bankName);
        storage.saveBankData(bankName, bankDataMap, priority);

        ph.put("%bankName%", bankName);
        ph.put("%level%", String.valueOf(levelNumber));
        ph.put("%value%", String.valueOf(value));
        sendMessage.send(sender, "bank.manage.level-modified", ph);
        return true;
    }

    private boolean handleDelete(CommandSender sender, Map<String, String> ph, DataStorage storage, String[] args) {
        if (args.length < 3) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        if ("confirm".equalsIgnoreCase(args[2])) {
            String pendingBank = plugin.getPendingBankDeletions().remove(((Player) sender).getUniqueId());
            if (pendingBank == null) {
                sendMessage.send(sender, "bank.manage.delete-no-pending", ph);
                return true;
            }

            deleteBank(storage, pendingBank);

            List<String> ordered = new ArrayList<>(storage.getAllBankNames());
            ordered.removeIf(name -> name.equalsIgnoreCase(pendingBank));
            updateBankPriorities(storage, ordered);

            plugin.updateRegisterBankPermissionTask();

            ph.put("%bankName%", pendingBank);
            sendMessage.send(sender, "bank.manage.bank-deleted", ph);
            return true;
        }

        String bankName = args[2];

        if (args.length == 5 && "level".equalsIgnoreCase(args[3])) {
            Integer levelNumber = parsePositiveInt(args[4]);
            if (levelNumber == null) {
                sendMessage.send(sender, "bank.not-positive-integer", ph);
                return true;
            }

            if (!bankExists(storage, bankName)) {
                ph.put("%bankName%", bankName);
                sendMessage.send(sender, "bank.manage.bank-not-found", ph);
                return true;
            }

            bankName = resolveExistingBankName(storage, bankName);
            Map<String, BankData> bankDataMap = storage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                ph.put("%bankName%", bankName);
                sendMessage.send(sender, "bank.manage.bank-not-found", ph);
                return true;
            }

            BankData bankData = bankDataMap.get(bankName);
            Map<String, BankData.Level> rawLevels = bankData.getLevels();
            LinkedHashMap<String, BankData.Level> levels = normalizeLevels(rawLevels);
            if (levels.size() <= 1) {
                ph.put("%bankName%", bankName);
                sendMessage.send(sender, "bank.manage.level-last-cannot-delete", ph);
                return true;
            }

            if (rawLevels == null || !rawLevels.containsKey(String.valueOf(levelNumber))) {
                ph.put("%bankName%", bankName);
                ph.put("%level%", String.valueOf(levelNumber));
                sendMessage.send(sender, "bank.manage.level-not-found", ph);
                return true;
            }

            rawLevels.remove(String.valueOf(levelNumber));
            levels = normalizeLevels(rawLevels);

            bankData.setLevels(levels);
            int priority = resolvePriority(storage, bankName);
            storage.saveBankData(bankName, bankDataMap, priority);

            ph.put("%bankName%", bankName);
            ph.put("%level%", String.valueOf(levelNumber));
            sendMessage.send(sender, "bank.manage.level-deleted", ph);
            return true;
        }

        if (args.length != 3) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        if (!bankExists(storage, bankName)) {
            ph.put("%bankName%", bankName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        bankName = resolveExistingBankName(storage, bankName);
        plugin.getPendingBankDeletions().put(((Player) sender).getUniqueId(), bankName);
        ph.put("%bankName%", bankName);
        sendMessage.send(sender, "bank.manage.delete-confirm-needed", ph);

        final String bankNameFinal = bankName;
        final Player playerFinal = (Player) sender;
        new BukkitRunnable() {
            @Override
            public void run() {
                String current = plugin.getPendingBankDeletions().get(playerFinal.getUniqueId());
                if (current != null && current.equalsIgnoreCase(bankNameFinal)) {
                    plugin.getPendingBankDeletions().remove(playerFinal.getUniqueId());
                    Player p = Bukkit.getPlayer(playerFinal.getUniqueId());
                    if (p != null && p.isOnline()) {
                        sendMessage.send(p, "bank.manage.delete-confirm-expired", ph);
                    }
                }
            }
        }.runTaskLater(plugin, 200L);

        return true;
    }

    private boolean handleRename(CommandSender sender, Map<String, String> ph, DataStorage storage, String[] args) {
        if (args.length != 4) {
            sendMessage.send(sender, "bank.manage.usage-admin", ph);
            return true;
        }

        String oldName = args[2];
        String newName = args[3];

        if (!bankExists(storage, oldName)) {
            ph.put("%bankName%", oldName);
            sendMessage.send(sender, "bank.manage.bank-not-found", ph);
            return true;
        }

        if (!isValidBankName(newName)) {
            sendMessage.send(sender, "bank.manage.invalid-bank-name", ph);
            return true;
        }

        if (bankExists(storage, newName)) {
            ph.put("%bankName%", newName);
            sendMessage.send(sender, "bank.manage.bank-exists", ph);
            return true;
        }

        oldName = resolveExistingBankName(storage, oldName);

        boolean renamed = renameBank(storage, oldName, newName);
        if (!renamed) {
            sendMessage.send(sender, "messages.processing-command-error", ph);
            return true;
        }

        renameBankForPlayers(storage, oldName, newName);
        updateBankPriorities(storage, storage.getAllBankNames());
        plugin.updateRegisterBankPermissionTask();

        ph.put("%bankName%", oldName);
        ph.put("%newBankName%", newName);
        sendMessage.send(sender, "bank.manage.bank-renamed", ph);
        return true;
    }

    private int resolvePriority(DataStorage storage, String bankName) {
        List<String> ordered = storage.getAllBankNames();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).equalsIgnoreCase(bankName)) {
                return i + 1;
            }
        }
        return ordered.size() + 1;
    }

    private boolean bankExists(DataStorage storage, String bankName) {
        List<String> banks = storage.getAllBankNames();
        for (String name : banks) {
            if (name.equalsIgnoreCase(bankName)) {
                return true;
            }
        }
        return false;
    }

    private String resolveExistingBankName(DataStorage storage, String bankName) {
        List<String> banks = storage.getAllBankNames();
        for (String name : banks) {
            if (name.equalsIgnoreCase(bankName)) {
                return name;
            }
        }
        return bankName;
    }

    private Integer parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseNonNegativeInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value >= 0 ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidBankName(String bankName) {
        if (bankName == null) return false;
        return bankName.matches("^[A-Za-z0-9_+]{3,}$");
    }

    private int resolveDefaultMaxBalance(DataStorage storage) {
        try {
            List<String> banks = storage.getAllBankNames();
            if (!banks.isEmpty()) {
                Map<String, BankData> bankDataMap = storage.loadBankData(banks.get(0));
                if (bankDataMap != null && bankDataMap.containsKey(banks.get(0))) {
                    BankData.Level level1 = bankDataMap.get(banks.get(0)).getLevels().get("1");
                    if (level1 != null) {
                        return level1.getMax_balance();
                    }
                }
            }
        } catch (Exception ignored) {}
        return 1000;
    }

    private LinkedHashMap<String, BankData.Level> normalizeLevels(Map<String, BankData.Level> levels) {
        if (levels == null || levels.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<Integer> keys = new ArrayList<>();
        for (String key : levels.keySet()) {
            try {
                keys.add(Integer.parseInt(key));
            } catch (Exception ignored) {}
        }
        Collections.sort(keys);

        LinkedHashMap<String, BankData.Level> ordered = new LinkedHashMap<>();
        int index = 1;
        for (Integer key : keys) {
            BankData.Level level = levels.get(String.valueOf(key));
            if (level != null) {
                ordered.put(String.valueOf(index), level);
                index++;
            }
        }

        return ordered;
    }

    private void updateBankPriorities(DataStorage storage, List<String> ordered) {
        if (ordered == null) return;
        if (storage instanceof JsonStorage) {
            ((JsonStorage) storage).saveBankPriorities(ordered);
        } else if (storage instanceof SQLiteStorage) {
            ((SQLiteStorage) storage).updateBankPriorities(ordered);
        } else if (storage instanceof MySQLStorage) {
            ((MySQLStorage) storage).updateBankPriorities(ordered);
        }
    }

    private void deleteBank(DataStorage storage, String bankName) {
        if (storage instanceof JsonStorage) {
            ((JsonStorage) storage).deleteBankData(bankName);
        } else if (storage instanceof SQLiteStorage) {
            ((SQLiteStorage) storage).deleteBankData(bankName);
        } else if (storage instanceof MySQLStorage) {
            ((MySQLStorage) storage).deleteBankData(bankName);
        }
    }

    private boolean renameBank(DataStorage storage, String oldName, String newName) {
        if (storage instanceof JsonStorage) {
            return ((JsonStorage) storage).renameBankData(oldName, newName);
        } else if (storage instanceof SQLiteStorage) {
            return ((SQLiteStorage) storage).renameBankData(oldName, newName);
        } else if (storage instanceof MySQLStorage) {
            return ((MySQLStorage) storage).renameBankData(oldName, newName);
        }
        return false;
    }

    private void renameBankForPlayers(DataStorage storage, String oldName, String newName) {
        try {
            for (java.util.UUID uuid : storage.getAllPlayerUUIDs()) {
                com.Guayand0.data.player.PlayerData data = storage.loadPlayerData(uuid);
                if (data == null || data.getBank() == null) continue;
                if (data.getBank().getName().equalsIgnoreCase(oldName)) {
                    data.getBank().setName(newName);
                    storage.savePlayerData(uuid, data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
