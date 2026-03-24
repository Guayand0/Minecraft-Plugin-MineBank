package com.Guayand0.api;

import com.Guayand0.MineBank;
import com.Guayand0.data.DataStorage;
import com.Guayand0.data.bank.BankData;
import com.Guayand0.data.player.PlayerData;
import com.Guayand0.data.transactions.TransactionData;
import com.Guayand0.data.transactions.TransactionService;
import com.Guayand0.managers.EventManager;
import com.Guayand0.utils.BalanceSymbolPosition;
import com.Guayand0.zlib.GetValues;
import com.Guayand0.zlib.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebApiService {

    private final MineBank plugin;
    private final DataStorage dataStorage;
    private final TransactionService transactionService;
    private final Economy economy;
    private final Map<UUID, Long> lastWebDepositTransactionTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastWebWithdrawTransactionTimes = new ConcurrentHashMap<>();
    private final GetValues GV = new GetValues();
    private final BalanceSymbolPosition BSP = new BalanceSymbolPosition();
    private final MessageUtils MU = new MessageUtils();

    public WebApiService(MineBank plugin) {
        this.plugin = plugin;
        this.dataStorage = plugin.getStorage();
        this.transactionService = plugin.getTransactionService();
        this.economy = plugin.getEconomy();
    }

    public PlayerSnapshot getPlayerSnapshot(UUID uuid, boolean includeTransactions, int page, int pageSize, String filter, String sort, String order, int maxTotal) throws Exception {
        return callSync(() -> {
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return null;
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();
            int offlineAccrued = playerData.getBank().getOffline() != null ? playerData.getBank().getOffline().getAccrued_profit() : 0;
            int offlineTimes = playerData.getBank().getOffline() != null ? playerData.getBank().getOffline().getProfit_times() : 0;

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return null;
            }

            BankData bankData = bankDataMap.get(bankName);
            int bankMaxLevel = bankData.getLevels().size();
            int bankMaxBalance = bankData.getLevels().get(String.valueOf(bankLevel)).getMax_balance();
            int upgradeCost = bankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            boolean online = offlinePlayer.isOnline();

            double economyBalance = -1;
            if (economy != null) {
                try {
                    economyBalance = economy.getBalance(offlinePlayer);
                } catch (Exception ignored) {
                    economyBalance = -1;
                }
            }

            String normalizedFilter = normalizeFilter(filter);
            String normalizedSort = normalizeSort(sort);
            boolean oldestFirst = "oldest".equalsIgnoreCase(order);
            int safePageSize = clampPageSize(pageSize);
            int safeMaxTotal = clampMaxTotal(maxTotal);
            int totalTransactions = transactionService.countPlayerTransactions(uuid);
            int filteredTransactions = normalizedFilter == null ? totalTransactions : transactionService.countPlayerTransactionsFiltered(uuid, normalizedFilter);

            List<TransactionView> transactions = Collections.emptyList();
            if (includeTransactions) {
                List<TransactionData> raw;
                if (normalizedSort == null) {
                    if (oldestFirst) {
                        raw = normalizedFilter == null
                                ? transactionService.getPlayerTransactionsOrdered(uuid, page, safePageSize, true)
                                : transactionService.getPlayerTransactionsFilteredOrdered(uuid, page, safePageSize, normalizedFilter, true);
                    } else {
                        raw = normalizedFilter == null
                                ? transactionService.getPlayerTransactions(uuid, page, safePageSize)
                                : transactionService.getPlayerTransactionsFilteredOrdered(uuid, page, safePageSize, normalizedFilter, false);
                    }
                } else {
                    int effectiveTotal = Math.min(safeMaxTotal, filteredTransactions);
                    if (effectiveTotal <= 0) {
                        raw = Collections.emptyList();
                    } else {
                        List<TransactionData> rawAll = normalizedFilter == null
                                ? transactionService.getPlayerTransactionsOrdered(uuid, 1, effectiveTotal, oldestFirst)
                                : transactionService.getPlayerTransactionsFilteredOrdered(uuid, 1, effectiveTotal, normalizedFilter, oldestFirst);
                        rawAll.sort((a, b) -> {
                            if (a == null && b == null) return 0;
                            if (a == null) return 1;
                            if (b == null) return -1;
                            int cmp = Integer.compare(a.getAmount(), b.getAmount());
                            if ("amount_desc".equals(normalizedSort)) {
                                cmp = -cmp;
                            }
                            if (cmp != 0) return cmp;
                            int tsCmp = Long.compare(a.getTimestamp(), b.getTimestamp());
                            return oldestFirst ? tsCmp : -tsCmp;
                        });
                        int start = Math.max(0, (Math.max(1, page) - 1) * safePageSize);
                        int end = Math.min(start + safePageSize, rawAll.size());
                        raw = start >= rawAll.size() ? Collections.emptyList() : rawAll.subList(start, end);
                    }
                }
                List<TransactionView> mapped = new ArrayList<>();
                for (TransactionData row : raw) {
                    if (row == null) continue;
                    mapped.add(new TransactionView(
                            row.getId(),
                            row.getType(),
                            row.getAmount(),
                            row.getDescription(),
                            row.getContext(),
                            row.getTimestamp()
                    ));
                }
                transactions = mapped;
            }

            int maxWithdrawAmount = calculateMaxWithdrawAmount(bankBalance, bankLevel);
            double interestPercent = calculateWithdrawInterestPercent(bankBalance, bankLevel);
            double profitPercent = calculateProfitPercent(bankBalance, bankLevel);
            int maxOfflineProfitTimes = GV.getInt(plugin, "bank.profit.times-profits-offline", 0);

            return new PlayerSnapshot(
                    uuid.toString(),
                    playerName,
                    online,
                    bankName,
                    bankLevel,
                    bankBalance,
                    bankMaxLevel,
                    bankMaxBalance,
                    upgradeCost,
                    offlineAccrued,
                    offlineTimes,
                    economyBalance,
                    maxWithdrawAmount,
                    totalTransactions,
                    filteredTransactions,
                    interestPercent,
                    profitPercent,
                    maxOfflineProfitTimes,
                    transactions
            );
        });
    }

    public ApiResult deposit(UUID uuid, int amount) throws Exception {
        return callSync(() -> {
            if (amount <= 0) {
                return ApiResult.error("amount_must_be_positive", resolveMessage(uuid, "bank.not-positive-integer", null));
            }

            ApiResult validation = validateSelfTransaction(uuid, amount, true);
            if (validation != null) {
                return validation;
            }

            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return ApiResult.error("player_not_found", resolveMessage(uuid, "bank.unregistered-player", null));
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return ApiResult.error("bank_not_found", resolveMessage(uuid, "bank.unregistered-bank", null));
            }

            int bankMaxBalance = bankDataMap.get(bankName).getLevels().get(String.valueOf(bankLevel)).getMax_balance();
            if (bankBalance + amount > bankMaxBalance) {
                return ApiResult.error("deposit_exceeds_max_balance", resolveMessage(uuid, "bank.add.deposit-exceeds", null));
            }

            if (economy == null) {
                return ApiResult.error("economy_unavailable", resolveMessage(uuid, "messages.processing-command-error", null));
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (economy.getBalance(offlinePlayer) < amount) {
                return ApiResult.error("not_enough_economy_balance", resolveMessage(uuid, "bank.add.not-enough-bank-balance", null));
            }

            economy.withdrawPlayer(offlinePlayer, amount);
            playerData.getBank().setBalance(bankBalance + amount);
            dataStorage.savePlayerData(uuid, playerData);
            transactionService.register(uuid, "deposit", amount, "web", "self");
            markWebTransactionNow(uuid, true);

            Map<String, String> extra = new HashMap<>();
            extra.put("%amount%", BSP.format(plugin, String.valueOf(amount)));
            return ApiResult.ok("deposit_success", resolveMessage(uuid, "bank.add.deposit-success", extra));
        });
    }

    public ApiResult withdraw(UUID uuid, int amount) throws Exception {
        return callSync(() -> {
            if (amount <= 0) {
                return ApiResult.error("amount_must_be_positive", resolveMessage(uuid, "bank.not-positive-integer", null));
            }

            ApiResult validation = validateSelfTransaction(uuid, amount, false);
            if (validation != null) {
                return validation;
            }

            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return ApiResult.error("player_not_found", resolveMessage(uuid, "bank.unregistered-player", null));
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return ApiResult.error("bank_not_found", resolveMessage(uuid, "bank.unregistered-bank", null));
            }

            if (bankBalance < amount) {
                return ApiResult.error("not_enough_bank_balance", resolveMessage(uuid, "bank.take.not-enough-bank-balance", null));
            }

            int accruedInterestData = dataStorage.loadAccruedInterestData();
            int minBankBalanceToApplyInterest = GV.getInt(plugin, "bank.interest.min-bank-balance-to-apply", -1);
            double withdrawInterestPercentage = GV.getDouble(plugin, "bank.interest.withdraw-percentage", 0);
            boolean interestMultiplyByBankLevel = GV.getBoolean(plugin, "bank.interest.multiply-by-bank-level", false);
            double taxMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.TAX);

            double interestPercentage = 0;
            double interestAmount = 0;

            if (minBankBalanceToApplyInterest > -1 && amount > minBankBalanceToApplyInterest) {
                interestPercentage = interestMultiplyByBankLevel ? withdrawInterestPercentage * bankLevel : withdrawInterestPercentage;
                interestPercentage = interestPercentage * taxMultiplier;
                interestAmount = amount * (interestPercentage / 100.0);
            }

            int interestsAmount = (int) Math.round(interestAmount);
            int totalBankDeduction = amount + interestsAmount;

            if (bankBalance < totalBankDeduction) {
                return ApiResult.error("withdraw_exceeds_balance", resolveMessage(uuid, "bank.take.withdraw-exceeds", null));
            }

            if (economy == null) {
                return ApiResult.error("economy_unavailable", resolveMessage(uuid, "messages.processing-command-error", null));
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            economy.depositPlayer(offlinePlayer, amount);

            int newBalance = bankBalance - totalBankDeduction;
            playerData.getBank().setBalance(newBalance);
            dataStorage.savePlayerData(uuid, playerData);
            dataStorage.saveAccruedInterestData(accruedInterestData + interestsAmount);
            transactionService.register(uuid, "withdraw", amount, "web", "self");
            markWebTransactionNow(uuid, false);

            Map<String, String> extra = new HashMap<>();
            extra.put("%amountReceived%", BSP.format(plugin, String.valueOf(amount)));
            extra.put("%amountDeducted%", BSP.format(plugin, String.valueOf(totalBankDeduction)));
            extra.put("%interestPercentage%", String.valueOf(interestPercentage));
            return ApiResult.ok("withdraw_success", resolveMessage(uuid, "bank.take.withdraw-success", extra));
        });
    }

    public ApiResult withdrawAll(UUID uuid) throws Exception {
        return callSync(() -> {
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return ApiResult.error("player_not_found", resolveMessage(uuid, "bank.unregistered-player", null));
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return ApiResult.error("bank_not_found", resolveMessage(uuid, "bank.unregistered-bank", null));
            }

            if (bankBalance <= 0) {
                return ApiResult.error("amount_must_be_positive", resolveMessage(uuid, "bank.not-positive-integer", null));
            }

            int accruedInterestData = dataStorage.loadAccruedInterestData();
            int minBankBalanceToApplyInterest = GV.getInt(plugin, "bank.interest.min-bank-balance-to-apply", -1);
            double withdrawInterestPercentage = GV.getDouble(plugin, "bank.interest.withdraw-percentage", 0);
            boolean interestMultiplyByBankLevel = GV.getBoolean(plugin, "bank.interest.multiply-by-bank-level", false);
            double taxMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.TAX);

            double interestPercentage = 0;
            int interestsAmount = 0;
            if (minBankBalanceToApplyInterest > -1 && bankBalance > minBankBalanceToApplyInterest) {
                interestPercentage = interestMultiplyByBankLevel ? withdrawInterestPercentage * bankLevel : withdrawInterestPercentage;
                interestPercentage = interestPercentage * taxMultiplier;
                interestsAmount = (int) Math.floor(bankBalance * (interestPercentage / 100.0));
            }

            int amount = bankBalance - interestsAmount;
            if (amount < 0) amount = 0;

            ApiResult validation = validateSelfTransaction(uuid, amount, false);
            if (validation != null) {
                return validation;
            }

            if (economy == null) {
                return ApiResult.error("economy_unavailable", resolveMessage(uuid, "messages.processing-command-error", null));
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            economy.depositPlayer(offlinePlayer, amount);

            playerData.getBank().setBalance(0);
            dataStorage.savePlayerData(uuid, playerData);
            dataStorage.saveAccruedInterestData(accruedInterestData + interestsAmount);
            transactionService.register(uuid, "withdraw", amount, "web", "self");
            markWebTransactionNow(uuid, false);

            Map<String, String> extra = new HashMap<>();
            extra.put("%amountReceived%", BSP.format(plugin, String.valueOf(amount)));
            extra.put("%amountDeducted%", BSP.format(plugin, String.valueOf(bankBalance)));
            extra.put("%interestPercentage%", String.valueOf(interestPercentage));
            return ApiResult.ok("withdraw_success", resolveMessage(uuid, "bank.take.withdraw-success", extra));
        });
    }

    public ApiResult levelUp(UUID uuid) throws Exception {
        return callSync(() -> {
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return ApiResult.error("player_not_found", resolveMessage(uuid, "bank.unregistered-player", null));
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return ApiResult.error("bank_not_found", resolveMessage(uuid, "bank.unregistered-bank", null));
            }

            BankData bankData = bankDataMap.get(bankName);
            int bankMaxLevel = bankData.getLevels().size();
            int upgradeCost = bankData.getLevels().get(String.valueOf(bankLevel)).getUpgrade_cost();

            if (bankLevel >= bankMaxLevel) {
                return ApiResult.error("already_max_level", resolveMessage(uuid, "bank.levelup.already-max-level", null));
            }

            if (bankBalance < upgradeCost) {
                return ApiResult.error("not_enough_bank_balance", resolveMessage(uuid, "bank.levelup.not-enough-bank-balance", null));
            }

            playerData.getBank().setLevel(bankLevel + 1);
            playerData.getBank().setBalance(bankBalance - upgradeCost);
            dataStorage.savePlayerData(uuid, playerData);

            int newLevel = bankLevel + 1;
            int newMaxBalance = bankData.getLevels().get(String.valueOf(newLevel)).getMax_balance();
            Map<String, String> extra = new HashMap<>();
            extra.put("%newPlayerBankLevel%", String.valueOf(newLevel));
            extra.put("%newPlayerBankMaxBalance%", BSP.format(plugin, String.valueOf(newMaxBalance)));
            return ApiResult.ok("levelup_success", resolveMessage(uuid, "bank.levelup.levelup-success", extra));
        });
    }

    public ApiResult receiveOfflineProfit(UUID uuid) throws Exception {
        return callSync(() -> {
            PlayerData playerData = dataStorage.loadPlayerData(uuid);
            if (playerData == null || playerData.getBank() == null) {
                return ApiResult.error("player_not_found", resolveMessage(uuid, "bank.unregistered-player", null));
            }

            String bankName = playerData.getBank().getName();
            int bankLevel = playerData.getBank().getLevel();
            int bankBalance = playerData.getBank().getBalance();
            int offlineProfitAccrued = playerData.getBank().getOffline() != null
                    ? playerData.getBank().getOffline().getAccrued_profit()
                    : 0;

            Map<String, BankData> bankDataMap = dataStorage.loadBankData(bankName);
            if (bankDataMap == null || !bankDataMap.containsKey(bankName)) {
                return ApiResult.error("bank_not_found", resolveMessage(uuid, "bank.unregistered-bank", null));
            }

            int bankMaxBalance = bankDataMap.get(bankName).getLevels().get(String.valueOf(bankLevel)).getMax_balance();

            if (offlineProfitAccrued <= 0) {
                return ApiResult.error("offline_not_profit", resolveMessage(uuid, "bank.receive.offline-not-profit", null));
            }

            int playerBankSpace = bankMaxBalance - bankBalance;
            if (playerBankSpace < offlineProfitAccrued) {
                return ApiResult.error("receive_exceeds", resolveMessage(uuid, "bank.receive.receive-exceeds", null));
            }

            int newBalance = bankBalance + offlineProfitAccrued;
            Map<String, String> extra = new HashMap<>();
            extra.put("%offlineProfitAmount%", BSP.format(plugin, String.valueOf(offlineProfitAccrued)));
            playerData.getBank().setBalance(newBalance);
            playerData.getBank().getOffline().setAccrued_profit(0);
            playerData.getBank().getOffline().setProfit_times(0);
            dataStorage.savePlayerData(uuid, playerData);

            return ApiResult.ok("offline_received_success", resolveMessage(uuid, "bank.receive.offline-received-success", extra));
        });
    }

    private <T> T callSync(Callable<T> task) throws Exception {
        if (Bukkit.isPrimaryThread()) {
            return task.call();
        }
        return plugin.getSchedulerCompat().callSync(task, 5, TimeUnit.SECONDS);
    }

    private String normalizeFilter(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("deposit".equals(normalized) || "withdraw".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeSort(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("amount_asc".equals(normalized) || "amount_desc".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private int clampMaxTotal(int value) {
        return value <= 0 ? 50 : value;
    }

    private ApiResult validateSelfTransaction(UUID uuid, int amount, boolean deposit) {
        int minAmount = deposit ? Math.max(0, GV.getInt(plugin, "bank.transactions.add.min-amount", 500)) : Math.max(0, GV.getInt(plugin, "bank.transactions.take.min-amount", 500));
        if (minAmount > 0 && amount < minAmount) {
            Map<String, String> extra = new HashMap<>();
            extra.put("%minTransactionAmount%", BSP.format(plugin, String.valueOf(minAmount)));
            return ApiResult.error(
                    deposit ? "transaction_min_add_amount" : "transaction_min_take_amount",
                    resolveMessage(uuid, deposit ? "bank.transaction.min-add-amount" : "bank.transaction.min-take-amount", extra)
            );
        }

        long cooldownRemainingMs = getWebTransactionRemainingMillis(uuid, deposit);
        if (cooldownRemainingMs > 0L) {
            Map<String, String> extra = new HashMap<>();
            extra.put("%transactionCooldownRemaining%", String.valueOf(Math.max(1L, (long) Math.ceil(cooldownRemainingMs / 1000.0))));
            return ApiResult.error("transaction_cooldown", resolveMessage(uuid, "bank.transaction.cooldown", extra));
        }

        return null;
    }

    private long getWebTransactionRemainingMillis(UUID uuid, boolean deposit) {
        if (uuid == null) return 0L;
        int cooldownSeconds = Math.max(0, GV.getInt(plugin, deposit ? "bank.transactions.add.cooldown-seconds" : "bank.transactions.take.cooldown-seconds", 10));
        if (cooldownSeconds <= 0) return 0L;
        Long lastTime = (deposit ? lastWebDepositTransactionTimes : lastWebWithdrawTransactionTimes).get(uuid);
        if (lastTime == null) return 0L;
        long remaining = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastTime);
        return Math.max(0L, remaining);
    }

    private void markWebTransactionNow(UUID uuid, boolean deposit) {
        if (uuid == null) return;
        (deposit ? lastWebDepositTransactionTimes : lastWebWithdrawTransactionTimes).put(uuid, System.currentTimeMillis());
    }

    private int clampPageSize(int value) {
        int safe = value;
        if (safe < 5) safe = 5;
        if (safe > 50) safe = 50;
        if (safe % 5 != 0) {
            safe = Math.round(safe / 5f) * 5;
        }
        return safe;
    }

    private int calculateMaxWithdrawAmount(int bankBalance, int bankLevel) {
        if (bankBalance <= 0) return 0;

        int minBankBalanceToApplyInterest = GV.getInt(plugin, "bank.interest.min-bank-balance-to-apply", -1);
        double withdrawInterestPercentage = GV.getDouble(plugin, "bank.interest.withdraw-percentage", 0);
        boolean interestMultiplyByBankLevel = GV.getBoolean(plugin, "bank.interest.multiply-by-bank-level", false);
        double taxMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.TAX);

        if (minBankBalanceToApplyInterest > -1 && bankBalance > minBankBalanceToApplyInterest) {
            double interestPct = interestMultiplyByBankLevel ? withdrawInterestPercentage * bankLevel : withdrawInterestPercentage;
            interestPct = interestPct * taxMultiplier;
            int interestInt = (int) Math.floor(bankBalance * (interestPct / 100.0));
            int max = bankBalance - interestInt;
            return Math.max(0, max);
        }

        return bankBalance;
    }

    private double calculateWithdrawInterestPercent(int bankBalance, int bankLevel) {
        double withdrawInterestPercentage = GV.getDouble(plugin, "bank.interest.withdraw-percentage", 0);
        boolean interestMultiplyByBankLevel = GV.getBoolean(plugin, "bank.interest.multiply-by-bank-level", false);
        double taxMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.TAX);
        double interestPct = interestMultiplyByBankLevel ? withdrawInterestPercentage * bankLevel : withdrawInterestPercentage;
        return interestPct * taxMultiplier;
    }

    private double calculateProfitPercent(int bankBalance, int bankLevel) {
        double profitPercent = GV.getDouble(plugin, "bank.profit.keep-in-bank-percentage", 0);
        boolean multiplyByLevel = GV.getBoolean(plugin, "bank.profit.multiply-by-bank-level", false);
        double eventMultiplier = plugin.getEventManager().getMultiplier(EventManager.EventType.PROFIT);
        double finalPercent = multiplyByLevel ? profitPercent * bankLevel : profitPercent;
        return finalPercent * eventMultiplier;
    }

    private String resolveMessage(UUID uuid, String path, Map<String, String> extraPlaceholders) {
        if (path == null || path.trim().isEmpty()) return null;
        Map<String, String> placeholders = plugin.buildPlayerPlaceholders(uuid);
        if (extraPlaceholders != null) {
            placeholders.putAll(extraPlaceholders);
        }
        List<String> messages = plugin.getLanguageManager().getAllMessage(path);
        if (messages == null || messages.isEmpty()) return path;
        StringBuilder out = new StringBuilder();
        for (String msg : messages) {
            String colored = MU.getColoredReplacePluginPlaceholdersText(msg, placeholders);
            String clean = ChatColor.stripColor(colored);
            if (clean != null && !clean.trim().isEmpty()) {
                if (out.length() > 0) out.append(" ");
                out.append(clean.trim());
            }
        }
        return out.length() == 0 ? path : out.toString();
    }

    public static class PlayerSnapshot {
        public final String uuid;
        public final String name;
        public final boolean online;
        public final String bankName;
        public final int bankLevel;
        public final int bankBalance;
        public final int bankMaxLevel;
        public final int bankMaxBalance;
        public final int bankUpgradeCost;
        public final int offlineAccruedProfit;
        public final int offlineProfitTimes;
        public final double economyBalance;
        public final int maxWithdrawAmount;
        public final int totalTransactionCount;
        public final int filteredTransactionCount;
        public final double interestPercentage;
        public final double profitPercentage;
        public final int maxOfflineProfitTimes;
        public final List<TransactionView> transactions;

        public PlayerSnapshot(
                String uuid,
                String name,
                boolean online,
                String bankName,
                int bankLevel,
                int bankBalance,
                int bankMaxLevel,
                int bankMaxBalance,
                int bankUpgradeCost,
                int offlineAccruedProfit,
                int offlineProfitTimes,
                double economyBalance,
                int maxWithdrawAmount,
                int totalTransactionCount,
                int filteredTransactionCount,
                double interestPercentage,
                double profitPercentage,
                int maxOfflineProfitTimes,
                List<TransactionView> transactions
        ) {
            this.uuid = uuid;
            this.name = name;
            this.online = online;
            this.bankName = bankName;
            this.bankLevel = bankLevel;
            this.bankBalance = bankBalance;
            this.bankMaxLevel = bankMaxLevel;
            this.bankMaxBalance = bankMaxBalance;
            this.bankUpgradeCost = bankUpgradeCost;
            this.offlineAccruedProfit = offlineAccruedProfit;
            this.offlineProfitTimes = offlineProfitTimes;
            this.economyBalance = economyBalance;
            this.maxWithdrawAmount = maxWithdrawAmount;
            this.totalTransactionCount = totalTransactionCount;
            this.filteredTransactionCount = filteredTransactionCount;
            this.interestPercentage = interestPercentage;
            this.profitPercentage = profitPercentage;
            this.maxOfflineProfitTimes = maxOfflineProfitTimes;
            this.transactions = transactions;
        }
    }

    public static class TransactionView {
        public final String id;
        public final String type;
        public final int amount;
        public final String description;
        public final String context;
        public final long timestamp;

        public TransactionView(String id, String type, int amount, String description, String context, long timestamp) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.context = context;
            this.timestamp = timestamp;
        }
    }

    public static class ApiResult {
        public final boolean ok;
        public final String message;
        public final String messageText;

        private ApiResult(boolean ok, String message, String messageText) {
            this.ok = ok;
            this.message = message;
            this.messageText = messageText;
        }

        public static ApiResult ok(String message) {
            return new ApiResult(true, message, null);
        }

        public static ApiResult ok(String message, String messageText) {
            return new ApiResult(true, message, messageText);
        }

        public static ApiResult error(String message) {
            return new ApiResult(false, message, null);
        }

        public static ApiResult error(String message, String messageText) {
            return new ApiResult(false, message, messageText);
        }
    }
}
