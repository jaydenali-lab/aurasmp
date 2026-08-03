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
    BULWARK("Bulwark", Material.IRON_CHESTPLATE, "+3 armor.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 3.0),
    SWIFTNESS("Swiftness", Material.SUGAR, "+10% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.10),
    ONSLAUGHT("Onslaught", Material.DIAMOND_SWORD, "+7% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.07),
    FRENZY("Frenzy", Material.GOLDEN_SWORD, "+10% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.10),
    STEADFAST("Steadfast", Material.NETHERITE_INGOT, "+40% knockback resistance.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.4),

    // ---- Flag cards (10) — logic in CombatListener ----
    LIFESTEAL("Lifesteal", Material.GHAST_TEAR, "Heal 10% of melee damage you deal."),
    GHOST("Ghost", Material.PHANTOM_MEMBRANE, "15% chance when hit to fully vanish (armor too) + Speed II for 3s."),
    BERSERKER("Berserker", Material.BLAZE_POWDER, "+7% damage while below 30% health."),
    EXECUTIONER("Executioner", Material.IRON_AXE, "+30% damage to targets below 20% health."),
    FEATHER("Feather", Material.FEATHER, "Immune to fall damage."),
    ADRENALINE("Adrenaline", Material.SUGAR, "Speed II + Regen I for 4s after a kill."),
    SHARPSHOOTER("Sharpshooter", Material.BOW, "+5% projectile damage."),
    JUGGERNAUT("Juggernaut", Material.NETHERITE_CHESTPLATE, "Take 10% less damage from all sources."),
    SCAVENGER("Scavenger", Material.EXPERIENCE_BOTTLE, "+50% Ruin XP from kills."),

    // ==== Expansion: 20 more talents ====

    // Attribute (3)

    // Aura effects (7) — kept refreshed by TalentAura
    REGENERATOR("Regenerator", Material.GOLDEN_APPLE, "Constantly regenerate health."),

    // Combat triggers (7) — resolved in CombatListener
    IGNITE("Ignite", Material.FLINT_AND_STEEL, "Melee hits set the target on fire."),
    FROSTBITE("Frostbite", Material.PACKED_ICE, "Melee hits slow the target."),
    CLEAVE("Cleave", Material.DIAMOND_AXE, "Melee hits splash 30% damage to nearby enemies."),
    RETRIBUTION("Retribution", Material.NETHERITE_SCRAP, "Every 5 hits taken, your next melee hit deals +1 heart of true damage."),

    // ==== Deepwoken talents (10) ====
    PACK_LEADER("Pack Leader", Material.WOLF_ARMOR, "Resistance while an ally fights beside you."),
    UNYIELDING_INFERNO("Unyielding Inferno", Material.BLAZE_POWDER, "+1.5 hearts to hits on burning foes."),
    SPINE_CUTTER("Spine Cutter", Material.NETHERITE_SWORD, "Backstabs deal +1.5 damage."),
    RISKY_MOVES("Risky Moves", Material.PHANTOM_MEMBRANE, "15% chance to fully negate an incoming hit."),

    // ==== More talents ====
    TITAN("Titan", Material.ENCHANTED_GOLDEN_APPLE, "+4 hearts of max health, but 20% slower.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 8.0),
    GLASS_CANNON("Glass Cannon", Material.TNT, "+15% melee damage, but take 20% more damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.15),
    SECOND_WIND("Second Wind", Material.TOTEM_OF_UNDYING, "Below 20% HP: Regen II + Absorption (45s cd)."),

    // ==== 1.9.0: 20 more talents — all in CombatListener / AbilityManager ====
    FIRST_STRIKE("First Strike", Material.WOODEN_SWORD, "+6% melee damage to full-health targets."),
    COMBO("Combo", Material.CLOCK, "Consecutive hits on the same target stack +2% damage (up to +6%)."),
    MANGLE("Mangle", Material.ROTTEN_FLESH, "Enemies you hit heal 50% less for 5s."),
    RAMPAGE("Rampage", Material.WITHER_SKELETON_SKULL, "Kills grant +2% damage for 20s, stacking 3 times."),
    ATTUNEMENT("Attunement", Material.AMETHYST_SHARD, "Your manifestation cooldowns are 15% shorter."),
    HEADHUNTER("Headhunter", Material.PLAYER_HEAD, "Player kills grant 2 absorption hearts for 30s."),
    UNDYING("Undying", Material.END_CRYSTAL, "Once per 90s, a killing blow leaves you at 1 HP instead."),
    DEFLECTION("Deflection", Material.ARROW, "Take 20% less projectile damage."),
    ESCAPE_ARTIST("Escape Artist", Material.LEAD, "Slowness never sticks to you."),

    // ==== 1.11.0: 8 more talents ====
    CLARITY("Clarity", Material.GLASS, "Blindness, nausea and darkness never stick to you."),
    BATTLE_RUSH("Battle Rush", Material.RECOVERY_COMPASS, "Player kills refund half your manifestation cooldowns."),
    OVERHEAL("Overheal", Material.SPONGE, "Healing past full health becomes absorption (up to 2 hearts)."),
    HAYMAKER("Haymaker", Material.SLIME_BALL, "Your melee hits knock enemies back much further.",
            Attribute.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_NUMBER, 0.5),
    MEDIC("Medic", Material.CAKE, "Nearby hurt allies slowly regenerate."),

    // ==== 2.0.0: weapon talents ====
    FENCER("Fencer", Material.IRON_SWORD, "+7% damage while holding a sword."),
    SPEARHEAD("Spearhead", Material.TRIDENT, "+10% damage while holding a spear."),
    SKEWER("Skewer", Material.ARROW, "Spear hits from 3.5+ blocks away deal +1.5 bonus damage."),
    CONCUSSIVE_BLOWS("Concussive Blows", Material.NETHERITE_AXE, "Every 5th axe hit stuns the target for 1.5s.");
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
    /** Resource-pack item model that shows this talent's glyph as the icon. */
    public String modelKey() { return "talent_" + name().toLowerCase(java.util.Locale.ROOT); }
    public String description() { return description; }
    public boolean isAttribute() { return attribute != null; }
    public Attribute attribute() { return attribute; }
    public AttributeModifier.Operation operation() { return operation; }
    public double amount() { return amount; }

    // ---- Rarity tiers (everything not listed is COMMON) ----
    private static final java.util.EnumMap<Card, Rarity> RARITY = new java.util.EnumMap<>(Card.class);
    static {
        for (Card c : new Card[]{ONSLAUGHT, EXECUTIONER, JUGGERNAUT, GHOST, RISKY_MOVES,
                GLASS_CANNON, SECOND_WIND, UNDYING, CONCUSSIVE_BLOWS}) {
            RARITY.put(c, Rarity.LEGENDARY);
        }
        for (Card c : new Card[]{LIFESTEAL, REGENERATOR, CLEAVE, RETRIBUTION,
                UNYIELDING_INFERNO, SPINE_CUTTER, TITAN, COMBO, MANGLE, RAMPAGE, ATTUNEMENT, HEADHUNTER, ESCAPE_ARTIST,
                BATTLE_RUSH, OVERHEAL}) {
            RARITY.put(c, Rarity.EPIC);
        }
        for (Card c : new Card[]{VITALITY, BULWARK, SWIFTNESS, STEADFAST, BERSERKER,
                ADRENALINE, PACK_LEADER, FIRST_STRIKE, DEFLECTION, CLARITY, HAYMAKER, MEDIC,
                SPEARHEAD, SKEWER}) {
            RARITY.put(c, Rarity.RARE);
        }
    }

    public Rarity rarity() { return RARITY.getOrDefault(this, Rarity.COMMON); }
}
