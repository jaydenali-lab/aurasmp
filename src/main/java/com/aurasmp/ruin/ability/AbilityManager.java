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
    // Players in a Riposte parry stance (until timestamp).
    private final Map<UUID, Long> riposteUntil = new HashMap<>();
    // 1.11.0 manifestation state (all until-timestamps / counters).
    private final Map<UUID, Long> silencedUntil = new HashMap<>();
    private final Map<UUID, Long> sunderedUntil = new HashMap<>();
    private final Map<UUID, Long> overchargeUntil = new HashMap<>();
    private final Map<UUID, Long> aegisUntil = new HashMap<>();
    private final Map<UUID, Long> afterimageUntil = new HashMap<>();
    private final Map<UUID, Integer> staticCharges = new HashMap<>();
    private final Map<UUID, Long> staticChargesUntil = new HashMap<>();
    private final Map<UUID, Location> warpBeacons = new HashMap<>();
    private final Map<UUID, Long> warpBeaconUntil = new HashMap<>();
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

    /** If the player is in a Riposte stance, consume it (one parry per cast). */
    public boolean consumeRiposte(UUID id) {
        Long until = riposteUntil.get(id);
        if (until == null || System.currentTimeMillis() >= until) return false;
        riposteUntil.remove(id);
        return true;
    }

    /** Drops transient per-player state on quit. Cooldowns intentionally survive relogs. */
    public void cleanup(UUID id) {
        noFallUntil.remove(id);
        riposteUntil.remove(id);
        activeUntil.remove(id);
        silencedUntil.remove(id);
        overchargeUntil.remove(id);
        aegisUntil.remove(id);
        afterimageUntil.remove(id);
        staticCharges.remove(id);
        staticChargesUntil.remove(id);
        warpBeacons.remove(id);
        warpBeaconUntil.remove(id);
    }

    private boolean activeNow(Map<UUID, Long> map, UUID id) {
        Long until = map.get(id);
        return until != null && System.currentTimeMillis() < until;
    }

    /** Sundered victims take +15% damage (checked by the combat listener). */
    public boolean isSundered(UUID id) { return activeNow(sunderedUntil, id); }

    /** Overcharged defenders zap melee attackers (combat listener). */
    public boolean isOvercharged(UUID id) { return activeNow(overchargeUntil, id); }

    /** Aegis wards cancel incoming projectiles (combat listener). */
    public boolean hasAegis(UUID id) { return activeNow(aegisUntil, id); }

    /** Afterimage: consume the one evasive blink if the window is live. */
    public boolean consumeAfterimage(UUID id) {
        if (!activeNow(afterimageUntil, id)) return false;
        afterimageUntil.remove(id);
        return true;
    }

    /** Battle Rush talent: cut every remaining manifestation cooldown in half. */
    public void halveCooldowns(UUID id) {
        for (Ability ability : Ability.values()) {
            long remaining = cooldowns.remainingMillis(id, ability.name());
            if (remaining > 0) cooldowns.set(id, ability.name(), remaining / 2);
        }
    }

    /** Static Charge: spend one empowered hit if any remain. */
    public boolean consumeStaticCharge(UUID id) {
        if (!activeNow(staticChargesUntil, id)) { staticCharges.remove(id); return false; }
        int left = staticCharges.getOrDefault(id, 0);
        if (left <= 0) return false;
        staticCharges.put(id, left - 1);
        return true;
    }

    /** Entity ray-trace clamped to line of sight — walls stop targeting. */
    private RayTraceResult rayTraceLos(Player player, double range, double size) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult blockHit = world.rayTraceBlocks(eye, eye.getDirection(), range);
        double max = blockHit != null ? blockHit.getHitPosition().distance(eye.toVector()) : range;
        if (max < 0.5) return null;
        return world.rayTraceEntities(eye, eye.getDirection(), max, size,
                e -> e instanceof LivingEntity && !e.equals(player)
                        && !(e instanceof org.bukkit.entity.ArmorStand));
    }

    /** Something abilities are allowed to hit: alive, not the caster, not decoration. */
    private boolean targetable(Entity entity, Player caster) {
        return entity instanceof LivingEntity && !entity.equals(caster)
                && !(entity instanceof org.bukkit.entity.ArmorStand);
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
            if (victim.isDead() || !victim.isValid()) return;
            double now = victim.getHealth();
            if (before - now <= 0.001) return; // hit was cancelled (god mode, protection) — no top-up
            double target = before - amount;
            if (now <= target) return; // armour didn't eat anything
            if (target > 0) {
                victim.setHealth(target); // strip the armour reduction back off
            } else {
                // Lethal: go through the damage pipeline so totems and death events work.
                victim.setNoDamageTicks(0);
                dealDamage(victim, source, Math.max(1000.0, now * 2));
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
        // Silenced players cannot cast at all.
        Long sealed = silencedUntil.get(id);
        if (sealed != null && System.currentTimeMillis() < sealed) {
            player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.6f);
            return;
        }
        if (!cooldowns.isReady(id, ability.name())) {
            // Feedback lives in the HUD; a soft "denied" note marks the failed cast.
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        activate(player, ability);
        long cooldown = ability.cooldownMillis();
        // Attunement talent: manifestation cooldowns are 15% shorter.
        if (plugin.data().get(id).hasCard(com.aurasmp.ruin.card.Card.ATTUNEMENT)) {
            cooldown = (long) (cooldown * 0.85);
        }
        cooldowns.set(id, ability.name(), cooldown);
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
            case WITHER_TOUCH -> witherTouch(player);
            case MAGNETIZE -> magnetize(player);
            case FIREBALL -> fireball(player);
            case UPDRAFT -> updraft(player);
            case GRAPPLE -> grapple(player);
            case BERSERK -> berserk(player);
            case SMOKE_BOMB -> smokeBomb(player);
            case SANCTUARY -> sanctuary(player);
            case METEOR -> meteor(player);
            case DASH -> dash(player);
            case FLAME_GRAB -> flameGrab(player);
            case WILDFIRE -> wildfire(player);
            case FROSTDRAW_SPIKES -> frostdrawSpikes(player);
            case TUNDRA -> tundra(player);
            case SHOCK_SWORD -> shockSword(player);
            case WIND_SLAM -> windSlam(player);
            case GALE_STEP -> galeStep(player);
            case PHASE_STRIKE -> phaseStrike(player);
            case ZELKOVA -> zelkova(player);
            case MOOK -> mook(player);
            case SHADOW_LANCE -> shadowLance(player);
            case ASTRAL_WIND -> astralWind(player);
            case BLOOD_PACT -> bloodPact(player);
            case RIPOSTE -> riposte(player);
            case ICE_BARRIER -> iceBarrier(player);
            case GUILLOTINE -> guillotine(player);
            case REWIND -> rewind(player);
            case SINGULARITY -> singularity(player);
            case DISPLACE -> displace(player);
            case MIASMA -> miasma(player);
            case CRYOSTASIS -> cryostasis(player);
            case AEGIS -> aegis(player);
            case OVERCHARGE -> overcharge(player);
            case HEX -> hex(player);
            case STATIC_CHARGE -> staticCharge(player);
            case GRASPING_VINES -> graspingVines(player);
            case AFTERIMAGE -> afterimage(player);
            case VOLLEY -> volley(player);
            case CHAKRAM -> chakram(player);
            case FISSURE -> fissure(player);
            case LIFEDRAIN -> lifedrain(player);
            case WARP_BEACON -> warpBeacon(player);
            case CANNONBALL -> cannonball(player);
            case SILENCE -> silence(player);
            case TORRENT -> torrent(player);
            case EMBER_MINE -> emberMine(player);
            case SONIC_SHRIEK -> sonicShriek(player);
            case LEVITATE -> levitate(player);
            case SUNDER -> sunder(player);
            case RALLY -> rally(player);
            case COCOON -> cocoon(player);
            case PURGE -> purge(player);
            case TRUE_SIGHT -> trueSight(player);
            case TETHER -> tether(player);
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
        target.subtract(0, 1.62, 0); // ray ends at eye height — drop back down to the feet
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
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01) push = new Vector(1, 0, 0); // overlapping: pick a direction
            target.setVelocity(push.normalize().multiply(1.4).setY(0.5));
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
        RayTraceResult result = rayTraceLos(player, 30.0, 1.0);
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

    private void witherTouch(Player player) {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 40, RADIUS / 2, 0.6, RADIUS / 2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.4f);
        for (LivingEntity target : nearbyEnemies(player)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1));
        }
    }

    private void magnetize(Player player) {
        Location center = player.getLocation();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.6f);
        for (LivingEntity target : nearbyEnemies(player, 8.0)) {
            Vector pull = center.toVector().subtract(target.getLocation().toVector());
            if (pull.lengthSquared() < 0.01) continue; // already on top of you
            target.setVelocity(pull.normalize().multiply(1.2).setY(0.3));
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 0));
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 8, 0.4, 0.4, 0.4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1.2f);
    }

    private void smokeBomb(Player player) {
        Location center = player.getLocation();
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1, 0), 60, 2, 1, 2, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_TNT_PRIMED, 1f, 1.4f);
        for (LivingEntity target : nearbyEnemies(player, 6.0)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
        }
    }

    private void sanctuary(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
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
                if (targetable(entity, player)) {
                    LivingEntity le = (LivingEntity) entity;
                    le.setFireTicks(60);
                    dealDamage(le, player, 6.0);
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
        RayTraceResult res = rayTraceLos(player, 6.0, 1.2);
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
            dealDamage(target, player, 5.0);
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
            // Only revert blocks that are still our ice — never overwrite player changes.
            for (BlockState st : changed) {
                if (st.getBlock().getType() == Material.PACKED_ICE) st.update(true, false);
            }
        }, 100L);

        // Impale + chill enemies caught in the spikes.
        for (Entity entity : world.getNearbyEntities(base, 3, 3, 3)) {
            if (targetable(entity, player) && entity instanceof LivingEntity le) {
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

        RayTraceResult res = rayTraceLos(player, reach, 1.4);
        if (res != null && res.getHitEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3));
            world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);
            world.playSound(target.getLocation(), Sound.ENTITY_ALLAY_HURT, 1f, 0.6f);
            dealDamage(target, player, 0.88); // applies Slow -> 1/5
        }
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
                if (!player.isOnline() || player.isDead()) { cancel(); return; }
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
        // base 1.5 hearts, + up to 3 hearts the higher you leapt.
        double dmg = 3.0 + Math.min(fall, 10.0) * 0.6;
        for (LivingEntity target : nearbyEnemies(player, 5.0)) {
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01) push = new Vector(1, 0, 0);
            target.setVelocity(push.normalize().multiply(1.5).setY(0.65));
            dealDamage(target, player, dmg);
        }
    }

    private void galeStep(Player player) {
        // A true blink-dash on wind: instantly step ~6 blocks forward (block-clipped),
        // unlike Dash's velocity burst. No fall damage after, like a wind charge.
        grantNoFall(player);
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().clone().setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        dir.normalize();
        RayTraceResult hit = world.rayTraceBlocks(eye, dir, 6.0);
        double distance = hit != null ? Math.max(0, hit.getHitPosition().distance(eye.toVector()) - 1) : 6.0;
        Location target = player.getLocation().add(dir.multiply(distance));
        world.spawnParticle(Particle.GUST, player.getLocation(), 3, 0.3, 0.2, 0.3, 0);
        world.spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.1, 0.2, 0.05);
        player.teleport(target);
        world.spawnParticle(Particle.CLOUD, target, 15, 0.2, 0.1, 0.2, 0.05);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
        world.playSound(target, Sound.ENTITY_BREEZE_JUMP, 1f, 1.3f);
    }

    private void phaseStrike(Player player) {
        grantNoFall(player);
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult res = rayTraceLos(player, 12.0, 1.0); // walls block the strike
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
            // Whiff: short blink forward, clamped to line of sight (no phasing through walls).
            RayTraceResult blockHit = world.rayTraceBlocks(eye, eye.getDirection(), 6.0);
            double distance = blockHit != null
                    ? Math.max(0, blockHit.getHitPosition().distance(eye.toVector()) - 1) : 6.0;
            Location dest = eye.add(eye.getDirection().multiply(distance)).subtract(0, 1.62, 0);
            player.teleport(dest);
            world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        }
    }

    private void zelkova(Player player) {
        zelkovaSlam(player, false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) zelkovaSlam(player, true);
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
            dealTrueDamage(target, player, 3.0); // 1.5 hearts of true damage per slam
            if (stunning && target instanceof Player victim) stun(victim, 20);
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
                if (!victim.isOnline() || victim.isDead() || t > ticks) { cancel(); return; }
                victim.teleport(lock); // re-lock position + look direction each tick
                victim.setVelocity(new Vector(0, 0, 0));
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void mook(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult res = rayTraceLos(player, 5.5, 1.0);
        player.setVelocity(eye.getDirection().clone().setY(0.05).normalize().multiply(1.4)); // dash in
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.MOOK, 4_000); // whiffed — most of the cooldown back
            return;
        }
        // Multi-slash: 4 hits totalling 12 damage, a cloud-particle slash on each.
        new BukkitRunnable() {
            int slashes = 0;

            @Override
            public void run() {
                slashes++;
                if (!player.isOnline() || player.isDead() || target.isDead() || !target.isValid()
                        || slashes > 4
                        || target.getWorld() != player.getWorld()
                        || target.getLocation().distanceSquared(player.getLocation()) > 36) { // broke off
                    cancel();
                    return;
                }
                target.setNoDamageTicks(0); // bypass i-frames so every slash lands
                dealDamage(target, player, 2.0);
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

    /**
     * Replaces the cooldown {@code tryActivate} is about to set with a shorter one
     * (must run a tick later, since tryActivate sets the full cooldown after activate()).
     */
    private void refundCooldown(Player player, Ability ability, long millis) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> cooldowns.set(player.getUniqueId(), ability.name(), millis));
    }

    // ==== 1.9.0 manifestations ====

    private void shadowLance(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.6f);
        java.util.Set<UUID> pierced = new java.util.HashSet<>();
        for (double d = 1.0; d <= 12.0; d += 0.5) {
            Location p = eye.clone().add(dir.clone().multiply(d));
            if (!p.getBlock().isPassable()) break;
            world.spawnParticle(Particle.SQUID_INK, p, 2, 0.05, 0.05, 0.05, 0.01);
            world.spawnParticle(Particle.SOUL, p, 1, 0.05, 0.05, 0.05, 0.005);
            for (Entity e : world.getNearbyEntities(p, 0.9, 0.9, 0.9)) {
                if (targetable(e, player) && e instanceof LivingEntity le && pierced.add(e.getUniqueId())) {
                    dealDamage(le, player, 4.0); // pierces everyone in the line
                }
            }
        }
    }

    private void astralWind(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.4f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (!player.isOnline() || player.isDead() || ticks > 100) { cancel(); return; }
                Location center = player.getLocation();
                center.getWorld().spawnParticle(Particle.GUST, center, 2, 1.5, 0.5, 1.5, 0);
                for (LivingEntity target : nearbyEnemies(player, 4.0)) {
                    Vector push = target.getLocation().toVector().subtract(center.toVector());
                    if (push.lengthSquared() < 0.01) push = new Vector(1, 0, 0);
                    target.setVelocity(push.normalize().multiply(0.7).setY(0.15));
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void bloodPact(Player player) {
        if (player.getHealth() <= 5.0) {
            // Too hurt to pay the price — soft fail, give the cooldown back.
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            refundCooldown(player, Ability.BLOOD_PACT, 1_000);
            return;
        }
        player.setHealth(player.getHealth() - 4.0);
        UUID id = player.getUniqueId();
        for (Ability other : Ability.values()) {
            if (other != Ability.BLOOD_PACT) cooldowns.set(id, other.name(), 0);
        }
        Location loc = player.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 40, 0.4, 0.6, 0.4,
                new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.6f));
        loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 10, 0.3, 0.4, 0.3, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.2f);
    }

    private void riposte(Player player) {
        riposteUntil.put(player.getUniqueId(), System.currentTimeMillis() + 1_500);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1.4f);
    }

    /** The parry payoff — called by the combat listener when a stance eats a hit. */
    public void riposteCounter(Player player, LivingEntity attacker) {
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f);
        world.spawnParticle(Particle.SWEEP_ATTACK, attacker.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0);
        Vector away = attacker.getLocation().toVector().subtract(player.getLocation().toVector());
        if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
        attacker.setVelocity(away.normalize().multiply(1.0).setY(0.35));
        dealDamage(attacker, player, 5.0);
    }

    private void iceBarrier(Player player) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().clone().setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        Location base = player.getLocation().add(dir.clone().multiply(2.5));
        java.util.List<BlockState> changed = new java.util.ArrayList<>();
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h <= 2; h++) {
                Block b = world.getBlockAt(
                        base.getBlockX() + (int) Math.round(right.getX() * w),
                        base.getBlockY() + h,
                        base.getBlockZ() + (int) Math.round(right.getZ() * w));
                if (b.isPassable()) {
                    changed.add(b.getState());
                    b.setType(Material.PACKED_ICE, false);
                }
            }
        }
        world.spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, 1, 0), 40, 1.5, 1, 1.5, 0.02);
        world.playSound(base, Sound.BLOCK_GLASS_PLACE, 1f, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Only revert blocks that are still our ice — never overwrite player changes.
            for (BlockState st : changed) {
                if (st.getBlock().getType() == Material.PACKED_ICE) st.update(true, false);
            }
            world.playSound(base, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
        }, 100L);
    }

    private void guillotine(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.GUILLOTINE, 4_000);
            return;
        }
        double max = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        double missing = Math.max(0, max - target.getHealth());
        Location at = target.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.SWEEP_ATTACK, at, 3, 0.4, 0.4, 0.4, 0);
        world.spawnParticle(Particle.CRIT, at, 15, 0.4, 0.4, 0.4, 0.2);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.6f);
        dealDamage(target, player, 3.0 + missing * 0.20); // execute: scales with missing health
    }

    private void rewind(Player player) {
        Location mark = player.getLocation().clone();
        double healthMark = player.getHealth();
        World world = player.getWorld();
        world.spawnParticle(Particle.REVERSE_PORTAL, mark.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.02);
        world.playSound(mark, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 1.4f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            world.spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.02);
            player.teleport(mark);
            double max = player.getAttribute(Attribute.MAX_HEALTH) != null
                    ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            if (player.getHealth() < healthMark) {
                // Undo damage taken since the mark, capped at 4 hearts of healing.
                player.setHealth(Math.min(max, Math.min(healthMark, player.getHealth() + 8.0)));
            }
            world.spawnParticle(Particle.REVERSE_PORTAL, mark.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.02);
            world.playSound(mark, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
        }, 60L);
    }

    private void singularity(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 12.0);
        Location point = hit != null
                ? hit.getHitPosition().toLocation(world).add(0, 0.5, 0)
                : eye.add(eye.getDirection().multiply(8));
        world.playSound(point, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (ticks > 60) { cancel(); return; }
                world.spawnParticle(Particle.PORTAL, point, 25, 0.3, 0.3, 0.3, 0.6);
                world.spawnParticle(Particle.END_ROD, point, 3, 0.1, 0.1, 0.1, 0.02);
                for (Entity e : world.getNearbyEntities(point, 6, 6, 6)) {
                    if (targetable(e, player) && e instanceof LivingEntity le) {
                        Vector pull = point.toVector().subtract(le.getLocation().toVector());
                        if (pull.lengthSquared() < 1.0) {
                            dealDamage(le, player, 0.6); // grinding at the core
                        } else {
                            le.setVelocity(pull.normalize().multiply(0.55).setY(Math.max(0.1, pull.getY() * 0.1)));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void displace(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 10.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.DISPLACE, 3_000);
            return;
        }
        Location mine = player.getLocation().clone();
        Location theirs = target.getLocation().clone();
        // Keep each one's own facing after the swap.
        Location toTheirs = theirs.clone();
        toTheirs.setYaw(mine.getYaw());
        toTheirs.setPitch(mine.getPitch());
        Location toMine = mine.clone();
        toMine.setYaw(theirs.getYaw());
        toMine.setPitch(theirs.getPitch());
        world.spawnParticle(Particle.PORTAL, mine, 30, 0.3, 0.6, 0.3, 0.5);
        world.spawnParticle(Particle.PORTAL, theirs, 30, 0.3, 0.6, 0.3, 0.5);
        player.teleport(toTheirs);
        target.teleport(toMine);
        world.playSound(toTheirs, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);
        world.playSound(toMine, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.9f);
    }

    private void miasma(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 12.0);
        Location at = hit != null
                ? hit.getHitPosition().toLocation(world)
                : eye.add(eye.getDirection().multiply(8));
        world.playSound(at, Sound.ENTITY_WITCH_THROW, 1f, 0.7f);
        org.bukkit.entity.AreaEffectCloud cloud =
                world.spawn(at, org.bukkit.entity.AreaEffectCloud.class);
        cloud.setSource(player);
        cloud.setRadius(3.0f);
        cloud.setDuration(100); // 5s
        cloud.setWaitTime(5);
        cloud.setRadiusOnUse(0f);
        cloud.setColor(Color.fromRGB(70, 130, 40));
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 80, 1), true);
    }


    // ==== 1.11.0 manifestations ====

    private void cryostasis(Player player) {
        World world = player.getWorld();
        Location lock = player.getLocation().clone();
        player.setInvulnerable(true);
        world.playSound(lock, Sound.BLOCK_GLASS_PLACE, 1f, 0.6f);
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (!player.isOnline() || player.isDead() || t > 50) {
                    player.setInvulnerable(false);
                    if (player.isOnline()) {
                        world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.9f);
                    }
                    cancel();
                    return;
                }
                Location here = lock.clone();
                here.setYaw(player.getLocation().getYaw());     // frozen in place,
                here.setPitch(player.getLocation().getPitch()); // camera stays free
                player.teleport(here);
                player.setVelocity(new Vector(0, 0, 0));
                world.spawnParticle(Particle.SNOWFLAKE, lock.clone().add(0, 1, 0), 4, 0.3, 0.6, 0.3, 0.01);
                world.spawnParticle(Particle.BLOCK, lock.clone().add(0, 1, 0), 3, 0.3, 0.7, 0.3,
                        Material.PACKED_ICE.createBlockData());
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void aegis(Player player) {
        aegisUntil.put(player.getUniqueId(), System.currentTimeMillis() + 4_000);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 25, 0.5, 0.8, 0.5, 0.03);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.8f);
    }

    private void overcharge(Player player) {
        overchargeUntil.put(player.getUniqueId(), System.currentTimeMillis() + 6_000);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.6f);
    }

    /** The retaliation zap — called by the combat listener when an overcharged player is hit. */
    public void overchargeZap(Player defender, LivingEntity attacker) {
        World world = defender.getWorld();
        world.spawnParticle(Particle.ELECTRIC_SPARK, attacker.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        world.playSound(attacker.getLocation(), Sound.ENTITY_BEE_STING, 1f, 1.6f);
        dealDamage(attacker, defender, 2.0);
    }

    private void hex(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 10.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.HEX, 3_000);
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
        world.spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.02);
        world.playSound(target.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 0.7f);
        dealDamage(target, player, 0.6);
    }

    private void staticCharge(Player player) {
        UUID id = player.getUniqueId();
        staticCharges.put(id, 3);
        staticChargesUntil.put(id, System.currentTimeMillis() + 8_000);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 1f, 1.4f);
    }

    private void graspingVines(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.GRASPING_VINES, 3_000);
            return;
        }
        Location root = target.getLocation().clone();
        world.playSound(root, Sound.BLOCK_VINE_BREAK, 1f, 0.6f);
        dealDamage(target, player, 0.6);
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (t > 40 || target.isDead() || !target.isValid()) { cancel(); return; }
                Location here = root.clone();
                here.setYaw(target.getLocation().getYaw());     // rooted, not stunned —
                here.setPitch(target.getLocation().getPitch()); // they can still look and swing
                target.teleport(here);
                target.setVelocity(new Vector(0, 0, 0));
                world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, root.clone().add(0, 0.5, 0), 3, 0.3, 0.4, 0.3, 0);
                world.spawnParticle(Particle.COMPOSTER, root.clone().add(0, 0.2, 0), 3, 0.3, 0.2, 0.3, 0);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void afterimage(Player player) {
        afterimageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 4_000);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8f, 1.4f);
    }

    /** The evasive blink — called by the combat listener when a hit lands during the window. */
    public void afterimageBlink(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector back = eye.getDirection().clone().multiply(-1).setY(0);
        if (back.lengthSquared() < 0.01) back = new Vector(1, 0, 0);
        back.normalize();
        RayTraceResult blockHit = world.rayTraceBlocks(eye, back, 3.5);
        double distance = blockHit != null
                ? Math.max(0, blockHit.getHitPosition().distance(eye.toVector()) - 1) : 3.5;
        world.spawnParticle(Particle.PORTAL, player.getLocation(), 25, 0.3, 0.6, 0.3, 0.4);
        player.teleport(player.getLocation().add(back.multiply(distance)));
        world.spawnParticle(Particle.PORTAL, player.getLocation(), 25, 0.3, 0.6, 0.3, 0.4);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
    }

    private void volley(Player player) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection();
        for (int i = -2; i <= 2; i++) {
            Vector spread = dir.clone().rotateAroundY(Math.toRadians(i * 8));
            Arrow arrow = player.launchProjectile(Arrow.class, spread.multiply(2.2));
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setDamage(2.0);
        }
        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.9f);
        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
    }

    private void chakram(Player player) {
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().clone().setY(0).normalize();
        chakramPass(player, player.getLocation().clone().add(0, 1, 0), dir);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            // return pass sweeps back to wherever the caster is NOW
            Location from = player.getLocation().clone().add(0, 1, 0).add(dir.clone().multiply(8));
            Vector back = player.getLocation().clone().add(0, 1, 0).toVector().subtract(from.toVector());
            if (back.lengthSquared() < 0.01) back = dir.clone().multiply(-1);
            chakramPass(player, from, back.normalize());
            world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 1.2f);
        }, 10L);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.3f);
    }

    private void chakramPass(Player player, Location start, Vector dir) {
        World world = player.getWorld();
        java.util.Set<UUID> cut = new java.util.HashSet<>();
        for (double d = 0.5; d <= 8.0; d += 0.5) {
            Location at = start.clone().add(dir.clone().multiply(d));
            if (!at.getBlock().isPassable()) break;
            world.spawnParticle(Particle.SWEEP_ATTACK, at, 1, 0.05, 0.05, 0.05, 0);
            for (Entity e : world.getNearbyEntities(at, 0.9, 0.9, 0.9)) {
                if (targetable(e, player) && cut.add(e.getUniqueId())) {
                    LivingEntity le = (LivingEntity) e;
                    le.setNoDamageTicks(0); // both passes should land
                    dealDamage(le, player, 3.0);
                }
            }
        }
    }

    private void fissure(Player player) {
        World world = player.getWorld();
        Vector flat = player.getEyeLocation().getDirection().clone().setY(0);
        if (flat.lengthSquared() < 0.01) flat = new Vector(1, 0, 0);
        Vector step = flat.normalize();
        Location base = player.getLocation().clone();
        java.util.Set<UUID> hit = new java.util.HashSet<>();
        world.playSound(base, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
        new BukkitRunnable() {
            int d = 0;

            @Override
            public void run() {
                d++;
                if (d > 8) { cancel(); return; }
                Location at = base.clone().add(step.clone().multiply(d));
                world.spawnParticle(Particle.BLOCK, at.clone().add(0, 0.2, 0), 25, 0.4, 0.2, 0.4,
                        Material.DIRT.createBlockData());
                world.playSound(at, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.8f, 0.7f + d * 0.05f);
                for (Entity e : world.getNearbyEntities(at, 1.2, 1.5, 1.2)) {
                    if (targetable(e, player) && hit.add(e.getUniqueId())) {
                        LivingEntity le = (LivingEntity) e;
                        le.setVelocity(le.getVelocity().setY(0.45));
                        dealDamage(le, player, 4.0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void lifedrain(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.LIFEDRAIN, 4_000);
            return;
        }
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.6f);
        new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                pulses++;
                if (pulses > 3 || !player.isOnline() || player.isDead()
                        || target.isDead() || !target.isValid()
                        || target.getWorld() != player.getWorld()
                        || target.getLocation().distanceSquared(player.getLocation()) > 100) {
                    cancel();
                    return;
                }
                target.setNoDamageTicks(0);
                dealDamage(target, player, 1.0);
                heal(player, 1.0);
                // a trail of soul particles streaming from them to you
                Vector to = player.getLocation().add(0, 1, 0).toVector()
                        .subtract(target.getLocation().add(0, 1, 0).toVector());
                for (double f = 0; f <= 1.0; f += 0.2) {
                    Location at = target.getLocation().add(0, 1, 0).add(to.clone().multiply(f));
                    world.spawnParticle(Particle.SCULK_SOUL, at, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void warpBeacon(Player player) {
        UUID id = player.getUniqueId();
        World world = player.getWorld();
        Location stored = warpBeacons.get(id);
        Long until = warpBeaconUntil.get(id);
        if (stored != null && until != null && System.currentTimeMillis() < until
                && stored.getWorld() == world) {
            // Second cast: warp back, full cooldown applies.
            world.spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.4, 0.8, 0.4, 0.5);
            player.teleport(stored);
            world.spawnParticle(Particle.PORTAL, stored, 30, 0.4, 0.8, 0.4, 0.5);
            world.playSound(stored, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
            warpBeacons.remove(id);
            warpBeaconUntil.remove(id);
            return;
        }
        // First cast: place the beacon; nearly free — the cost is paid on the warp.
        warpBeacons.put(id, player.getLocation().clone());
        warpBeaconUntil.put(id, System.currentTimeMillis() + 10_000);
        world.spawnParticle(Particle.END_ROD, player.getLocation(), 20, 0.3, 0.5, 0.3, 0.02);
        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.5f);
        refundCooldown(player, Ability.WARP_BEACON, 1_000);
    }

    private void cannonball(Player player) {
        grantNoFall(player);
        World world = player.getWorld();
        Vector dir = player.getEyeLocation().getDirection().clone();
        player.setVelocity(dir.setY(Math.max(0.7, dir.getY())).normalize().multiply(1.5));
        world.spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 15, 0.3, 0.2, 0.3, 0.03);
        world.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1.2f);
        new BukkitRunnable() {
            int ticks = 0;
            boolean airborne = false;

            @Override
            public void run() {
                ticks++;
                if (!player.isOnline() || player.isDead() || ticks > 80) { cancel(); return; }
                if (!airborne && !player.isOnGround()) airborne = true;
                if (airborne && player.isOnGround()) {
                    Location center = player.getLocation();
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);
                    for (LivingEntity target : nearbyEnemies(player, 4.0)) {
                        Vector push = target.getLocation().toVector().subtract(center.toVector());
                        if (push.lengthSquared() < 0.01) push = new Vector(1, 0, 0);
                        target.setVelocity(push.normalize().multiply(1.0).setY(0.5));
                        dealDamage(target, player, 4.0);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 4L, 1L);
    }

    private void silence(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 10.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.SILENCE, 4_000);
            return;
        }
        if (target instanceof Player victim) {
            silencedUntil.put(victim.getUniqueId(), System.currentTimeMillis() + 4_000);
            victim.playSound(victim.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.2f);
        }
        world.spawnParticle(Particle.SCULK_CHARGE_POP, target.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        world.playSound(target.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.7f, 1.4f);
        dealDamage(target, player, 0.8);
    }

    private void torrent(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.TORRENT, 3_000);
            return;
        }
        Vector away = player.getEyeLocation().getDirection().clone().setY(0.15).normalize();
        target.setVelocity(away.multiply(2.2).setY(0.4));
        target.setFireTicks(0);
        world.spawnParticle(Particle.SPLASH, target.getLocation().add(0, 1, 0), 60, 0.4, 0.6, 0.4, 0.2);
        world.spawnParticle(Particle.BUBBLE_POP, target.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);
        world.playSound(target.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 0.8f);
        dealDamage(target, player, 1.6);
    }

    private void emberMine(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), 6.0);
        Location at = (hit != null ? hit.getHitPosition().toLocation(world) : player.getLocation()).clone();
        world.spawnParticle(Particle.SMALL_FLAME, at, 8, 0.3, 0.1, 0.3, 0.01);
        world.playSound(at, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 0.8f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (ticks > 600) { cancel(); return; } // fizzles after 30s
                world.spawnParticle(Particle.SMALL_FLAME, at, 1, 0.15, 0.05, 0.15, 0);
                for (Entity e : world.getNearbyEntities(at, 1.8, 1.5, 1.8)) {
                    if (!targetable(e, player)) continue;
                    world.spawnParticle(Particle.EXPLOSION, at, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.FLAME, at, 40, 1, 0.5, 1, 0.05);
                    world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.3f);
                    for (Entity e2 : world.getNearbyEntities(at, 2.5, 2, 2.5)) {
                        if (targetable(e2, player)) {
                            LivingEntity le = (LivingEntity) e2;
                            le.setFireTicks(60);
                            dealDamage(le, player, 5.0);
                        }
                    }
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 10L, 5L);
    }

    private void sonicShriek(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 10.0, 1.2);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.3f);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) return;
        Vector to = target.getLocation().toVector().subtract(player.getLocation().toVector());
        for (double f = 0.2; f <= 1.0; f += 0.15) {
            world.spawnParticle(Particle.SONIC_BOOM,
                    player.getEyeLocation().add(to.clone().multiply(f)), 1, 0, 0, 0, 0);
        }
        Vector fling = to.clone().setY(0.2);
        if (fling.lengthSquared() < 0.01) fling = new Vector(1, 0, 0);
        target.setVelocity(fling.normalize().multiply(2.5).setY(0.5));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        dealDamage(target, player, 1.2);
    }

    private void levitate(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.LEVITATE, 3_000);
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1));
        world.spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.03);
        world.playSound(target.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 1.3f);
        dealDamage(target, player, 0.6);
    }

    private void sunder(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 6.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.SUNDER, 3_000);
            return;
        }
        sunderedUntil.put(target.getUniqueId(), System.currentTimeMillis() + 5_000);
        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.15);
        world.playSound(target.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.4f);
        dealDamage(target, player, 1.5);
    }

    private void rally(Player player) {
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 0.7f, 1.2f);
        world.spawnParticle(Particle.NOTE, player.getLocation().add(0, 2, 0), 10, 0.6, 0.4, 0.6, 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (e instanceof Player ally) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
                ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                world.spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0), 3, 0.3, 0.2, 0.3, 0);
            }
        }
    }

    private void cocoon(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 8.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.COCOON, 3_000);
            return;
        }
        Block feet = target.getLocation().getBlock();
        Block head = feet.getRelative(org.bukkit.block.BlockFace.UP);
        java.util.List<BlockState> changed = new java.util.ArrayList<>();
        for (Block b : new Block[]{feet, head}) {
            if (b.isPassable() && b.getType() != Material.COBWEB) {
                changed.add(b.getState());
                b.setType(Material.COBWEB, false);
            }
        }
        world.playSound(target.getLocation(), Sound.BLOCK_COBWEB_PLACE, 1f, 0.8f);
        world.spawnParticle(Particle.ITEM_COBWEB, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        dealDamage(target, player, 0.6);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Only clear webs still standing — Zelkova may already have smashed them.
            for (BlockState st : changed) {
                if (st.getBlock().getType() == Material.COBWEB) st.update(true, false);
            }
        }, 80L);
    }

    private void purge(Player player) {
        java.util.List<PotionEffectType> bad = java.util.List.of(
                PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
                PotionEffectType.MINING_FATIGUE, PotionEffectType.LEVITATION, PotionEffectType.DARKNESS,
                PotionEffectType.GLOWING, PotionEffectType.HUNGER, PotionEffectType.UNLUCK);
        for (PotionEffectType type : bad) player.removePotionEffect(type);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 40, 0.4, 0.7, 0.4, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1.5f);
    }

    private void trueSight(Player player) {
        World world = player.getWorld();
        int revealed = 0;
        for (Entity e : player.getNearbyEntities(20, 20, 20)) {
            if (e instanceof Player enemy) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                revealed++;
            }
        }
        world.spawnParticle(Particle.END_ROD, player.getEyeLocation(), 15, 0.4, 0.3, 0.4, 0.05);
        world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.8f);
        if (revealed == 0) refundCooldown(player, Ability.TRUE_SIGHT, 5_000);
    }

    private void tether(Player player) {
        World world = player.getWorld();
        RayTraceResult res = rayTraceLos(player, 10.0, 1.2);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity target)) {
            refundCooldown(player, Ability.TETHER, 4_000);
            return;
        }
        Location anchor = target.getLocation().clone();
        world.playSound(anchor, Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.8f);
        dealDamage(target, player, 0.6);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (ticks > 80 || target.isDead() || !target.isValid()
                        || target.getWorld() != world) {
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.CRIT, anchor.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0);
                if (target.getLocation().distanceSquared(anchor) > 36) {
                    Vector pull = anchor.toVector().subtract(target.getLocation().toVector());
                    target.setVelocity(pull.normalize().multiply(1.2).setY(Math.max(0.15, pull.getY() * 0.05)));
                    world.playSound(target.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.6f, 1.4f);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
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
            if (targetable(entity, player)) {
                out.add((LivingEntity) entity);
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
