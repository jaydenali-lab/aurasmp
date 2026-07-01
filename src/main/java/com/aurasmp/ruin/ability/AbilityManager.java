package com.aurasmp.ruin.ability;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.util.Cooldowns;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.scheduler.BukkitRunnable;
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
    // Players currently dealing ability damage — lets the combat listener skip melee talents.
    private final java.util.Set<UUID> abilityDamaging = new java.util.HashSet<>();
    // Players who shouldn't take fall damage for a window (Gale Step).
    private final Map<UUID, Long> noFallUntil = new HashMap<>();
    private final NamespacedKey fireballKey;

    public AbilityManager(RuinPlugin plugin) {
        this.plugin = plugin;
        this.fireballKey = new NamespacedKey(plugin, "fireball");
    }

    public Cooldowns cooldowns() { return cooldowns; }

    public boolean isAbilityDamage(UUID id) { return abilityDamaging.contains(id); }

    public boolean hasNoFall(UUID id) {
        Long until = noFallUntil.get(id);
        return until != null && System.currentTimeMillis() < until;
    }

    /** Suppress fall damage for the next 8s (used by all movement manifestations). */
    private void grantNoFall(Player player) {
        noFallUntil.put(player.getUniqueId(), System.currentTimeMillis() + 8_000);
    }

    public boolean isRuinFireball(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(fireballKey, PersistentDataType.BYTE);
    }

    /**
     * Deals normal (armour-affected) damage from an ability, credited to the source.
     * The guard flag keeps the combat listener from layering melee talents on it.
     */
    public void dealDamage(LivingEntity victim, Player source, double amount) {
        if (victim.isDead() || amount <= 0) return;
        UUID id = source.getUniqueId();
        abilityDamaging.add(id);
        try {
            victim.damage(amount, source);
        } finally {
            abilityDamaging.remove(id);
        }
    }

    /**
     * Deals true (armour-bypassing) damage from an ability. Triggers the hurt
     * flash/knockback, then next tick corrects the victim's health so exactly
     * {@code amount} is lost regardless of armour.
     */
    public void dealTrueDamage(LivingEntity victim, Player source, double amount) {
        if (victim.isDead() || amount <= 0) return;
        double before = victim.getHealth();
        victim.setNoDamageTicks(0);
        dealDamage(victim, source, amount); // animation + knockback (armour-reduced)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (victim.isDead()) return;
            double target = before - amount;
            if (target <= 0) {
                victim.setHealth(0);
            } else if (victim.getHealth() > target) {
                victim.setHealth(target); // strip the armour reduction back off
            }
        }, 1L);
    }

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
            case FIREBALL -> fireball(player);
            case UPDRAFT -> updraft(player);
            case GRAPPLE -> grapple(player);
            case BERSERK -> berserk(player);
            case SMOKE_BOMB -> smokeBomb(player);
            case LIGHTNING_STORM -> lightningStorm(player);
            case SANCTUARY -> sanctuary(player);
            case PLAGUE -> plague(player);
            case METEOR -> meteor(player);
            case DASH -> dash(player);
            case FLAME_GRAB -> flameGrab(player);
            case WILDFIRE -> wildfire(player);
            case FROSTDRAW_SPIKES -> frostdrawSpikes(player);
            case TUNDRA -> tundra(player);
            case SHOCK_SWORD -> shockSword(player);
            case GALVANIZE -> galvanize(player);
            case WIND_SLAM -> windSlam(player);
            case GALE_STEP -> galeStep(player);
            case PHASE_STRIKE -> phaseStrike(player);
            case ZELKOVA -> zelkova(player);
            case MOOK -> mook(player);
        }
    }

    // ---- individual abilities ----

    private void blink(Player player) {
        grantNoFall(player);
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
            Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.4).setY(0.5);
            target.setVelocity(push);
            dealDamage(target, player, 4.8); // 12s cd -> 2.4 hearts
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
            dealDamage(target, player, 5.6); // 14s cd -> 2.8 hearts
        }
        world.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.2f);
    }

    private void leap(Player player) {
        grantNoFall(player);
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
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
            dealDamage(target, player, 1.2); // applies Slow -> 1/5 damage
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
            dealDamage(target, player, 8.0);
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

    private void fireball(Player player) {
        SmallFireball fb = player.launchProjectile(SmallFireball.class,
                player.getEyeLocation().getDirection().multiply(1.4));
        fb.setIsIncendiary(false); // no block grief
        fb.getPersistentDataContainer().set(fireballKey, PersistentDataType.BYTE, (byte) 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
    }

    private void updraft(Player player) {
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 5, 1, 0.2, 1, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
        for (LivingEntity target : nearbyEnemies(player, 6.0)) {
            target.setVelocity(target.getVelocity().setY(1.3));
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 10, 0.2, 0.1, 0.2, 0.05);
        }
    }

    private void grapple(Player player) {
        grantNoFall(player);
        // Fire an arrow and ride it through the air until it lands.
        Arrow arrow = player.launchProjectile(Arrow.class, player.getEyeLocation().getDirection().multiply(2.5));
        arrow.setShooter(player);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setDamage(0.0); // harmless — you're riding it
        arrow.setInvisible(true);
        arrow.addPassenger(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                boolean landed = !arrow.isValid() || arrow.isDead()
                        || arrow.isInBlock() || arrow.isOnGround() || ticks > 120;
                if (landed) {
                    arrow.eject();
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                    if (arrow.isValid()) arrow.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 1L);
    }

    private void berserk(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0));
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 8, 0.4, 0.4, 0.4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1.2f);
    }

    private void smokeBomb(Player player) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1, 0), 60, 2, 1, 2, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_TNT_PRIMED, 1f, 1.4f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
        for (LivingEntity target : nearbyEnemies(player, 6.0)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
        }
    }

    private void lightningStorm(Player player) {
        World world = player.getWorld();
        int struck = 0;
        for (LivingEntity target : nearbyEnemies(player, 8.0)) {
            world.strikeLightningEffect(target.getLocation());
            dealDamage(target, player, 7.2); // 18s cd -> 3.6 hearts
            if (++struck >= 3) break;
        }
        if (struck == 0) world.strikeLightningEffect(player.getLocation());
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
    }

    private void sanctuary(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
    }

    private void plague(Player player) {
        player.getWorld().spawnParticle(Particle.ITEM_SLIME, player.getLocation().add(0, 1, 0), 30, 3, 0.5, 3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 0.8f);
        for (LivingEntity target : nearbyEnemies(player, 6.0)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 0));
        }
    }

    private void meteor(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 30.0);
        Location target = hit != null
                ? hit.getHitPosition().toLocation(world)
                : eye.add(eye.getDirection().multiply(20));
        world.spawnParticle(Particle.FLAME, target, 30, 1, 0.1, 1, 0.01);
        world.playSound(target, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            world.strikeLightningEffect(target);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, target, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, target, 60, 2, 1, 2, 0.05);
            world.playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
            for (Entity entity : world.getNearbyEntities(target, 4, 4, 4)) {
                if (entity instanceof LivingEntity le && !entity.equals(player)) {
                    le.setFireTicks(60);
                    dealDamage(le, player, 8.0);
                }
            }
        }, 20L);
    }

    private void dash(Player player) {
        grantNoFall(player);
        Vector dir = player.getEyeLocation().getDirection();
        dir.setY(Math.max(0.15, dir.getY() * 0.3));
        player.setVelocity(dir.normalize().multiply(1.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.1, 0.2, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
    }

    // ==== Deepwoken mantras ====

    private void flameGrab(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult res = world.rayTraceEntities(eye, dir, 6.0, 1.2,
                e -> e instanceof LivingEntity && !e.equals(player));
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            player.setVelocity(dir.clone().multiply(0.9).setY(0.25)); // whiff: short lunge
            return;
        }
        // Seize: lunge in, hoist the target into the air, then slam them down.
        player.setVelocity(dir.clone().multiply(0.6).setY(0.2));
        target.setFireTicks(100);
        target.setVelocity(new Vector(0, 1.15, 0));
        world.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.03);
        world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.3f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isDead() || !target.isValid()) return;
            target.setVelocity(new Vector(0, -2.2, 0)); // slam down
            world.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0.05);
            world.spawnParticle(Particle.LAVA, target.getLocation(), 8, 0.3, 0.2, 0.3, 0);
            world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);
            dealDamage(target, player, 5.2);
        }, 13L);
    }

    private void wildfire(Player player) {
        Location loc = player.getLocation();
        Vector dir = player.getEyeLocation().getDirection();
        loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.7f);
        for (double d = 1; d <= 6; d += 1) {
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(dir.clone().multiply(d)).add(0, 0.5, 0),
                    12, 0.6, 0.3, 0.6, 0.03);
        }
        for (LivingEntity target : cone(player, 6.0, 0.3)) {
            target.setFireTicks(100);
            dealDamage(target, player, 6.0);
        }
    }

    private void frostdrawSpikes(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 10.0);
        Location base = hit != null ? hit.getHitPosition().toLocation(world) : eye.add(eye.getDirection().multiply(7));
        Block ground = base.getBlock();
        world.playSound(base, Sound.BLOCK_GLASS_BREAK, 1f, 0.7f);
        world.spawnParticle(Particle.SNOWFLAKE, base, 50, 1.5, 1, 1.5, 0.03);

        // Raise temporary ice spikes (only into empty space) and restore them after 5s.
        java.util.List<BlockState> changed = new java.util.ArrayList<>();
        int[][] pattern = {{0, 0, 3}, {1, 0, 2}, {-1, 0, 2}, {0, 1, 2}, {0, -1, 2}, {1, 1, 1}, {-1, -1, 1}};
        for (int[] p : pattern) {
            for (int h = 0; h < p[2]; h++) {
                Block b = world.getBlockAt(ground.getX() + p[0], ground.getY() + h, ground.getZ() + p[1]);
                if (b.isPassable()) {
                    changed.add(b.getState());
                    b.setType(Material.PACKED_ICE, false);
                }
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (BlockState st : changed) st.update(true, false);
        }, 100L);

        // Impale + chill enemies caught in the spikes.
        for (Entity entity : world.getNearbyEntities(base, 3, 3, 3)) {
            if (entity instanceof LivingEntity le && !entity.equals(player)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                dealDamage(le, player, 0.96); // applies Slow -> 1/5
            }
        }
    }

    private void tundra(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 120, 4, 0.5, 4, 0.03);
        loc.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_PLACE, 1f, 0.8f);
        for (LivingEntity target : nearbyEnemies(player, 8.0)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 1));
            target.setFreezeTicks(140);
            dealDamage(target, player, 1.44); // applies Slow -> 1/5
        }
    }

    private void shockSword(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        // Draw an arcing slash made of cloud particles sweeping across the swing.
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = right.clone().crossProduct(dir).normalize();
        double reach = 5.0;
        for (double t = -1.0; t <= 1.0; t += 0.08) {
            // arc: forward reach with a curved sideways+vertical sweep
            double side = Math.sin(t * Math.PI / 2) * 1.6;
            double lift = (1 - t * t) * 0.9 - 0.2;
            for (double d = 1.5; d <= reach; d += 1.0) {
                Location p = eye.clone()
                        .add(dir.clone().multiply(d))
                        .add(right.clone().multiply(side * (d / reach)))
                        .add(up.clone().multiply(lift * (d / reach)));
                world.spawnParticle(Particle.CLOUD, p, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        world.spawnParticle(Particle.CLOUD, eye.clone().add(dir.clone().multiply(2.5)), 12, 0.4, 0.4, 0.4, 0.02);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);

        RayTraceResult res = world.rayTraceEntities(eye, dir, reach, 1.4,
                e -> e instanceof LivingEntity && !e.equals(player));
        if (res != null && res.getHitEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3));
            world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);
            world.playSound(target.getLocation(), Sound.ENTITY_ALLAY_HURT, 1f, 0.6f);
            dealDamage(target, player, 0.88); // applies Slow -> 1/5
        }
    }

    private void galvanize(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 120, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0));
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.4f);
    }

    private void windSlam(Player player) {
        grantNoFall(player);
        // Mace-style: leap up, then dive super fast and slam — damage scales with fall height.
        Vector v = player.getVelocity();
        player.setVelocity(new Vector(v.getX(), 1.2, v.getZ()));
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 4, 0.3, 0.2, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 0.9f);
        new BukkitRunnable() {
            int ticks = 0;
            boolean airborne = false;
            boolean diving = false;
            float maxFall = 0f;

            @Override
            public void run() {
                ticks++;
                if (!player.isOnline()) { cancel(); return; }
                if (!airborne && !player.isOnGround()) airborne = true;
                if (airborne) maxFall = Math.max(maxFall, player.getFallDistance());
                if (airborne && !player.isOnGround() && player.getVelocity().getY() <= 0.05) diving = true;
                if (diving && !player.isOnGround()) {
                    // slam down fast
                    player.setVelocity(new Vector(player.getVelocity().getX(), -1.8, player.getVelocity().getZ()));
                    player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
                if ((airborne && player.isOnGround()) || ticks > 80) {
                    windSlamImpact(player, maxFall);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 4L, 1L);
    }

    private void windSlamImpact(Player player, float fall) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1, 0.2, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 0.8f);
        // base ~2 hearts, + up to ~5.4 hearts the higher you leapt.
        double dmg = 4.0 + Math.min(fall, 12.0) * 0.9;
        for (LivingEntity target : nearbyEnemies(player, 5.0)) {
            Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5).setY(0.65);
            target.setVelocity(push);
            dealDamage(target, player, dmg);
        }
    }

    private void galeStep(Player player) {
        Vector dir = player.getEyeLocation().getDirection();
        dir.setY(Math.max(0.2, dir.getY() * 0.4));
        player.setVelocity(dir.normalize().multiply(1.8));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
        // No fall damage after the step (like a wind charge), for 8s.
        noFallUntil.put(player.getUniqueId(), System.currentTimeMillis() + 8_000);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 4, 0.3, 0.2, 0.3, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.1, 0.2, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 1.3f);
    }

    private void phaseStrike(Player player) {
        grantNoFall(player);
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult res = world.rayTraceEntities(eye, eye.getDirection(), 12.0, 1.0,
                e -> e instanceof LivingEntity && !e.equals(player));
        if (res != null && res.getHitEntity() instanceof LivingEntity target) {
            Location dest = target.getLocation().clone();
            Vector toPlayer = player.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
            if (toPlayer.lengthSquared() > 0.01) dest.add(toPlayer.normalize().multiply(1.2));
            dest.setDirection(target.getLocation().toVector().subtract(dest.toVector()));
            world.spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.4, 0.6, 0.4, 0.5);
            player.teleport(dest);
            world.spawnParticle(Particle.PORTAL, dest, 30, 0.4, 0.6, 0.4, 0.5);
            world.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            dealDamage(target, player, 4.8); // 12s
        } else {
            player.teleport(eye.add(eye.getDirection().multiply(6)));
            world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        }
    }

    private void zelkova(Player player) {
        zelkovaSlam(player, false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) zelkovaSlam(player, true);
        }, 12L);
    }

    private void zelkovaSlam(Player player, boolean stunning) {
        Location center = player.getLocation();
        World world = center.getWorld();
        world.spawnParticle(Particle.EXPLOSION, center, 2, 0.8, 0.1, 0.8, 0);
        world.spawnParticle(Particle.BLOCK, center.clone().add(0, 0.1, 0), 40, 1.2, 0.1, 1.2,
                Material.DIRT.createBlockData());
        world.playSound(center, Sound.ITEM_MACE_SMASH_GROUND, 1f, stunning ? 0.8f : 1.1f);
        // Break cobwebs in the area.
        int r = 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (b.getType() == Material.COBWEB) b.setType(Material.AIR, false);
                }
            }
        }
        for (LivingEntity target : nearbyEnemies(player, 4.5)) {
            dealTrueDamage(target, player, 4.0); // 2 hearts of true damage per slam
            if (stunning && target instanceof Player victim) stun(victim, 30);
        }
    }

    /** Locks a player's movement AND camera for {@code ticks} ticks by re-teleporting them. */
    private void stun(Player victim, int ticks) {
        Location lock = victim.getLocation().clone();
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0));
        victim.getWorld().playSound(lock, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (!victim.isOnline() || t > ticks) { cancel(); return; }
                victim.teleport(lock); // re-lock position + look direction each tick
                victim.setVelocity(new Vector(0, 0, 0));
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void mook(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult res = world.rayTraceEntities(eye, eye.getDirection(), 5.5, 1.0,
                e -> e instanceof LivingEntity && !e.equals(player));
        player.setVelocity(eye.getDirection().clone().setY(0.05).normalize().multiply(1.4)); // dash in
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) return;
        // Multi-slash: 4 hits totalling 12 damage, a cloud-particle slash on each.
        new BukkitRunnable() {
            int slashes = 0;

            @Override
            public void run() {
                slashes++;
                if (!player.isOnline() || target.isDead() || !target.isValid() || slashes > 4) { cancel(); return; }
                target.setNoDamageTicks(0); // bypass i-frames so every slash lands
                dealDamage(target, player, 3.0);
                Location at = target.getLocation().add(0, 1, 0);
                cloudSlash(world, at, slashes); // alternating diagonal cloud slash
                world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.3f + slashes * 0.1f);
            }
        }.runTaskTimer(plugin, 2L, 3L);
    }

    /** Draws a short diagonal slash of cloud particles through {@code center}; angle alternates. */
    private void cloudSlash(World world, Location center, int index) {
        // Alternate the slash angle each hit so it reads as a flurry.
        double angle = (index % 2 == 0) ? Math.PI / 4 : -Math.PI / 4;
        Vector axis = new Vector(Math.cos(angle), Math.sin(angle), 0);
        for (double s = -1.0; s <= 1.0; s += 0.15) {
            Location p = center.clone().add(axis.clone().multiply(s * 1.2));
            world.spawnParticle(Particle.CLOUD, p, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    /** Enemies within {@code radius} that fall inside the look-direction cone (dot > minDot). */
    private java.util.List<LivingEntity> cone(Player player, double radius, double minDot) {
        Vector dir = player.getEyeLocation().getDirection();
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity le : nearbyEnemies(player, radius)) {
            Vector to = le.getLocation().toVector().subtract(player.getLocation().toVector());
            if (to.lengthSquared() < 0.01 || dir.dot(to.normalize()) > minDot) out.add(le);
        }
        return out;
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
