package com.aurasmp.ruin.card;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps the "aura" talents applied as long-lived potion effects, refreshed on a
 * timer so they never wear off (and never flash, since the refresh interval is
 * well under the applied duration).
 */
public final class TalentAura {

    private static final int REFRESH_TICKS = 200;   // every 10s
    private static final int DURATION_TICKS = 420;  // 21s — always > refresh

    private final RuinPlugin plugin;
    private BukkitTask task;

    public TalentAura(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    private BukkitTask fastTask;

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 40L, REFRESH_TICKS);
        // Reactive talents need a much faster pulse than the 10s aura refresh.
        fastTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::fastTick, 40L, 40L);
    }

    public void stop() {
        if (task != null) task.cancel();
        if (fastTask != null) fastTask.cancel();
    }

    /** Every 2s: the reactive/situational auras. */
    private void fastTick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.data().get(player.getUniqueId());
            // Mindful: regenerate while no enemies are near.
            if (data.hasCard(Card.MINDFUL) && !enemyNear(player, data, 10)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, false));
            }
            // Evergreen: regenerate in open sunlight.
            if (data.hasCard(Card.EVERGREEN) && player.getWorld().isDayTime()
                    && player.getLocation().getBlock().getLightFromSky() >= 15) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, false));
            }
            // Aegis Soul: 2 absorption hearts after 8s unhit.
            if (data.hasCard(Card.AEGIS_SOUL)
                    && plugin.passives().sinceLastHit(player.getUniqueId()) > 8_000
                    && !player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 3600, 0, true, false, false));
            }
            // Dread Aura: enemies within 5 blocks wilt.
            if (data.hasCard(Card.DREAD_AURA)) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof Player enemy && !data.isTrusted(enemy.getUniqueId())) {
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false, false));
                    }
                }
            }
            // Ringleader: nearby trusted allies pick up the pace.
            if (data.hasCard(Card.RINGLEADER)) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(8, 8, 8)) {
                    if (entity instanceof Player ally && data.isTrusted(ally.getUniqueId())) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, false));
                    }
                }
            }
            // Seer: invisible players nearby shimmer (visible to the seer only).
            if (data.hasCard(Card.SEER)) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(8, 8, 8)) {
                    if (entity instanceof Player hidden
                            && hidden.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        player.spawnParticle(org.bukkit.Particle.END_ROD,
                                hidden.getLocation().add(0, 1, 0), 8, 0.25, 0.6, 0.25, 0.01);
                    }
                }
            }
            // Medic: nearby hurt allies slowly regenerate (allies = players you /trust).
            if (data.hasCard(Card.MEDIC)) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(8, 8, 8)) {
                    if (entity instanceof Player ally && data.isTrusted(ally.getUniqueId())
                            && healthRatio(ally) < 0.9) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, false));
                    }
                }
            }
        }
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.data().get(player.getUniqueId());
            apply(player, data, Card.REGENERATOR, PotionEffectType.REGENERATION, 0);
            // Pack Leader (Deepwoken): resistance while a trusted ally fights beside you.
            if (data.hasCard(Card.PACK_LEADER) && hasAllyNear(player, data)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, DURATION_TICKS, 0, true, false, false));
            }
            // Data-driven permanent effects (Spring Step, Parkour, Wind Sprint, ...).
            for (var entry : Passives.PERM.entrySet()) {
                if (data.hasCard(entry.getKey())) {
                    player.addPotionEffect(new PotionEffect(entry.getValue().effect(),
                            DURATION_TICKS, entry.getValue().amplifier(), true, false, false));
                }
            }
        }
    }

    private void apply(Player player, PlayerData data, Card talent, PotionEffectType type, int amplifier) {
        if (!data.hasCard(talent)) return;
        // ambient, hidden particles, hidden icon — a clean passive buff.
        player.addPotionEffect(new PotionEffect(type, DURATION_TICKS, amplifier, true, false, false));
    }

    private double healthRatio(Player player) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double max = attr != null ? attr.getValue() : 20.0;
        return max <= 0 ? 1.0 : player.getHealth() / max;
    }

    private boolean enemyNear(Player player, PlayerData data, double range) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player other && !data.isTrusted(other.getUniqueId())) return true;
            if (entity instanceof org.bukkit.entity.Monster) return true;
        }
        return false;
    }

    private boolean hasAllyNear(Player player, PlayerData data) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Player other && data.isTrusted(other.getUniqueId())) return true;
        }
        return false;
    }
}
