package com.Guayand0.api;

import com.Guayand0.MineBank;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebTokenStore {

    private final MineBank plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public WebTokenStore(MineBank plugin) {
        this.plugin = plugin;
    }

    public String createNewSession(UUID uuid) {
        if (uuid == null) return null;
        long ttlMinutes = Math.max(1, plugin.getConfig().getLong("web.token.ttl-minutes", 10));
        long expiresAt = System.currentTimeMillis() + (ttlMinutes * 60_000L);
        String token = UUID.randomUUID().toString();
        sessions.put(uuid, new Session(token, expiresAt, 0L));
        return token;
    }

    public boolean isValid(UUID uuid, String token) {
        if (uuid == null || token == null || token.trim().isEmpty()) return false;
        Session session = sessions.get(uuid);
        if (session == null) return false;
        if (session.expiresAt <= System.currentTimeMillis()) {
            sessions.remove(uuid);
            return false;
        }
        return session.token.equals(token);
    }

    public UUID resolveUuidByToken(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Session>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            Session session = entry.getValue();
            if (session == null) {
                iterator.remove();
                continue;
            }
            if (session.expiresAt <= now) {
                iterator.remove();
                continue;
            }
            if (session.token.equals(token)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void invalidate(UUID uuid) {
        if (uuid != null) {
            sessions.remove(uuid);
        }
    }

    public long getRemainingMillis(UUID uuid, String token) {
        if (uuid == null || token == null || token.trim().isEmpty()) return 0L;
        Session session = sessions.get(uuid);
        if (session == null || !session.token.equals(token)) return 0L;
        long remaining = session.expiresAt - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public long getDisconnectedAt(UUID uuid, String token) {
        if (uuid == null || token == null || token.trim().isEmpty()) return 0L;
        Session session = sessions.get(uuid);
        if (session == null || !session.token.equals(token)) return 0L;
        return Math.max(0L, session.disconnectedAt);
    }

    public long markDisconnected(UUID uuid) {
        if (uuid == null) return 0L;
        Session session = sessions.get(uuid);
        if (session == null) return 0L;
        long disconnectedAt = System.currentTimeMillis();
        session.disconnectedAt = disconnectedAt;
        return disconnectedAt;
    }

    public void markConnected(UUID uuid) {
        if (uuid == null) return;
        Session session = sessions.get(uuid);
        if (session != null) {
            session.disconnectedAt = 0L;
        }
    }

    public void invalidateIfDisconnected(UUID uuid, long expectedDisconnectedAt) {
        if (uuid == null || expectedDisconnectedAt <= 0L) return;
        Session session = sessions.get(uuid);
        if (session == null) return;
        long now = System.currentTimeMillis();
        if (session.expiresAt <= now) {
            sessions.remove(uuid);
            return;
        }
        if (session.disconnectedAt != expectedDisconnectedAt) {
            return;
        }
        sessions.remove(uuid);
    }

    public void clear() {
        sessions.clear();
    }

    private static class Session {
        private final String token;
        private final long expiresAt;
        private volatile long disconnectedAt;

        private Session(String token, long expiresAt, long disconnectedAt) {
            this.token = token;
            this.expiresAt = expiresAt;
            this.disconnectedAt = disconnectedAt;
        }
    }
}
