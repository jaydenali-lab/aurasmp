package com.aurasmp.ruin.listener;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
        if (data.hasCard(Card.BLOODLUST)) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
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
            UUID aid = attacker.getUniqueId();
            PlayerData data = plugin.data().get(aid);
            double damage = event.getDamage();
            if (data.hasCard(Card.BERSERKER) && healthRatio(attacker) < 0.30) damage *= 1.30;
            if (data.hasCard(Card.EXECUTIONER) && healthRatio(victim) < 0.20) damage *= 1.50;
            if (projectile && data.hasCard(Card.SHARPSHOOTER)) damage *= 1.25;
            if (!projectile && data.hasCard(Card.CRIT) && ThreadLocalRandom.current().nextDouble() < 0.25) {
                damage *= 1.50;
                attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
            }
            if (!projectile && data.hasCard(Card.RETRIBUTION) && consumeRetribution(aid)) {
                damage += 4.0;
            }
            event.setDamage(damage);

            if (!projectile && data.hasCard(Card.LIFESTEAL)) heal(attacker, damage * 0.10);

            if (!projectile) {
                if (data.hasCard(Card.IGNITE)) victim.setFireTicks(60);
                if (data.hasCard(Card.VENOM)) victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                if (data.hasCard(Card.FROSTBITE)) victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                if (data.hasCard(Card.CLEAVE)) cleave(attacker, victim, damage);
            }
        }
    }

    // Retribution: buffered for 5s after taking a hit, consumed by the next melee hit.
    private final java.util.Map<UUID, Long> retributionUntil = new java.util.HashMap<>();
    // Guard so Cleave's splash hits don't recursively trigger more cleaves.
    private final java.util.Set<UUID> cleaving = new java.util.HashSet<>();

    private void armRetribution(UUID id) {
        retributionUntil.put(id, System.currentTimeMillis() + 5_000);
    }

    private boolean consumeRetribution(UUID id) {
        Long until = retributionUntil.get(id);
        if (until != null && System.currentTimeMillis() < until) {
            retributionUntil.remove(id);
            return true;
        }
        return false;
    }

    private void cleave(Player attacker, LivingEntity origin, double damage) {
        UUID id = attacker.getUniqueId();
        if (cleaving.contains(id)) return;
        cleaving.add(id);
        try {
            double splash = damage * 0.30;
            for (org.bukkit.entity.Entity entity : origin.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof LivingEntity le && !entity.equals(attacker)
                        && !entity.equals(origin) && le.getHealth() > 0) {
                    le.damage(splash, attacker);
                }
            }
            origin.getWorld().spawnParticle(Particle.SWEEP_ATTACK, origin.getLocation().add(0, 1, 0), 3, 0.5, 0.3, 0.5, 0);
        } finally {
            cleaving.remove(id);
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
        // Ghost: when hit, a 15% chance to fully vanish (armor too) + Speed II for 3s.
        if (data.hasCard(Card.GHOST) && ThreadLocalRandom.current().nextDouble() < 0.15) {
            applyGhost(player, 60);
        }
        // Retribution: buffer a bonus that the player's next melee hit will spend.
        if (data.hasCard(Card.RETRIBUTION)) {
            armRetribution(player.getUniqueId());
        }
    }

    private static final EquipmentSlot[] VISUAL_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
    };

    /**
     * True invisibility: the potion alone leaves armor/held items visible, so we also
     * send empty equipment to every other player, then restore the real gear when the
     * effect ends.
     */
    private void applyGhost(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.6f);

        ItemStack air = new ItemStack(Material.AIR);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(player)) continue;
            for (EquipmentSlot slot : VISUAL_SLOTS) viewer.sendEquipmentChange(player, slot, air);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (Player viewer : plugin.getServer().getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                for (EquipmentSlot slot : VISUAL_SLOTS) {
                    viewer.sendEquipmentChange(player, slot, gearAt(player, slot));
                }
            }
        }, durationTicks);
    }

    private ItemStack gearAt(Player player, EquipmentSlot slot) {
        ItemStack item = switch (slot) {
            case HEAD -> player.getInventory().getHelmet();
            case CHEST -> player.getInventory().getChestplate();
            case LEGS -> player.getInventory().getLeggings();
            case FEET -> player.getInventory().getBoots();
            case HAND -> player.getInventory().getItemInMainHand();
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            default -> null;
        };
        return item == null ? new ItemStack(Material.AIR) : item;
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
