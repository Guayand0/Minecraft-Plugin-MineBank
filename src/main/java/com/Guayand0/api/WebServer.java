package com.Guayand0.api;

import com.Guayand0.MineBank;
import com.Guayand0.api.WebApiService.ApiResult;
import com.Guayand0.api.WebApiService.PlayerSnapshot;
import com.Guayand0.zlib.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.UUID;

public class WebServer {

    private final MineBank plugin;
    private final int port;
    private final WebApiService apiService;
    private final WebTokenStore tokenStore;
    private final int refreshIntervalMs;
    private final MessageUtils MU = new MessageUtils();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public WebServer(MineBank plugin, int port) {
        this.plugin = plugin;
        this.port = port;
        this.apiService = new WebApiService(plugin);
        this.tokenStore = plugin.getWebTokenStore();
        this.refreshIntervalMs = Math.max(1000, plugin.getConfig().getInt("web.refresh-interval-ms", 2000));
    }

    public void start() {
        try {
            if (server != null) {
                return;
            }

            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", this::handleRoot);
            server.createContext("/player", this::handlePlayerPage);
            server.createContext("/api", this::handleApi);
            server.createContext("/web", this::handleStatic);
            executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.start();

            ensureWebFiles();
            File webDir = new File(plugin.getDataFolder(), "web");
            File logo = new File(webDir, "bank.png");
            if (!logo.exists()) {
                Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Web logo not found at " + logo.getPath()));
            }

            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Web panel started on port " + port));
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Failed to start web panel: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Web panel stopped"));
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        sendRedirect(exchange, "/web/login.html");
    }

    private void handlePlayerPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String token = query.getOrDefault("t", "");
        UUID parsed = tokenStore.resolveUuidByToken(token);
        if (parsed == null || !tokenStore.isValid(parsed, token)) {
            sendText(exchange, 403, "Forbidden");
            return;
        }
        sendRedirect(exchange, "/web/bank.html?t=" + token);
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = exchange.getRequestURI().getPath();

        if (!path.startsWith("/api/")) {
            sendJson(exchange, 404, mapOf("ok", false, "message", "not_found"));
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String token = query.getOrDefault("t", "");
        UUID uuid = tokenStore.resolveUuidByToken(token);
        if (uuid == null || !tokenStore.isValid(uuid, token)) {
            sendJson(exchange, 403, mapOf("ok", false, "message", "forbidden"));
            return;
        }

        try {
            if ("GET".equals(method) && "/api/me".equalsIgnoreCase(path)) {
                boolean includeTransactions = "1".equals(query.get("transactions")) || "true".equalsIgnoreCase(query.get("transactions"));
                int page = parseInt(query.get("page"), 1);
                int pageSize = parseInt(query.get("pageSize"), 20);
                String filter = query.get("filter");
                String sort = query.get("sort");
                String order = query.get("order");
                int maxTotal = parseInt(query.get("max"), 10);

                PlayerSnapshot snapshot = apiService.getPlayerSnapshot(uuid, includeTransactions, page, pageSize, filter, sort, order, maxTotal);
                if (snapshot == null) {
                    sendJson(exchange, 404, mapOf("ok", false, "message", "player_not_found"));
                    return;
                }
                long tokenRemainingMs = tokenStore.getRemainingMillis(uuid, token);
                sendJson(exchange, 200, mapOf("ok", true, "data", snapshot, "tokenRemainingMs", tokenRemainingMs, "offlineSessionRemainingMs", getOfflineSessionRemainingMs(uuid, token, tokenRemainingMs), "refreshIntervalMs", Math.max(1000, plugin.getConfig().getInt("web.refresh-interval-ms", 2000))));
                return;
            }

            if ("GET".equals(method) && "/api/me/transactions".equalsIgnoreCase(path)) {
                int page = parseInt(query.get("page"), 1);
                int pageSize = parseInt(query.get("pageSize"), 20);
                String filter = query.get("filter");
                String sort = query.get("sort");
                String order = query.get("order");
                int maxTotal = parseInt(query.get("max"), 10);
                PlayerSnapshot snapshot = apiService.getPlayerSnapshot(uuid, true, page, pageSize, filter, sort, order, maxTotal);
                if (snapshot == null) {
                    sendJson(exchange, 404, mapOf("ok", false, "message", "player_not_found"));
                    return;
                }
                long tokenRemainingMs = tokenStore.getRemainingMillis(uuid, token);
                sendJson(exchange, 200, mapOf("ok", true, "data", snapshot.transactions, "tokenRemainingMs", tokenRemainingMs, "offlineSessionRemainingMs", getOfflineSessionRemainingMs(uuid, token, tokenRemainingMs), "refreshIntervalMs", Math.max(1000, plugin.getConfig().getInt("web.refresh-interval-ms", 2000))));
                return;
            }

            if ("POST".equals(method) && path.startsWith("/api/me/")) {
                String action = path.substring("/api/me/".length()).toLowerCase(Locale.ROOT);
                Map<String, Object> body = parseJsonBody(exchange);
                int amount = parseAmount(body.get("amount"));

                if ("deposit".equals(action)) {
                    ApiResult result = apiService.deposit(uuid, amount);
                    sendJson(exchange, 200, mapOf("ok", result.ok, "message", result.message, "messageText", result.messageText));
                    return;
                }

                if ("withdraw".equals(action)) {
                    ApiResult result = apiService.withdraw(uuid, amount);
                    sendJson(exchange, 200, mapOf("ok", result.ok, "message", result.message, "messageText", result.messageText));
                    return;
                }

                if ("withdraw-all".equals(action)) {
                    ApiResult result = apiService.withdrawAll(uuid);
                    sendJson(exchange, 200, mapOf("ok", result.ok, "message", result.message, "messageText", result.messageText));
                    return;
                }

                if ("levelup".equals(action)) {
                    ApiResult result = apiService.levelUp(uuid);
                    sendJson(exchange, 200, mapOf("ok", result.ok, "message", result.message, "messageText", result.messageText));
                    return;
                }

                if ("receive-profit".equals(action)) {
                    ApiResult result = apiService.receiveOfflineProfit(uuid);
                    sendJson(exchange, 200, mapOf("ok", result.ok, "message", result.message, "messageText", result.messageText));
                    return;
                }

                if ("invalidate-token".equals(action)) {
                    tokenStore.invalidate(uuid);
                    sendJson(exchange, 200, mapOf("ok", true, "message", "disconnected", "messageText", "Disconnected"));
                    return;
                }
            }

            sendJson(exchange, 404, mapOf("ok", false, "message", "not_found"));
        } catch (Exception e) {
            sendJson(exchange, 500, mapOf("ok", false, "message", "internal_error"));
            Bukkit.getConsoleSender().sendMessage(MU.getColoredText(plugin.prefix + " Web API error: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith("/web/")) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        String fileName = path.substring("/web/".length());
        if (fileName.isEmpty()) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        java.io.File file = new java.io.File(plugin.getDataFolder(), "web" + java.io.File.separator + fileName);
        if (!file.exists() || !file.isFile()) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        String contentType = "application/octet-stream";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lower.endsWith(".html")) {
            contentType = "text/html; charset=utf-8";
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", contentType);
        headers.add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> query = new HashMap<>();
        String raw = uri.getQuery();
        if (raw == null || raw.isEmpty()) return query;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                query.put(kv[0], kv[1]);
            }
        }
        return query;
    }

    private Map<String, Object> parseJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] bytes = readAllBytes(input);
            if (bytes.length == 0) return Collections.emptyMap();
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.trim().isEmpty()) return Collections.emptyMap();
            return gson.fromJson(text, Map.class);
        } catch (JsonSyntaxException ex) {
            return Collections.emptyMap();
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int parseAmount(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) {
            return (int) Math.floor(((Number) value).doubleValue());
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            try {
                return (int) Math.floor(Double.parseDouble(raw));
            } catch (Exception ignored2) {
                return 0;
            }
        }
    }

    private void ensureWebFiles() throws IOException {
        java.io.File webDir = new java.io.File(plugin.getDataFolder(), "web");
        if (!webDir.exists()) {
            webDir.mkdirs();
        }
        writeUtf8(new java.io.File(webDir, "login.html"), loadResourceText("web/login.html"));
        String bankHtml = loadResourceText("web/bank.html")
                .replace("__REFRESH_MS__", String.valueOf(refreshIntervalMs))
                .replace("__WITHDRAW_INTEREST_THRESHOLD__", String.valueOf(plugin.getConfig().getInt("bank.interest.min-bank-balance-to-apply", -1)));
        writeUtf8(new java.io.File(webDir, "bank.html"), bankHtml);
        writeBytes(new java.io.File(webDir, "bank.png"), loadResourceBytes("web/bank.png"));
    }

    private void writeUtf8(java.io.File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBytes(java.io.File file, byte[] content) throws IOException {
        Files.write(file.toPath(), content);
    }

    private String loadResourceText(String resourcePath) throws IOException {
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing web resource: " + resourcePath);
            }
            return new String(readAllBytes(input), StandardCharsets.UTF_8);
        }
    }

    private byte[] loadResourceBytes(String resourcePath) throws IOException {
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing web resource: " + resourcePath);
            }
            return readAllBytes(input);
        }
    }

    private long getOfflineSessionRemainingMs(UUID uuid, String token, long tokenRemainingMs) {
        long disconnectedAt = tokenStore.getDisconnectedAt(uuid, token);
        if (disconnectedAt <= 0L) {
            return 0L;
        }
        long disconnectMinutes = Math.max(0L, plugin.getConfig().getLong("web.token.invalidate-after-disconnect-minutes", 5));
        long disconnectRemainingMs = Math.max(0L, (disconnectMinutes * 60_000L) - (System.currentTimeMillis() - disconnectedAt));
        return Math.max(0L, Math.min(tokenRemainingMs, disconnectRemainingMs));
    }

    private void sendRedirect(HttpExchange exchange, String location) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Location", location);
        headers.add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void sendText(HttpExchange exchange, int status, String text) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "text/plain; charset=utf-8");
        headers.add("Access-Control-Allow-Origin", "*");
        send(exchange, status, text);
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        headers.add("Access-Control-Allow-Origin", "*");
        String json = gson.toJson(payload);
        send(exchange, status, json);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
