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
    BRUTALITY("Brutality", Material.IRON_SWORD, "+20% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.20),
    ONSLAUGHT("Onslaught", Material.DIAMOND_SWORD, "+35% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.35),
    FRENZY("Frenzy", Material.GOLDEN_SWORD, "+25% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.25),
    STEADFAST("Steadfast", Material.NETHERITE_INGOT, "+40% knockback resistance.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.4),
    REACH("Reach", Material.FISHING_ROD, "+1 block of attack reach.",
            Attribute.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_NUMBER, 1.0),

    // ---- Flag cards (10) — logic in CombatListener ----
    LIFESTEAL("Lifesteal", Material.GHAST_TEAR, "Heal 10% of melee damage you deal."),
    LEECH("Leech", Material.GLISTERING_MELON_SLICE, "Heal 1 heart on every melee kill."),
    THORNS("Thorns", Material.CACTUS, "Reflect 30% of melee damage to attackers."),
    BERSERKER("Berserker", Material.BLAZE_POWDER, "+30% damage while below 30% health."),
    EXECUTIONER("Executioner", Material.IRON_AXE, "+50% damage to targets below 20% health."),
    FEATHER("Feather", Material.FEATHER, "Immune to fall damage."),
    ADRENALINE("Adrenaline", Material.SUGAR, "Speed II + Regen I for 4s after a kill."),
    SHARPSHOOTER("Sharpshooter", Material.BOW, "+25% projectile damage."),
    JUGGERNAUT("Juggernaut", Material.NETHERITE_CHESTPLATE, "Take 15% less damage from all sources."),
    SCAVENGER("Scavenger", Material.EXPERIENCE_BOTTLE, "+50% Ruin XP from kills.");

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
