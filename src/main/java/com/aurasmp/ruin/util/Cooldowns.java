package com.aurasmp.ruin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Simple per-player, per-key cooldown tracker backed by System time. */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> data = new HashMap<>();

    public boolean isReady(UUID player, String key) {
        return remainingMillis(player, key) <= 0;
    }

    public long remainingMillis(UUID player, String key) {
        Map<String, Long> m = data.get(player);
        if (m == null) return 0;
        Long until = m.get(key);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public void set(UUID player, String key, long millis) {
        data.computeIfAbsent(player, k -> new HashMap<>()).put(key, System.currentTimeMillis() + millis);
    }

    /** Shaves {@code millis} off every running cooldown this player has. */
    public void reduceAll(UUID player, long millis) {
        Map<String, Long> m = data.get(player);
        if (m == null) return;
        m.replaceAll((key, until) -> until - millis);
    }

    public void clear(UUID player) {
        data.remove(player);
    }
}
