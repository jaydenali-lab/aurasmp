package com.aurasmp.ruin.card;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves the data-driven talent effects declared in {@link Passives}, plus
 * the one-off passives that don't fit a spec map (Crusher, Thornplate,
 * Hardened, Guardian, Doom Mark, ...). Melee-talent scaling that predates 3.0
 * stays in CombatListener; everything new lives here.
 */
public final class PassiveListener implements Listener {

    private final RuinPlugin plugin;
    private final Map<UUID, Integer> doomHits = new java.util.HashMap<>();
    private final Map<UUID, Long> doomMarked = new java.util.HashMap<>();
    private final Map<UUID, Long> phoenixCd = new java.util.HashMap<>();
    private final Map<UUID, Long> lastHitAt = new java.util.HashMap<>();
    private final java.util.Set<UUID> alchemistExtending = new java.util.HashSet<>();

    public PassiveListener(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerData data(Player player) {
        return plugin.data().get(player.getUniqueId());
    }

    /** True when this Doom Mark victim is currently marked. */
    public boolean isDoomMarked(UUID id) {
        Long until = doomMarked.get(id);
        return until != null && until > System.currentTimeMillis();
    }

    /** Millis since this player last took a hit (for Aegis Soul). */
    public long sinceLastHit(UUID id) {
        Long at = lastHitAt.get(id);
        return at == null ? Long.MAX_VALUE : System.currentTimeMillis() - at;
    }

    // ---------------------------------------------------------- outgoing ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        PlayerData data = data(attacker);

        double mult = 1.0;
        for (Map.Entry<Card, Passives.CondDamage> entry : Passives.DMG_VS.entrySet()) {
            if (data.hasCard(entry.getKey()) && condMet(entry.getValue().cond(), attacker, victim)) {
                mult += entry.getValue().pct();
            }
        }
        // Colossus: the health comes at the cost of 15% outgoing damage.
        if (data.hasCard(Card.COLOSSUS)) mult -= 0.15;
        // Doom Mark: marked players take 30% more from everyone.
        if (victim instanceof Player hit && isDoomMarked(hit.getUniqueId())) mult += 0.30;
        if (mult != 1.0) event.setDamage(Math.max(0, event.getDamage() * mult));

        // On-hit procs against the victim (Charisma/Willpower scale durations).
        for (Map.Entry<Card, Passives.Proc> entry : Passives.ON_HIT.entrySet()) {
            if (data.hasCard(entry.getKey())) proc(entry.getValue(), attacker, victim);
        }
        // Self buffs for landing a hit.
        for (Map.Entry<Card, Passives.Proc> entry : Passives.ON_HIT_SELF.entrySet()) {
            if (data.hasCard(entry.getKey())) procSelf(entry.getValue(), attacker);
        }

        // Crusher: smash through raised shields.
        if (data.hasCard(Card.CRUSHER) && victim instanceof Player blocker && blocker.isBlocking()) {
            blocker.setCooldown(org.bukkit.Material.SHIELD, 60);
            blocker.getWorld().playSound(blocker.getLocation(),
                    org.bukkit.Sound.ITEM_SHIELD_BREAK, 0.8f, 1.1f);
        }
        // Battle Mage: melee hits shave 0.5s off manifestation cooldowns.
        if (data.hasCard(Card.BATTLE_MAGE)) {
            plugin.abilities().reduceCooldowns(attacker.getUniqueId(), 500);
        }
        // Doom Mark: every 7th hit on a player marks them for 5s.
        if (data.hasCard(Card.DOOM_MARK) && victim instanceof Player hit) {
            int hits = doomHits.merge(attacker.getUniqueId(), 1, Integer::sum);
            if (hits >= 7) {
                doomHits.put(attacker.getUniqueId(), 0);
                doomMarked.put(hit.getUniqueId(), System.currentTimeMillis()
                        + plugin.stats().debuffTicks(attacker, hit, 100) * 50L);
                hit.getWorld().spawnParticle(org.bukkit.Particle.RAID_OMEN,
                        hit.getLocation().add(0, 2.2, 0), 12, 0.3, 0.2, 0.3, 0.02);
                hit.getWorld().playSound(hit.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.6f);
            }
        }
    }

    private boolean condMet(Passives.Cond cond, Player attacker, LivingEntity victim) {
        return switch (cond) {
            case TARGET_UNARMED -> victim.getEquipment() == null
                    || victim.getEquipment().getItemInMainHand().getType().isAir();
            case TARGET_AIRBORNE -> !victim.isOnGround();
            case TARGET_ARMORED -> attrValue(victim, Attribute.ARMOR) >= 15;
            case TARGET_POISONED -> victim.hasPotionEffect(PotionEffectType.POISON);
            case TARGET_SLOWED -> victim.hasPotionEffect(PotionEffectType.SLOWNESS);
            case TARGET_BLIND -> victim.hasPotionEffect(PotionEffectType.BLINDNESS);
            case TARGET_WITHERED -> victim.hasPotionEffect(PotionEffectType.WITHER);
            case TARGET_ANY_DEBUFF -> victim instanceof Player p && hasAnyDebuff(p);
            case TARGET_BELOW_YOU -> victim.getLocation().getY() < attacker.getLocation().getY() - 1.0;
            case SELF_SPRINTING -> attacker.isSprinting();
            case SELF_AIRBORNE -> !attacker.isOnGround();
            case SELF_FULL -> healthRatio(attacker) >= 0.9;
            case SELF_DYING -> healthRatio(attacker) <= 0.10;
        };
    }

