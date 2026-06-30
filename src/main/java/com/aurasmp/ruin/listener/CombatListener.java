package com.aurasmp.ruin.listener;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.event.entity.ProjectileHitEvent;
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

    /** Ruin fireball impact: true AoE damage + fire, no block grief. */
    @EventHandler(ignoreCancelled = true)
    public void onFireball(ProjectileHitEvent event) {
        if (!plugin.abilities().isRuinFireball(event.getEntity())) return;
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            event.getEntity().remove();
            return;
        }
        Location loc = event.getEntity().getLocation();
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FLAME, loc, 25, 1, 1, 1, 0.03);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.3f);
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity le && !entity.equals(shooter)) {
                le.setFireTicks(60);
                plugin.abilities().dealTrueDamage(le, shooter, 4.4); // 11s cd -> ~2.2 hearts
            }
        }
        event.getEntity().remove();
    }

    /** Attacker-side multipliers + lifesteal + thorns. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // Ruin fireball: damage is handled as true AoE in onFireball — cancel the vanilla hit.
        if (plugin.abilities().isRuinFireball(event.getDamager())) {
            event.setCancelled(true);
            return;
        }

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
            // Ability true-damage routes back through here — don't layer melee talents on it.
            if (plugin.abilities().isAbilityDamage(aid)) return;

            PlayerData data = plugin.data().get(aid);
            double damage = event.getDamage();
            if (data.hasCard(Card.BERSERKER) && healthRatio(attacker) < 0.30) damage *= 1.30;
            if (data.hasCard(Card.EXECUTIONER) && healthRatio(victim) < 0.20) damage *= 1.50;
            if (projectile && data.hasCard(Card.SHARPSHOOTER)) damage *= 1.25;
            if (!projectile && data.hasCard(Card.CRIT) && ThreadLocalRandom.current().nextDouble() < 0.25) {
                damage *= 1.50;
                attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
            }
            // Unyielding Inferno: bonus damage to burning targets.
            if (!projectile && data.hasCard(Card.UNYIELDING_INFERNO) && victim.getFireTicks() > 0) {
                damage += 4.0;
            }
            event.setDamage(damage);

            if (!projectile && data.hasCard(Card.LIFESTEAL)) heal(attacker, damage * 0.10);

            if (!projectile) {
                if (data.hasCard(Card.IGNITE)) victim.setFireTicks(60);
                if (data.hasCard(Card.VENOM)) victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                if (data.hasCard(Card.FROSTBITE)) victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                if (data.hasCard(Card.CLEAVE)) cleave(attacker, victim, damage);
                // Retribution: spend the armed counter (every 3 hits taken) as +2 hearts of TRUE
                // damage. Applied next tick on top of this hit so it ignores armour/i-frames.
                if (data.hasCard(Card.RETRIBUTION) && retributionArmed.remove(aid)) {
                    LivingEntity v = victim;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!v.isDead() && v.isValid()) v.setHealth(Math.max(0.0, v.getHealth() - 4.0));
                    });
                    attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
                }
                if (data.hasCard(Card.UNYIELDING_INFERNO) && victim.getFireTicks() > 0) {
                    victim.setFireTicks(Math.max(victim.getFireTicks(), 100));
                    attacker.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.02);
                }
                // Spine Cutter: a back hit (facings aligned) deals bonus true damage.
                if (data.hasCard(Card.SPINE_CUTTER)
                        && attacker.getLocation().getDirection().dot(victim.getLocation().getDirection()) > 0.4) {
                    LivingEntity v = victim;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!v.isDead() && v.isValid()) v.setHealth(Math.max(0.0, v.getHealth() - 4.0));
                    });
                    attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }

    // Retribution: count hits taken; every 3rd arms a true-damage bonus for the next melee hit.
    private final java.util.Map<UUID, Integer> retributionHits = new java.util.HashMap<>();
    private final java.util.Set<UUID> retributionArmed = new java.util.HashSet<>();
    // Guard so Cleave's splash hits don't recursively trigger more cleaves.
    private final java.util.Set<UUID> cleaving = new java.util.HashSet<>();

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

        // Invincible while a draft menu is open (you're forced to pick).
        if (plugin.gui().hasOpen(player)) {
            event.setCancelled(true);
            return;
        }

        PlayerData data = plugin.data().get(player.getUniqueId());

        // Risky Moves: chance to fully negate an incoming hit.
        if (data.hasCard(Card.RISKY_MOVES) && ThreadLocalRandom.current().nextDouble() < 0.20) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
            return;
        }
        if ((data.hasCard(Card.FEATHER)
                || (data.hasCard(Card.KICK_OFF) && event.getDamage() <= 6.0))
                && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (data.hasCard(Card.JUGGERNAUT)) {
            event.setDamage(event.getDamage() * 0.85);
        }
        // Ghost: when hit, a chance to fully vanish (armor too) + Speed II for 3s.
        if (data.hasCard(Card.GHOST) && ThreadLocalRandom.current().nextDouble() < plugin.config().ghostChance()) {
            applyGhost(player, 60);
        }
        // Retribution: every 3rd hit taken arms a true-damage bonus for the next melee hit.
        if (data.hasCard(Card.RETRIBUTION)) {
            int hits = retributionHits.merge(player.getUniqueId(), 1, Integer::sum);
            if (hits >= 3) {
                retributionArmed.add(player.getUniqueId());
                retributionHits.put(player.getUniqueId(), 0);
            }
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
