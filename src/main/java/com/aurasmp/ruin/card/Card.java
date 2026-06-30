package com.aurasmp.ruin.card;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

/**
 * The 20 draftable cards.
 *
 * <p>Two flavours:
 * <ul>
 *   <li><b>Attribute</b> cards carry an {@link Attribute}, an {@link AttributeModifier.Operation}
 *       and an amount. They are applied as persistent attribute modifiers
 *       (see {@code CardManager#recalc}).</li>
 *   <li><b>Flag</b> cards have a {@code null} attribute. Their effect lives in the combat
 *       listeners, which simply check whether the player owns the card.</li>
 * </ul>
 */
public enum Card {

    // ---- Attribute cards (10) ----
    VITALITY("Vitality", Material.RED_DYE, "+2 hearts of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 4.0),
    ENDURANCE("Endurance", Material.PINK_DYE, "+3 hearts of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 6.0),
    BULWARK("Bulwark", Material.IRON_CHESTPLATE, "+3 armor.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 3.0),
    TOUGH_SKIN("Tough Skin", Material.TURTLE_HELMET, "+4 armor toughness.",
            Attribute.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_NUMBER, 4.0),
    SWIFTNESS("Swiftness", Material.SUGAR, "+15% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.15),
    BRUTALITY("Brutality", Material.IRON_SWORD, "+10% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.10),
    ONSLAUGHT("Onslaught", Material.DIAMOND_SWORD, "+20% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.20),
    FRENZY("Frenzy", Material.GOLDEN_SWORD, "+10% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.10),
    STEADFAST("Steadfast", Material.NETHERITE_INGOT, "+40% knockback resistance.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.4),
    REACH("Reach", Material.FISHING_ROD, "+1 block of attack reach.",
            Attribute.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_NUMBER, 1.0),

    // ---- Flag cards (10) — logic in CombatListener ----
    LIFESTEAL("Lifesteal", Material.GHAST_TEAR, "Heal 10% of melee damage you deal."),
    LEECH("Leech", Material.GLISTERING_MELON_SLICE, "Heal 1 heart on every melee kill."),
    GHOST("Ghost", Material.PHANTOM_MEMBRANE, "15% chance when hit to fully vanish (armor too) + Speed II for 3s."),
    BERSERKER("Berserker", Material.BLAZE_POWDER, "+30% damage while below 30% health."),
    EXECUTIONER("Executioner", Material.IRON_AXE, "+50% damage to targets below 20% health."),
    FEATHER("Feather", Material.FEATHER, "Immune to fall damage."),
    ADRENALINE("Adrenaline", Material.SUGAR, "Speed II + Regen I for 4s after a kill."),
    SHARPSHOOTER("Sharpshooter", Material.BOW, "+25% projectile damage."),
    JUGGERNAUT("Juggernaut", Material.NETHERITE_CHESTPLATE, "Take 15% less damage from all sources."),
    SCAVENGER("Scavenger", Material.EXPERIENCE_BOTTLE, "+50% Ruin XP from kills."),

    // ==== Expansion: 20 more talents ====

    // Attribute (3)
    SURE_FOOTED("Sure-Footed", Material.LEATHER_BOOTS, "Step up full blocks without jumping.",
            Attribute.STEP_HEIGHT, AttributeModifier.Operation.ADD_NUMBER, 0.6),
    DEEP_LUNGS("Deep Lungs", Material.PUFFERFISH, "Hold your breath far longer underwater.",
            Attribute.OXYGEN_BONUS, AttributeModifier.Operation.ADD_NUMBER, 4.0),
    LUCKY("Lucky", Material.EMERALD, "+3 Luck — better loot rolls.",
            Attribute.LUCK, AttributeModifier.Operation.ADD_NUMBER, 3.0),

    // Aura effects (7) — kept refreshed by TalentAura
    NIGHT_OWL("Night Owl", Material.GOLDEN_CARROT, "Permanent Night Vision."),
    LEAPER("Leaper", Material.SLIME_BLOCK, "Permanent Jump Boost."),
    HASTE("Haste", Material.GOLDEN_PICKAXE, "Permanent Haste."),
    AQUATIC("Aquatic", Material.HEART_OF_THE_SEA, "Breathe underwater freely."),
    FIRE_WALKER("Fire Walker", Material.MAGMA_CREAM, "Permanent Fire Resistance."),
    REGENERATOR("Regenerator", Material.GOLDEN_APPLE, "Constantly regenerate health."),
    BARRIER("Barrier", Material.SHIELD, "A constant 2-heart absorption shield."),

    // Combat triggers (7) — resolved in CombatListener
    IGNITE("Ignite", Material.FLINT_AND_STEEL, "Melee hits set the target on fire."),
    VENOM("Venom", Material.SPIDER_EYE, "Melee hits apply Poison."),
    FROSTBITE("Frostbite", Material.PACKED_ICE, "Melee hits slow the target."),
    CLEAVE("Cleave", Material.DIAMOND_AXE, "Melee hits splash 30% damage to nearby enemies."),
    CRIT("Crit", Material.QUARTZ, "25% chance for melee hits to deal +50%."),
    BLOODLUST("Bloodlust", Material.REDSTONE_BLOCK, "Strength I for 5s after a kill."),
    RETRIBUTION("Retribution", Material.NETHERITE_SCRAP, "Every 3 hits taken, your next melee hit deals +2 hearts of true damage."),

    // ==== Deepwoken talents (10) ====
    STEADY_FEET("Steady Feet", Material.NETHERITE_BOOTS, "+50% knockback resistance; hard to push.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.5),
    THRESHERS_REACH("Thresher's Reach", Material.TRIDENT, "+1.5 blocks of attack reach.",
            Attribute.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_NUMBER, 1.5),
    QUICKDRAW("Quickdraw", Material.CROSSBOW, "+20% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.20),
    ENDURANCE_RUNNER("Endurance Runner", Material.LEATHER_LEGGINGS, "+8% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.08),
    KICK_OFF("Kick Off", Material.RABBIT_FOOT, "+6% speed and no short-fall damage.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.06),
    CONDITIONED_RUNNER("Conditioned Runner", Material.GOLDEN_BOOTS, "Regenerate while sprinting hurt."),
    PACK_LEADER("Pack Leader", Material.WOLF_ARMOR, "Resistance while an ally fights beside you."),
    UNYIELDING_INFERNO("Unyielding Inferno", Material.BLAZE_POWDER, "+2 hearts to hits on burning foes."),
    SPINE_CUTTER("Spine Cutter", Material.NETHERITE_SWORD, "Backstabs deal +2 hearts of true damage."),
    RISKY_MOVES("Risky Moves", Material.PHANTOM_MEMBRANE, "20% chance to fully negate an incoming hit.");

    private final String displayName;
    private final Material icon;
    private final String description;
    private final Attribute attribute;       // null for flag cards
    private final AttributeModifier.Operation operation;
    private final double amount;

    Card(String displayName, Material icon, String description,
         Attribute attribute, AttributeModifier.Operation operation, double amount) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.attribute = attribute;
        this.operation = operation;
        this.amount = amount;
    }

    Card(String displayName, Material icon, String description) {
        this(displayName, icon, description, null, null, 0.0);
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String description() { return description; }
    public boolean isAttribute() { return attribute != null; }
    public Attribute attribute() { return attribute; }
    public AttributeModifier.Operation operation() { return operation; }
    public double amount() { return amount; }
}
