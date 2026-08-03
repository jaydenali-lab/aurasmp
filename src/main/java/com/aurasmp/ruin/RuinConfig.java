package com.aurasmp.ruin;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** Typed view over config.yml, reloadable at runtime via {@code /ruin reload}. */
public final class RuinConfig {

    private static final int[] DEFAULT_THRESHOLDS = {
            100, 200, 320, 460, 620, 800, 1000, 1220, 1460, 1720,
            2000, 2300, 2620, 2960, 3320, 3700, 4100, 4520, 4960, 5420,
            5900, 6400, 6920, 7460};

    private final RuinPlugin plugin;

    private int xpPlayer, xpBoss, xpStrong, xpCommon, xpPassive, xpWeak;
    private int[] thresholds;
    private double ghostChance;
    private double scavengerMultiplier;
    private long bossBarHideTicks;

    public RuinConfig(RuinPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        xpPlayer = c.getInt("xp.player", 50);
        xpBoss = c.getInt("xp.boss", 150);
        xpStrong = c.getInt("xp.strong", 20);
        xpCommon = c.getInt("xp.common", 8);
        xpPassive = c.getInt("xp.passive", 2);
        xpWeak = c.getInt("xp.weak", 1);

        List<Integer> list = c.getIntegerList("thresholds");
        if (list.isEmpty()) {
            thresholds = DEFAULT_THRESHOLDS.clone();
        } else {
            thresholds = list.stream().mapToInt(Integer::intValue).toArray();
        }

        ghostChance = clamp(c.getDouble("ghost.chance", 0.15), 0.0, 1.0);
        scavengerMultiplier = Math.max(0.0, c.getDouble("scavenger.multiplier", 1.5));
        bossBarHideTicks = Math.max(1L, Math.round(c.getDouble("boss-bar.hide-seconds", 5.0) * 20.0));
    }

    public int xpPlayer() { return xpPlayer; }
    public int xpBoss() { return xpBoss; }
    public int xpStrong() { return xpStrong; }
    public int xpCommon() { return xpCommon; }
    public int xpPassive() { return xpPassive; }
    public int xpWeak() { return xpWeak; }

    /** XP to advance from {@code level}, or -1 if there is no next level. */
    public int threshold(int level) {
        if (level < 1 || level > thresholds.length) return -1;
        return thresholds[level - 1];
    }

    public double ghostChance() { return ghostChance; }
    public double scavengerMultiplier() { return scavengerMultiplier; }
    public long bossBarHideTicks() { return bossBarHideTicks; }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
