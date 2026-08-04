package com.aurasmp.ruin.card;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven talent effects. The maps below are consumed by PassiveListener
 * (procs, resists, conditional damage), TalentAura (permanent effects) and
 * CardManager (secondary attribute riders). Talents with one-off mechanics
 * are coded directly in those classes instead.
 */
public final class Passives {

    private Passives() {}

    /** Situations conditional-damage talents key on. */
    public enum Cond { TARGET_UNARMED, TARGET_AIRBORNE, TARGET_ARMORED, TARGET_POISONED,
        TARGET_SLOWED, TARGET_BLIND, TARGET_WITHERED, TARGET_ANY_DEBUFF, TARGET_BELOW_YOU,
        SELF_SPRINTING, SELF_AIRBORNE, SELF_FULL, SELF_DYING }

    public record CondDamage(Cond cond, double pct) {}
    public record Resist(Set<DamageCause> causes, double pct) {}
    public record Proc(PotionEffectType effect, int seconds, int amplifier, int chancePct) {}
    public record Perm(PotionEffectType effect, int amplifier) {}
    public record AttrRider(Attribute attribute, AttributeModifier.Operation operation, double amount) {}

    public static final Map<Card, CondDamage> DMG_VS = new EnumMap<>(Card.class);
    public static final Map<Card, Resist> RESIST = new EnumMap<>(Card.class);
    public static final Map<Card, Proc> ON_HIT = new EnumMap<>(Card.class);
    public static final Map<Card, Proc> ON_HIT_SELF = new EnumMap<>(Card.class);
    public static final Map<Card, Proc> ON_HURT_SELF = new EnumMap<>(Card.class);
    public static final Map<Card, Proc> ON_HURT_ATTACKER = new EnumMap<>(Card.class);
    public static final Map<Card, Proc> ON_KILL = new EnumMap<>(Card.class);
    public static final Map<Card, Perm> PERM = new EnumMap<>(Card.class);
    public static final Map<Card, Set<PotionEffectType>> IMMUNE = new EnumMap<>(Card.class);
    public static final Map<Card, List<AttrRider>> EXTRA_ATTR = new EnumMap<>(Card.class);