    private static final PotionEffectType[] DEBUFFS = {
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.DARKNESS};

    private boolean hasAnyDebuff(Player player) {
        for (PotionEffectType type : DEBUFFS) {
            if (player.hasPotionEffect(type)) return true;
        }
        return false;
    }

    private void proc(Passives.Proc spec, Player attacker, LivingEntity victim) {
        if (ThreadLocalRandom.current().nextInt(100) >= spec.chancePct()) return;
        int ticks = plugin.stats().debuffTicks(attacker, victim, spec.seconds() * 20);
        // Hexweaver: your debuffs last 20% longer.
        if (data(attacker).hasCard(Card.HEXWEAVER)) ticks = (int) (ticks * 1.20);
        victim.addPotionEffect(new PotionEffect(spec.effect(), ticks, spec.amplifier()));
        // Leeching Curse: heal half a heart whenever you inflict a debuff.
        if (data(attacker).hasCard(Card.LEECHING_CURSE)) heal(attacker, 1.0);
    }

    private void procSelf(Passives.Proc spec, Player player) {
        if (ThreadLocalRandom.current().nextInt(100) >= spec.chancePct()) return;
        player.addPotionEffect(new PotionEffect(spec.effect(), spec.seconds() * 20,
                spec.amplifier(), true, false, true));
    }

    // ---------------------------------------------------------- incoming ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        PlayerData data = data(victim);
        lastHitAt.put(victim.getUniqueId(), System.currentTimeMillis());

        double mult = 1.0;
        for (Map.Entry<Card, Passives.Resist> entry : Passives.RESIST.entrySet()) {
            if (data.hasCard(entry.getKey()) && entry.getValue().causes().contains(event.getCause())) {
                mult -= entry.getValue().pct();
            }
        }
        if (data.hasCard(Card.ANVIL_STANCE) && victim.isSneaking()) mult -= 0.25;
        if (data.hasCard(Card.SECOND_SKIN) && healthRatio(victim) >= 0.9) mult -= 0.15;
        if (data.hasCard(Card.LAST_BASTION) && healthRatio(victim) < 0.25) mult -= 0.20;
        if (data.hasCard(Card.FASTING) && victim.getFoodLevel() < 7) mult -= 0.15;
        if (data.hasCard(Card.GALE_FORM)) mult += 0.10;
        if (data.hasCard(Card.GLASS_MIND)) mult += 0.10;
        // Guardian: trusted allies within 8 blocks shield you 10%.
        if (guardedByAlly(victim)) mult -= 0.10;
        mult = Math.max(0, mult);
        if (mult != 1.0) event.setDamage(event.getDamage() * mult);

        // Hardened: every hit deals 1 less (never below half a heart's worth of 0).
        if (data.hasCard(Card.HARDENED)) {
            event.setDamage(Math.max(0, event.getDamage() - 1.0));
        }

