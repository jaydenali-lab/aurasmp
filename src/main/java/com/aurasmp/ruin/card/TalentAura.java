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

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 40L, REFRESH_TICKS);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.data().get(player.getUniqueId());
            apply(player, data, Card.NIGHT_OWL, PotionEffectType.NIGHT_VISION, 0);
            apply(player, data, Card.LEAPER, PotionEffectType.JUMP_BOOST, 0);
            apply(player, data, Card.HASTE, PotionEffectType.HASTE, 0);
            apply(player, data, Card.AQUATIC, PotionEffectType.WATER_BREATHING, 0);
            apply(player, data, Card.FIRE_WALKER, PotionEffectType.FIRE_RESISTANCE, 0);
            apply(player, data, Card.REGENERATOR, PotionEffectType.REGENERATION, 0);
            apply(player, data, Card.BARRIER, PotionEffectType.ABSORPTION, 0);

            // Conditioned Runner (Deepwoken): regen while sprinting and hurt.
            if (data.hasCard(Card.CONDITIONED_RUNNER) && player.isSprinting() && healthRatio(player) < 0.75) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, DURATION_TICKS, 0, true, false, false));
            }
            // Pack Leader (Deepwoken): resistance while an ally fights beside you.
            if (data.hasCard(Card.PACK_LEADER) && hasAllyNear(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, DURATION_TICKS, 0, true, false, false));
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

    private boolean hasAllyNear(Player player) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Player) return true;
        }
        return false;
    }
}
