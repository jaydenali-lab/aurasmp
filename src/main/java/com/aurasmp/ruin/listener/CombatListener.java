package com.aurasmp.ruin.listener;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** XP-on-kill and all flag-card combat effects. */
public final class CombatListener implements Listener {

    private final RuinPlugin plugin;

    public CombatListener(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null || killer.equals(dead)) return;

        PlayerData data = plugin.data().get(killer.getUniqueId());
        int xp = plugin.progression().xpFor(dead.getType());
        plugin.progression().award(killer, data, xp);

        if (data.hasCard(Card.LEECH)) heal(killer, 2.0);
        if (data.hasCard(Card.ADRENALINE)) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0));
        }
    }

    /** Attacker-side multipliers + lifesteal + thorns. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        boolean projectile = false;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
            projectile = true;
        }

        if (attacker != null && !attacker.equals(victim)) {
            PlayerData data = plugin.data().get(attacker.getUniqueId());
            double damage = event.getDamage();
            if (data.hasCard(Card.BERSERKER) && healthRatio(attacker) < 0.30) damage *= 1.30;
            if (data.hasCard(Card.EXECUTIONER) && healthRatio(victim) < 0.20) damage *= 1.50;
            if (projectile && data.hasCard(Card.SHARPSHOOTER)) damage *= 1.25;
            event.setDamage(damage);

            if (!projectile && data.hasCard(Card.LIFESTEAL)) heal(attacker, damage * 0.10);
        }

        // Thorns: a victim player reflects part of melee damage back. Diminishes quickly,
        // so even mutual-thorns duels converge rather than loop.
        if (victim instanceof Player victimPlayer
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getDamager() instanceof LivingEntity source
                && !source.equals(victimPlayer)) {
            PlayerData vd = plugin.data().get(victimPlayer.getUniqueId());
            if (vd.hasCard(Card.THORNS) && source.getHealth() > 0) {
                source.damage(event.getDamage() * 0.30, victimPlayer);
            }
        }
    }

    /** Victim-side mitigation: Feather (fall) and Juggernaut (all sources). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.data().get(player.getUniqueId());

        if (data.hasCard(Card.FEATHER) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (data.hasCard(Card.JUGGERNAUT)) {
            event.setDamage(event.getDamage() * 0.85);
        }
    }

    private double healthRatio(LivingEntity entity) {
        double max = entity.getAttribute(Attribute.MAX_HEALTH) != null
                ? entity.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        return max <= 0 ? 1.0 : entity.getHealth() / max;
    }

    private void heal(Player player, double amount) {
        double max = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }
}
