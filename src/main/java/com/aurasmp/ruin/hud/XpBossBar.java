package com.aurasmp.ruin.hud;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A per-player XP boss bar that pops up when the player gains Ruin XP and hides
 * itself 5 seconds later. Each gain refreshes the timer, so a streak of kills
 * keeps it on screen.
 */
public final class XpBossBar {

    private final RuinPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, BukkitTask> hideTasks = new HashMap<>();

    public XpBossBar(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Show/refresh the bar for the player's current level + XP. */
    public void show(Player player, PlayerData data) {
        UUID id = player.getUniqueId();
        // Pink + solid = the classic Ender Dragon health bar look.
        BossBar bar = bars.computeIfAbsent(id,
                k -> Bukkit.createBossBar("", BarColor.PINK, BarStyle.SOLID));

        if (data.isMaxLevel()) {
            bar.setTitle("Ruin — Level MAX");
            bar.setProgress(1.0);
        } else {
            int threshold = plugin.progression().threshold(data.level());
            double progress = threshold <= 0 ? 1.0
                    : Math.max(0.0, Math.min(1.0, (double) data.xp() / threshold));
            bar.setTitle("Ruin — Level " + data.level() + "    " + data.xp() + " / " + threshold + " XP");
            bar.setProgress(progress);
        }

        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        bar.setVisible(true);

        BukkitTask prev = hideTasks.remove(id);
        if (prev != null) prev.cancel();
        long hideTicks = plugin.config().bossBarHideTicks();
        hideTasks.put(id, plugin.getServer().getScheduler().runTaskLater(plugin, () -> hide(id), hideTicks));
    }

    private void hide(UUID id) {
        BossBar bar = bars.get(id);
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }
        hideTasks.remove(id);
    }

    /** Drop all state for a player (logout) and clean up on disable. */
    public void cleanup(UUID id) {
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        BukkitTask task = hideTasks.remove(id);
        if (task != null) task.cancel();
    }

    public void cleanupAll() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        hideTasks.values().forEach(BukkitTask::cancel);
        hideTasks.clear();
    }
}