    static {
        DMG_VS.put(Card.BULLY, new CondDamage(Cond.TARGET_UNARMED, 0.12));
        DMG_VS.put(Card.SLUGGER, new CondDamage(Cond.SELF_SPRINTING, 0.08));
        DMG_VS.put(Card.BONEBREAKER, new CondDamage(Cond.TARGET_AIRBORNE, 0.2));
        DMG_VS.put(Card.SIEGEBREAKER, new CondDamage(Cond.TARGET_ARMORED, 0.15));
        DMG_VS.put(Card.TITANS_WRATH, new CondDamage(Cond.SELF_FULL, 0.15));
        DMG_VS.put(Card.PLAGUEBEARER, new CondDamage(Cond.TARGET_POISONED, 0.15));
        DMG_VS.put(Card.TORMENTOR, new CondDamage(Cond.TARGET_SLOWED, 0.15));
        DMG_VS.put(Card.OPPORTUNIST, new CondDamage(Cond.TARGET_BLIND, 0.15));
        DMG_VS.put(Card.CURSED_BLADE, new CondDamage(Cond.TARGET_WITHERED, 0.1));
        DMG_VS.put(Card.PARIAH, new CondDamage(Cond.TARGET_ANY_DEBUFF, 0.08));
        DMG_VS.put(Card.DEATHS_DOOR, new CondDamage(Cond.SELF_DYING, 0.2));
        DMG_VS.put(Card.VANTAGE, new CondDamage(Cond.TARGET_BELOW_YOU, 0.15));
        DMG_VS.put(Card.AERIAL_ACE, new CondDamage(Cond.SELF_AIRBORNE, 0.2));
        RESIST.put(Card.PADDING, new Resist(EnumSet.of(DamageCause.FALL), 0.5));
        RESIST.put(Card.ACROBAT, new Resist(EnumSet.of(DamageCause.FALL), 0.7));
        RESIST.put(Card.FIREPROOF, new Resist(EnumSet.of(DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.HOT_FLOOR), 0.5));
        RESIST.put(Card.BLASTGUARD, new Resist(EnumSet.of(DamageCause.BLOCK_EXPLOSION, DamageCause.ENTITY_EXPLOSION), 0.4));
        RESIST.put(Card.CHAINWEAVE, new Resist(EnumSet.of(DamageCause.PROJECTILE), 0.1));
        RESIST.put(Card.SPELLWARD, new Resist(EnumSet.of(DamageCause.MAGIC, DamageCause.DRAGON_BREATH), 0.3));
        RESIST.put(Card.IRON_WILL, new Resist(EnumSet.of(DamageCause.WITHER), 0.5));
        RESIST.put(Card.THICK_BLOOD, new Resist(EnumSet.of(DamageCause.POISON), 0.5));
        ON_HIT.put(Card.VENOMOUS, new Proc(PotionEffectType.POISON, 3, 0, 20));
        ON_HIT.put(Card.WITHERING_TOUCH, new Proc(PotionEffectType.WITHER, 2, 0, 15));
        ON_HIT.put(Card.CRIPPLE, new Proc(PotionEffectType.SLOWNESS, 2, 1, 20));
        ON_HIT.put(Card.BLINDING_STRIKES, new Proc(PotionEffectType.BLINDNESS, 2, 0, 10));
        ON_HIT.put(Card.EXPOSE, new Proc(PotionEffectType.GLOWING, 5, 0, 100));
        ON_HIT.put(Card.DEMORALIZE, new Proc(PotionEffectType.WEAKNESS, 3, 0, 15));
        ON_HIT.put(Card.INFECTIOUS_WOUND, new Proc(PotionEffectType.MINING_FATIGUE, 3, 0, 15));
        ON_HIT_SELF.put(Card.HIT_AND_RUN, new Proc(PotionEffectType.SPEED, 3, 0, 100));
        ON_HURT_SELF.put(Card.BLUR, new Proc(PotionEffectType.SPEED, 2, 1, 100));
        ON_HURT_ATTACKER.put(Card.JINX, new Proc(PotionEffectType.SLOWNESS, 3, 1, 10));
        ON_HURT_ATTACKER.put(Card.BANSHEE, new Proc(PotionEffectType.BLINDNESS, 2, 0, 10));
        ON_KILL.put(Card.TAILWIND, new Proc(PotionEffectType.SPEED, 4, 2, 100));
        PERM.put(Card.SPRING_STEP, new Perm(PotionEffectType.JUMP_BOOST, 0));
        PERM.put(Card.PARKOUR, new Perm(PotionEffectType.JUMP_BOOST, 1));
        PERM.put(Card.WIND_SPRINT, new Perm(PotionEffectType.SPEED, 0));
        PERM.put(Card.GALE_FORM, new Perm(PotionEffectType.SPEED, 1));
        PERM.put(Card.SECOND_HEART, new Perm(PotionEffectType.ABSORPTION, 0));
        IMMUNE.put(Card.IMMOVABLE, Set.of(PotionEffectType.LEVITATION));
        IMMUNE.put(Card.STUBBORN, Set.of(PotionEffectType.WEAKNESS));
        IMMUNE.put(Card.RESOLUTE, Set.of(PotionEffectType.WITHER));
        IMMUNE.put(Card.CALM, Set.of(PotionEffectType.HUNGER));
        IMMUNE.put(Card.KEEN_MIND, Set.of(PotionEffectType.NAUSEA, PotionEffectType.MINING_FATIGUE));
        IMMUNE.put(Card.UNTRACKABLE, Set.of(PotionEffectType.GLOWING));
        EXTRA_ATTR.put(Card.BULKHEAD, List.of(new AttrRider(Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, -0.1)));
        EXTRA_ATTR.put(Card.FEATHERWEIGHT, List.of(new AttrRider(Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.05)));
    }
}