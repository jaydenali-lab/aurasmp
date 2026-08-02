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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
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
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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
            killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0));
        }
        // Rampage: each kill banks a +5% damage stack for 20s (max 3).
        if (data.hasCard(Card.RAMPAGE)) {
            java.util.ArrayDeque<Long> stacks =
                    rampage.computeIfAbsent(killer.getUniqueId(), k -> new java.util.ArrayDeque<>());
            stacks.addLast(System.currentTimeMillis() + 20_000);
            while (stacks.size() > 3) stacks.removeFirst();
        }
        // Headhunter: player kills grant 2 absorption hearts for 30s.
        if (data.hasCard(Card.HEADHUNTER) && dead instanceof Player) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 1));
        }
        // Battle Rush: player kills refund half of every manifestation cooldown.
        if (data.hasCard(Card.BATTLE_RUSH) && dead instanceof Player) {
            plugin.abilities().halveCooldowns(killer.getUniqueId());
            killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.8f);
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
            if (entity instanceof LivingEntity le && !entity.equals(shooter)
                    && !(entity instanceof org.bukkit.entity.ArmorStand)
                    && !(entity instanceof Player other
                        && plugin.data().get(shooter.getUniqueId()).isTrusted(other.getUniqueId()))) {
                le.setFireTicks(60);
                plugin.abilities().dealDamage(le, shooter, 4.4); // 11s cd -> ~2.2 hearts
            }
        }
        event.getEntity().remove();
    }

    /**
     * Attacker-side multipliers + on-hit talents. Runs at HIGHEST so victim-side
     * cancels (Riposte, Risky Moves, draft protection) resolve first — cancelled
     * hits must not trigger attacker talents.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            // Cleave splash also re-enters here — splash must not re-trigger the talent suite.
            if (cleaving.contains(aid)) return;

            PlayerData data = plugin.data().get(aid);
            double damage = event.getDamage();
            if (data.hasCard(Card.BERSERKER) && healthRatio(attacker) < 0.30) damage *= 1.07;
            if (data.hasCard(Card.EXECUTIONER) && healthRatio(victim) < 0.20) damage *= 1.30;
            if (projectile && data.hasCard(Card.SHARPSHOOTER)) damage *= 1.05;
            if (!projectile && data.hasCard(Card.CRIT) && ThreadLocalRandom.current().nextDouble() < 0.25) {
                damage *= 1.20;
                attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
            }
            // Unyielding Inferno: bonus damage to burning targets.
            if (!projectile && data.hasCard(Card.UNYIELDING_INFERNO) && victim.getFireTicks() > 0) {
                damage += 3.0;
            }
            // 1.9.0 conditional melee talents.
            if (!projectile) {
                if (data.hasCard(Card.FIRST_STRIKE) && healthRatio(victim) >= 0.999) damage *= 1.06;
                if (data.hasCard(Card.PREDATOR) && isDebuffed(victim)) damage *= 1.06;
                if (data.hasCard(Card.DUELIST) && nearbyEnemyCount(attacker) == 1) damage *= 1.04;
                if (data.hasCard(Card.AERIAL) && !attacker.isOnGround()) damage *= 1.06;
                if (data.hasCard(Card.WARPATH) && attacker.isSprinting()) damage *= 1.04;
                if (data.hasCard(Card.NIGHT_STALKER)
                        && victim.getLocation().getBlock().getLightLevel() < 7) damage *= 1.06;
                if (data.hasCard(Card.GIANT_SLAYER) && victim.getHealth() > attacker.getHealth()) damage *= 1.05;
                if (data.hasCard(Card.SHIELDBREAKER) && victim.getAbsorptionAmount() > 0) damage *= 1.07;
                if (data.hasCard(Card.VENDETTA)) {
                    Grudge grudge = grudges.get(aid);
                    if (grudge != null && grudge.enemy().equals(victim.getUniqueId())
                            && System.currentTimeMillis() < grudge.until()) {
                        damage *= 1.07;
                    }
                }
                if (data.hasCard(Card.COMBO)) {
                    damage *= 1 + 0.02 * comboStacks(aid, victim.getUniqueId());
                }
                // Weapon talents: bonus while holding the matching weapon.
                if (data.hasCard(Card.FENCER) && holdingWeapon(attacker, "_SWORD")) damage *= 1.07;
                if (data.hasCard(Card.SPEARHEAD) && holdingWeapon(attacker, "SPEAR")) damage *= 1.10;
                if (data.hasCard(Card.SKEWER) && holdingWeapon(attacker, "SPEAR")
                        && attacker.getLocation().distanceSquared(victim.getLocation()) >= 3.5 * 3.5) {
                    damage += 1.5; // poke from spear range
                }
                if (data.hasCard(Card.RAMPAGE)) {
                    damage *= 1 + 0.02 * rampageStacks(aid);
                }
            }
            event.setDamage(damage);

            if (!projectile && data.hasCard(Card.LIFESTEAL)) heal(attacker, damage * 0.10);
            if (!projectile && data.hasCard(Card.VAMPIRIC)) heal(attacker, damage * 0.20);

            if (!projectile) {
                if (data.hasCard(Card.IGNITE)) victim.setFireTicks(60);
                if (data.hasCard(Card.VENOM)) victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                if (data.hasCard(Card.FROSTBITE)) victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                // Mangle: the victim heals 50% less for 5s (see onRegain).
                if (data.hasCard(Card.MANGLE)) {
                    mangledUntil.put(victim.getUniqueId(), System.currentTimeMillis() + 5_000);
                }
                // Skirmisher: hitting grants a short burst of speed to stick to the target.
                if (data.hasCard(Card.SKIRMISHER)) {
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0));
                }
                // Bloodhound: players you hit glow briefly.
                if (data.hasCard(Card.BLOODHOUND) && victim instanceof Player) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                }
                // Concussive Blows: every 5th axe hit stuns (Zelkova-style) for 1.5s.
                if (data.hasCard(Card.CONCUSSIVE_BLOWS) && holdingWeapon(attacker, "_AXE")) {
                    int hits = axeHits.merge(aid, 1, Integer::sum);
                    if (hits >= 5) {
                        axeHits.put(aid, 0);
                        if (victim instanceof Player stunned) {
                            plugin.abilities().stunPlayer(stunned, 30);
                        } else {
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4));
                        }
                        attacker.getWorld().spawnParticle(Particle.CRIT,
                                victim.getLocation().add(0, 1.6, 0), 15, 0.3, 0.2, 0.3, 0.1);
                        attacker.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.6f);
                    }
                }
                // Static Charge manifestation: spend an empowered hit.
                if (plugin.abilities().consumeStaticCharge(aid)) {
                    event.setDamage(event.getDamage() + 2.0);
                    attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            victim.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.1);
                    attacker.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BEE_HURT, 0.8f, 1.8f);
                }
                if (data.hasCard(Card.CLEAVE)) cleave(attacker, victim, damage);
                // Retribution: spend the armed counter (every 5 hits taken) as +1.5 hearts of TRUE
                // damage. Routed through dealTrueDamage so totems and death events still work.
                if (data.hasCard(Card.RETRIBUTION) && retributionArmed.remove(aid)) {
                    LivingEntity v = victim;
                    Player atk = attacker;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!v.isDead() && v.isValid()) plugin.abilities().dealTrueDamage(v, atk, 2.0);
                    });
                    attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
                }
                if (data.hasCard(Card.UNYIELDING_INFERNO) && victim.getFireTicks() > 0) {
                    victim.setFireTicks(Math.max(victim.getFireTicks(), 100));
                    attacker.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.02);
                }
                // Spine Cutter: a back hit (horizontal facings aligned — pitch ignored so
                // looking down at someone's face doesn't count) deals +3 damage next tick.
                if (data.hasCard(Card.SPINE_CUTTER) && isBackstab(attacker, victim)) {
                    LivingEntity v = victim;
                    Player atk = attacker;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!v.isDead() && v.isValid()) plugin.abilities().dealDamage(v, atk, 1.5);
                    });
                    // SFX + visual so the backstab proc is obvious.
                    Location fx = victim.getLocation().add(0, 1, 0);
                    attacker.getWorld().spawnParticle(Particle.CRIT, fx, 14, 0.3, 0.3, 0.3, 0.2);
                    attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, fx, 6, 0.2, 0.2, 0.2, 0);
                    attacker.getWorld().playSound(fx, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
                    attacker.getWorld().playSound(fx, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.5f);
                }
            }
        }
    }

    // Retribution: count hits taken; every 3rd arms a true-damage bonus for the next melee hit.
    private final java.util.Map<UUID, Integer> retributionHits = new java.util.HashMap<>();
    private final java.util.Set<UUID> retributionArmed = new java.util.HashSet<>();
    // Second Wind cooldown per player.
    private final java.util.Map<UUID, Long> secondWindUntil = new java.util.HashMap<>();
    // Guard so Cleave's splash hits don't recursively trigger more cleaves.
    private final java.util.Set<UUID> cleaving = new java.util.HashSet<>();

    // ---- 1.9.0 talent state ----
    /** Vendetta: the last enemy that hit this player, and until when the grudge lasts. */
    private record Grudge(UUID enemy, long until) {}
    private final java.util.Map<UUID, Grudge> grudges = new java.util.HashMap<>();
    /** Combo: consecutive-hit streak against a single target. */
    private static final class ComboState { UUID target; int stacks; long last; }
    private final java.util.Map<UUID, ComboState> combos = new java.util.HashMap<>();
    /** Rampage: expiry timestamps of kill stacks (max 3 kept). */
    private final java.util.Map<UUID, java.util.ArrayDeque<Long>> rampage = new java.util.HashMap<>();
    /** Mangle: victims healing at half effect until the timestamp. */
    private final java.util.Map<UUID, Long> mangledUntil = new java.util.HashMap<>();
    /** Undying: per-player cheat-death cooldown. */
    private final java.util.Map<UUID, Long> undyingUntil = new java.util.HashMap<>();

    // Concussive Blows: hit counter per attacker.
    private final java.util.Map<UUID, Integer> axeHits = new java.util.HashMap<>();

    /** Main-hand weapon check by material-name suffix ("SPEAR" also matches tiered spears). */
    private boolean holdingWeapon(Player player, String suffix) {
        return player.getInventory().getItemInMainHand().getType().name().endsWith(suffix);
    }

    /** Backstab = both facing roughly the same horizontal direction. */
    private boolean isBackstab(Player attacker, LivingEntity victim) {
        org.bukkit.util.Vector a = attacker.getLocation().getDirection().setY(0);
        org.bukkit.util.Vector v = victim.getLocation().getDirection().setY(0);
        if (a.lengthSquared() < 0.01 || v.lengthSquared() < 0.01) return false;
        return a.normalize().dot(v.normalize()) > 0.5;
    }

    private boolean isDebuffed(LivingEntity victim) {
        return victim.getFireTicks() > 0 || victim.getFreezeTicks() > 0
                || victim.hasPotionEffect(PotionEffectType.POISON)
                || victim.hasPotionEffect(PotionEffectType.SLOWNESS)
                || victim.hasPotionEffect(PotionEffectType.WITHER)
                || victim.hasPotionEffect(PotionEffectType.BLINDNESS);
    }

    /** Players (except trusted allies) and monsters near the attacker — Duelist's "enemies". */
    private int nearbyEnemyCount(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int count = 0;
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (e instanceof Player other) {
                if (!data.isTrusted(other.getUniqueId())) count++;
            } else if (e instanceof Monster) {
                count++;
            }
        }
        return count;
    }

    /** Advances the attacker's combo against this victim; returns the stack count (0-5). */
    private int comboStacks(UUID attacker, UUID victim) {
        long now = System.currentTimeMillis();
        ComboState state = combos.computeIfAbsent(attacker, k -> new ComboState());
        if (victim.equals(state.target) && now - state.last <= 2_000) {
            state.stacks = Math.min(state.stacks + 1, 3);
        } else {
            state.stacks = 0;
        }
        state.target = victim;
        state.last = now;
        return state.stacks;
    }

    private int rampageStacks(UUID id) {
        java.util.ArrayDeque<Long> stacks = rampage.get(id);
        if (stacks == null) return 0;
        long now = System.currentTimeMillis();
        stacks.removeIf(expiry -> expiry <= now);
        return Math.min(stacks.size(), 3);
    }

    /** Mangle: marked victims regain half health. Overheal: excess healing becomes absorption. */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        Long until = mangledUntil.get(event.getEntity().getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            event.setAmount(event.getAmount() * 0.5);
        }
        if (event.getEntity() instanceof Player player
                && plugin.data().get(player.getUniqueId()).hasCard(Card.OVERHEAL)) {
            double max = player.getAttribute(Attribute.MAX_HEALTH) != null
                    ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            double overflow = player.getHealth() + event.getAmount() - max;
            if (overflow > 0) {
                player.setAbsorptionAmount(Math.min(4.0, player.getAbsorptionAmount() + overflow));
            }
        }
    }

    /** Escape Artist / Clarity: certain debuffs never stick. */
    @EventHandler(ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getNewEffect() == null) return;
        PotionEffectType type = event.getNewEffect().getType();
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (type.equals(PotionEffectType.SLOWNESS) && data.hasCard(Card.ESCAPE_ARTIST)) {
            event.setCancelled(true);
            return;
        }
        if ((type.equals(PotionEffectType.BLINDNESS) || type.equals(PotionEffectType.NAUSEA)
                || type.equals(PotionEffectType.DARKNESS)) && data.hasCard(Card.CLARITY)) {
            event.setCancelled(true);
        }
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

        // Invincible while a draft menu is open (you're forced to pick) — capped at
        // 10s per draft so an unpicked menu can't be camped as permanent immunity.
        if (plugin.gui().isProtected(player)) {
            event.setCancelled(true);
            return;
        }

        // Aegis ward: incoming projectiles simply stop.
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                && plugin.abilities().hasAegis(player.getUniqueId())) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.02);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.9f);
            return;
        }

        // Riposte stance: parry the next hit outright and counter the attacker.
        if (event instanceof EntityDamageByEntityEvent parried
                && plugin.abilities().consumeRiposte(player.getUniqueId())) {
            event.setCancelled(true);
            LivingEntity source = null;
            if (parried.getDamager() instanceof LivingEntity le) source = le;
            else if (parried.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof LivingEntity le) source = le;
            if (source != null) plugin.abilities().riposteCounter(player, source);
            return;
        }

        // Overcharge: melee attackers get zapped back.
        if (event instanceof EntityDamageByEntityEvent zapped
                && zapped.getDamager() instanceof LivingEntity meleeSource
                && plugin.abilities().isOvercharged(player.getUniqueId())
                && !plugin.abilities().isAbilityDamage(player.getUniqueId())) {
            plugin.abilities().overchargeZap(player, meleeSource);
        }

        // Afterimage: the first hit in the window blinks you backwards (damage still lands).
        if (event instanceof EntityDamageByEntityEvent
                && plugin.abilities().consumeAfterimage(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> { if (player.isOnline() && !player.isDead()) plugin.abilities().afterimageBlink(player); });
        }

        // Sunder: cracked guard — take 15% more from everything while marked.
        if (plugin.abilities().isSundered(player.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.15);
        }

        PlayerData data = plugin.data().get(player.getUniqueId());

        // Vendetta: remember who last hit you (6s grudge window).
        if (data.hasCard(Card.VENDETTA) && event instanceof EntityDamageByEntityEvent hitBy) {
            LivingEntity source = null;
            if (hitBy.getDamager() instanceof LivingEntity le) source = le;
            else if (hitBy.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof LivingEntity le) source = le;
            if (source != null) {
                grudges.put(player.getUniqueId(),
                        new Grudge(source.getUniqueId(), System.currentTimeMillis() + 6_000));
            }
        }

        // Risky Moves: chance to fully negate an incoming hit.
        if (data.hasCard(Card.RISKY_MOVES) && ThreadLocalRandom.current().nextDouble() < 0.15) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && (data.hasCard(Card.FEATHER)
                    || (data.hasCard(Card.KICK_OFF) && event.getDamage() <= 6.0)
                    || plugin.abilities().hasNoFall(player.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }
        if (data.hasCard(Card.JUGGERNAUT)) {
            event.setDamage(event.getDamage() * 0.90);
        }
        // Glass Cannon: glass jaw — take 20% more damage from everything.
        if (data.hasCard(Card.GLASS_CANNON)) {
            event.setDamage(event.getDamage() * 1.20);
        }
        // Bastion: hunker down — 25% less damage while sneaking.
        if (data.hasCard(Card.BASTION) && player.isSneaking()) {
            event.setDamage(event.getDamage() * 0.85);
        }
        // Braced: hits taken at full health deal 30% less (anti-burst opener).
        if (data.hasCard(Card.BRACED) && healthRatio(player) >= 0.999) {
            event.setDamage(event.getDamage() * 0.85);
        }
        // Deflection: 30% less projectile damage.
        if (data.hasCard(Card.DEFLECTION)
                && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setDamage(event.getDamage() * 0.80);
        }
        // Ghost: when hit, a chance to fully vanish (armor too) + Speed II for 3s.
        if (data.hasCard(Card.GHOST) && ThreadLocalRandom.current().nextDouble() < plugin.config().ghostChance()) {
            applyGhost(player, 60);
        }
        // Retribution: every 5th hit taken arms a true-damage bonus for the next melee hit.
        if (data.hasCard(Card.RETRIBUTION)) {
            int hits = retributionHits.merge(player.getUniqueId(), 1, Integer::sum);
            if (hits >= 5) {
                retributionArmed.add(player.getUniqueId());
                retributionHits.put(player.getUniqueId(), 0);
            }
        }
        // Second Wind: at low HP, once per 30s, get Regen II + Absorption.
        if (data.hasCard(Card.SECOND_WIND) && healthRatio(player) < 0.20) {
            long now = System.currentTimeMillis();
            Long until = secondWindUntil.get(player.getUniqueId());
            if (until == null || now >= until) {
                secondWindUntil.put(player.getUniqueId(), now + 45_000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.4f);
            }
        }
        // Undying: once per 60s, a killing blow leaves you at 1 HP instead.
        if (data.hasCard(Card.UNDYING) && event.getFinalDamage() >= player.getHealth()) {
            long now = System.currentTimeMillis();
            Long until = undyingUntil.get(player.getUniqueId());
            if (until == null || now >= until) {
                undyingUntil.put(player.getUniqueId(), now + 90_000);
                event.setCancelled(true);
                player.setHealth(1.0);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.3);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.6f);
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
    // Ghost: when the vanish visuals should end — a newer proc extends it.
    private final java.util.Map<UUID, Long> ghostUntil = new java.util.HashMap<>();

    private void applyGhost(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.6f);
        ghostUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationTicks * 50L);

        ItemStack air = new ItemStack(Material.AIR);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(player)) continue;
            for (EquipmentSlot slot : VISUAL_SLOTS) viewer.sendEquipmentChange(player, slot, air);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            // An overlapping proc extended the vanish — let its restore task handle it.
            if (System.currentTimeMillis() < ghostUntil.getOrDefault(player.getUniqueId(), 0L) - 25) return;
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