        // Chance dodges.
        if (dodged(data, victim, event)) {
            event.setCancelled(true);
            victim.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,
                    victim.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.02);
            return;
        }

        // Phoenix Temper: dropping below 20% grants Strength I (20s internal cd).
        if (data.hasCard(Card.PHOENIX_TEMPER)
                && healthRatio(victim) - event.getFinalDamage() / maxHealth(victim) <= 0.20) {
            long now = System.currentTimeMillis();
            Long last = phoenixCd.get(victim.getUniqueId());
            if (last == null || now - last > 20_000) {
                phoenixCd.put(victim.getUniqueId(), now);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0, true, false, true));
            }
        }

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;
        LivingEntity attacker = resolveAttacker(byEntity);
        if (attacker == null) return;

        // Thornplate: reflect 15% of melee damage taken.
        if (data.hasCard(Card.THORNPLATE)
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && attacker != victim) {
            double reflect = event.getDamage() * 0.15;
            if (reflect > 0.1) attacker.damage(reflect, victim);
        }
        // Counter-procs on whoever hit you.
        PlayerData victimData = data;
        for (Map.Entry<Card, Passives.Proc> entry : Passives.ON_HURT_ATTACKER.entrySet()) {
            if (victimData.hasCard(entry.getKey())) {
                Passives.Proc spec = entry.getValue();
                if (ThreadLocalRandom.current().nextInt(100) < spec.chancePct()) {
                    int ticks = plugin.stats().debuffTicks(victim, attacker, spec.seconds() * 20);
                    attacker.addPotionEffect(new PotionEffect(spec.effect(), ticks, spec.amplifier()));
                }
            }
        }
        // Self buffs for taking a hit.
        for (Map.Entry<Card, Passives.Proc> entry : Passives.ON_HURT_SELF.entrySet()) {
            if (victimData.hasCard(entry.getKey())) procSelf(entry.getValue(), victim);
        }
    }

    private boolean dodged(PlayerData data, Player victim, EntityDamageEvent event) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (data.hasCard(Card.SIDESTEP)
                && event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                && rng.nextDouble() < 0.10) return true;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return false;
        if (data.hasCard(Card.EVASIVE_ROLL) && rng.nextDouble() < 0.08) return true;
        return data.hasCard(Card.DODGE_ROLL) && victim.isSprinting() && rng.nextDouble() < 0.15;
    }

    private LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity le) return le;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity le) return le;
        return null;
    }

    // ------------------------------------------------------------- misc ----

    @EventHandler(ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED
                && event.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        if (event.getNewEffect() == null) return;
        PlayerData data = data(player);
        // Alchemist: potions you drink last 30% longer (re-added once, extended).
        if (data.hasCard(Card.ALCHEMIST)
                && event.getCause() == EntityPotionEffectEvent.Cause.POTION_DRINK
                && event.getNewEffect() != null && !alchemistExtending.contains(player.getUniqueId())) {
            PotionEffect effect = event.getNewEffect();
            event.setCancelled(true);
            alchemistExtending.add(player.getUniqueId());
            try {
                player.addPotionEffect(new PotionEffect(effect.getType(),
                        (int) (effect.getDuration() * 1.30), effect.getAmplifier(),
                        effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
            } finally {
                alchemistExtending.remove(player.getUniqueId());
            }
            return;
        }
        for (Map.Entry<Card, java.util.Set<PotionEffectType>> entry : Passives.IMMUNE.entrySet()) {
            if (data.hasCard(entry.getKey()) && entry.getValue().contains(event.getModifiedType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = data(player);
        // Wellspring boosts regeneration; Transfusion boosts potion healing.
        if (data.hasCard(Card.WELLSPRING)
                && (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN
                    || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN)) {
            event.setAmount(event.getAmount() * 1.20);
        }
        if (data.hasCard(Card.TRANSFUSION)
                && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC) {
            event.setAmount(event.getAmount() * 1.50);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Slow Metabolism: skip every other point of hunger drain.
        if (event.getFoodLevel() < player.getFoodLevel()
                && data(player).hasCard(Card.SLOW_METABOLISM)
                && ThreadLocalRandom.current().nextBoolean()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        // Reinforced: armor never loses durability.
        if (!data(event.getPlayer()).hasCard(Card.REINFORCED)) return;
        String type = event.getItem().getType().name();
        if (type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE")
                || type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        PlayerData data = data(killer);
        for (Map.Entry<Card, Passives.Proc> entry : Passives.ON_KILL.entrySet()) {
            if (data.hasCard(entry.getKey())) procSelf(entry.getValue(), killer);
        }
        // Spell Thief: kills refund 3s of every cooldown.
        if (data.hasCard(Card.SPELL_THIEF)) {
            plugin.abilities().reduceCooldowns(killer.getUniqueId(), 3_000);
        }
        // Terrorize: kills spread Weakness to enemies near the corpse.
        if (data.hasCard(Card.TERRORIZE)) {
            for (org.bukkit.entity.Entity entity : event.getEntity().getNearbyEntities(6, 6, 6)) {
                if (entity instanceof Player enemy && enemy != killer
                        && !data.isTrusted(enemy.getUniqueId())) {
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                            plugin.stats().debuffTicks(killer, enemy, 100), 0));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Grave Pact: heal 4 hearts when a player dies within 12 blocks.
        for (org.bukkit.entity.Entity entity : event.getEntity().getNearbyEntities(12, 12, 12)) {
            if (entity instanceof Player nearby && nearby != event.getEntity()
                    && data(nearby).hasCard(Card.GRAVE_PACT)) {
                heal(nearby, 8.0);
                nearby.getWorld().spawnParticle(org.bukkit.Particle.SOUL,
                        nearby.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    private boolean guardedByAlly(Player victim) {
        for (org.bukkit.entity.Entity entity : victim.getNearbyEntities(8, 8, 8)) {
            if (entity instanceof Player ally && ally != victim
                    && data(ally).hasCard(Card.GUARDIAN)
                    && data(ally).isTrusted(victim.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private void heal(Player player, double amount) {
        player.setHealth(Math.min(maxHealth(player), player.getHealth() + amount));
    }

    private double maxHealth(LivingEntity entity) {
        return attrValue(entity, Attribute.MAX_HEALTH);
    }

    private double attrValue(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0 : instance.getValue();
    }

    private double healthRatio(LivingEntity entity) {
        double max = maxHealth(entity);
        return max <= 0 ? 1.0 : entity.getHealth() / max;
    }
}
