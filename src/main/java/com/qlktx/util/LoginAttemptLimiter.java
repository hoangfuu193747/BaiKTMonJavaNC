package com.qlktx.util;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginAttemptLimiter {
    private static final long WINDOW = 15 * 60 * 1000L;
    private static final int LIMIT = 8;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private record Attempt(int count, long expiresAt) { }

    public String key(String ip, String role, String username) {
        return ip + ":" + role + ":" + username.toLowerCase(Locale.ROOT);
    }

    public boolean blocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) return false;
        if (attempt.expiresAt() <= System.currentTimeMillis()) { attempts.remove(key, attempt); return false; }
        return attempt.count() >= LIMIT;
    }

    public void failed(String key) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (attempts.size() >= 10_000 && !attempts.containsKey(key)) return;
        attempts.compute(key, (ignored, old) -> old == null || old.expiresAt() <= now
                ? new Attempt(1, now + WINDOW) : new Attempt(old.count() + 1, old.expiresAt()));
    }

    public void succeeded(String key) { attempts.remove(key); }
}
