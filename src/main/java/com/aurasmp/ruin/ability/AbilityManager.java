package com.aurasmp.ruin.ability;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.util.Cooldowns;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runs ability effects, enforces per-ability cooldowns and tracks ACTIVE windows. */
public final class AbilityManager {

    private static final double RADIUS = 5.0;

    private final RuinPlugin plugin;
    private final Cooldowns cooldowns = new Cooldowns();
    // Per-player, per-ability "self-buff active until" timestamps (for the HUD).
    private final Map<UUID, Map<Ability, Long>> activeUntil = new HashMap<>();

    public AbilityManager(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public Cooldowns cooldowns() { return cooldowns; }

    public boolean isActive(UUID id, Ability ability) {
        return activeRemainingMillis(id, ability) > 0;
    }

    public long activeRemainingMillis(UUID id, Ability ability) {
        Map<Ability, Long> m = activeUntil.get(id);
        if (m == null) return 0;
        Long until = m.get(ability);
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    public void clearActive(UUID id) {
        activeUntil.remove(id);
    }

    /** Checks cooldown, runs the ability, and records its ACTIVE window. */
    public void tryActivate(Player player, Ability ability) {
        UUID id = player.getUniqueId();
        if (!cooldowns.isReady(id, ability.name())) {
            // Feedback lives in the HUD; a soft "denied" note marks the failed cast.
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        activate(player, ability);
        cooldowns.set(id, ability.name(), ability.cooldownMillis());
        if (ability.hasActiveState()) {
            activeUntil.computeIfAbsent(id, k -> new HashMap<>())
                    .put(ability, System.currentTimeMillis() + ability.activeDurationMillis());
        }
    }

    private void activate(Player player, Ability ability) {
        switch (ability) {
            case BLINK -> blink(player);
            case SHOCKWAVE -> shockwave(player);
            case VANISH -> vanish(player);
            case BLOODTHIRST -> bloodthirst(player);
            case SMITE -> smite(player);
            case LEAP -> leap(player);
            case FROST_NOVA -> frostNova(player);
            case WITHER_TOUCH -> witherTouch(player);
            case AURA_BURST -> auraBurst(player);
            case MAGNETIZE -> magnetize(player);
        }
    }

    // ---- individual abilities ----

    private void blink(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 8.0);
        double distance = hit != null ? Math.max(0, hit.getHitPosition().distance(eye.toVector()) - 1) : 8.0;
        Location target = eye.add(eye.getDirection().multiply(distance));
        target.setX(Math.floor(target.getX()) + 0.5);
        target.setZ(Math.floor(target.getZ()) + 0.5);
        target.setPitch(player.getLocation().getPitch());
        target.setYaw(player.getLocation().getYaw());

        world.spawnParticle(Particle.PORTAL, player.getLocation(), 40, 0.3, 0.8, 0.3, 0.5);
        player.teleport(target);
        world.spawnParticle(Particle.PORTAL, target, 40, 0.3, 0.8, 0.3, 0.5);
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    private void shockwave(Player player) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 6, RADIUS / 2, 0.3, RADIUS / 2, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
        for (LivingEntity target : nearbyEnemies(player)) {
            target.damage(6.0, player);
            Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.4).setY(0.5);
            target.setVelocity(push);
        }
    }

    private void vanish(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
    }

    private void bloodthirst(Player player) {
        heal(player, 6.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 1));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.4, 0.6, 0.4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
    }

    private void smite(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult result = world.rayTraceEntities(eye, eye.getDirection(), 30.0, 1.0,
                e -> e instanceof LivingEntity && !e.equals(player));
        Location strike = result != null && result.getHitEntity() != null
                ? result.getHitEntity().getLocation()
                : eye.add(eye.getDirection().multiply(20));
        world.strikeLightningEffect(strike);
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.damage(6.0, player);
        }
        world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.2f);
    }

    private void leap(Player player) {
        Vector velocity = player.getLocation().getDirection().multiply(1.0).setY(1.0);
        player.setVelocity(velocity);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.4f);
    }

    private void frostNova(Player player) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 60, RADIUS / 2, 0.6, RADIUS / 2, 0.02);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        for (LivingEntity target : nearbyEnemies(player)) {
            target.damage(3.0, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
        }
    }

    private void witherTouch(Player player) {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 40, RADIUS / 2, 0.6, RADIUS / 2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.4f);
        for (LivingEntity target : nearbyEnemies(player)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1));
        }
    }

    private void auraBurst(Player player) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 50, RADIUS / 2, 0.6, RADIUS / 2,
                new Particle.DustOptions(Color.fromRGB(180, 0, 255), 2f));
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);
        for (LivingEntity target : nearbyEnemies(player)) {
            target.damage(8.0, player);
        }
    }

    private void magnetize(Player player) {
        Location center = player.getLocation();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.6f);
        for (LivingEntity target : nearbyEnemies(player, 8.0)) {
            Vector pull = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.2).setY(0.3);
            target.setVelocity(pull);
            target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.5);
        }
    }

    // ---- helpers ----

    private java.util.List<LivingEntity> nearbyEnemies(Player player) {
        return nearbyEnemies(player, RADIUS);
    }

    private java.util.List<LivingEntity> nearbyEnemies(Player player, double radius) {
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                out.add(living);
            }
        }
        return out;
    }

    private void heal(Player player, double amount) {
        double max = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }
}
