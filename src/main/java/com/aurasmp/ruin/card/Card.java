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
    RETRIBUTION("Retribution", Material.NETHERITE_SCRAP, "Every 5 hits taken, your next melee hit deals +1.5 hearts of true damage."),

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
    SPINE_CUTTER("Spine Cutter", Material.NETHERITE_SWORD, "Backstabs deal +3 damage."),
    RISKY_MOVES("Risky Moves", Material.PHANTOM_MEMBRANE, "20% chance to fully negate an incoming hit."),

    // ==== More talents ====
    TITAN("Titan", Material.ENCHANTED_GOLDEN_APPLE, "+4 hearts of max health, but 20% slower.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 8.0),
    GLASS_CANNON("Glass Cannon", Material.TNT, "+40% melee damage, but take 20% more damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.40),
    FORTRESS("Fortress", Material.IRON_BLOCK, "+5 armor.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 5.0),
    VAMPIRIC("Vampiric", Material.REDSTONE, "Heal 20% of the melee damage you deal."),
    SECOND_WIND("Second Wind", Material.TOTEM_OF_UNDYING, "Below 20% HP: Regen II + Absorption (30s cd)."),

    // ==== 1.9.0: 20 more talents — all in CombatListener / AbilityManager ====
    FIRST_STRIKE("First Strike", Material.WOODEN_SWORD, "+30% melee damage to full-health targets."),
    PREDATOR("Predator", Material.BONE, "+25% melee damage to debuffed targets (poison, slow, wither, burning, frozen)."),
    DUELIST("Duelist", Material.CHAIN, "+15% melee damage while exactly one enemy is near you."),
    AERIAL("Aerial", Material.ELYTRA, "+25% melee damage while you're airborne."),
    WARPATH("Warpath", Material.LEATHER_BOOTS, "+20% melee damage while sprinting."),
    COMBO("Combo", Material.CLOCK, "Consecutive hits on the same target stack +8% damage (up to +40%)."),
    VENDETTA("Vendetta", Material.FERMENTED_SPIDER_EYE, "+40% melee damage for 6s against the last enemy that hit you."),
    NIGHT_STALKER("Night Stalker", Material.SCULK, "+25% melee damage to targets standing in darkness."),
    GIANT_SLAYER("Giant Slayer", Material.ANVIL, "+20% melee damage to enemies with more health than you."),
    SHIELDBREAKER("Shieldbreaker", Material.MACE, "+40% melee damage to targets with absorption hearts."),
    MANGLE("Mangle", Material.ROTTEN_FLESH, "Enemies you hit heal 50% less for 5s."),
    SKIRMISHER("Skirmisher", Material.STRING, "Hitting an enemy grants you Speed I for 2s."),
    RAMPAGE("Rampage", Material.WITHER_SKELETON_SKULL, "Kills grant +8% damage for 20s, stacking 3 times."),
    ATTUNEMENT("Attunement", Material.AMETHYST_SHARD, "Your manifestation cooldowns are 15% shorter."),
    HEADHUNTER("Headhunter", Material.PLAYER_HEAD, "Player kills grant 2 absorption hearts for 30s."),
    UNDYING("Undying", Material.END_CRYSTAL, "Once per 60s, a killing blow leaves you at 1 HP instead."),
    BASTION("Bastion", Material.OBSIDIAN, "Take 25% less damage while sneaking."),
    BRACED("Braced", Material.SHULKER_SHELL, "Hits taken at full health deal 30% less."),
    DEFLECTION("Deflection", Material.ARROW, "Take 30% less projectile damage."),
    ESCAPE_ARTIST("Escape Artist", Material.LEAD, "Slowness never sticks to you.");

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

    // ---- Rarity tiers (everything not listed is COMMON) ----
    private static final java.util.EnumMap<Card, Rarity> RARITY = new java.util.EnumMap<>(Card.class);
    static {
        for (Card c : new Card[]{ONSLAUGHT, EXECUTIONER, JUGGERNAUT, GHOST, RISKY_MOVES,
                GLASS_CANNON, SECOND_WIND, UNDYING}) {
            RARITY.put(c, Rarity.LEGENDARY);
        }
        for (Card c : new Card[]{ENDURANCE, LIFESTEAL, REGENERATOR, BARRIER, CLEAVE, RETRIBUTION,
                THRESHERS_REACH, UNYIELDING_INFERNO, SPINE_CUTTER, TITAN, VAMPIRIC,
                PREDATOR, COMBO, MANGLE, RAMPAGE, ATTUNEMENT, HEADHUNTER, BASTION, ESCAPE_ARTIST}) {
            RARITY.put(c, Rarity.EPIC);
        }
        for (Card c : new Card[]{VITALITY, BULWARK, SWIFTNESS, STEADFAST, REACH, LEECH, BERSERKER,
                ADRENALINE, HASTE, FIRE_WALKER, VENOM, CRIT, BLOODLUST, STEADY_FEET, QUICKDRAW,
                KICK_OFF, CONDITIONED_RUNNER, PACK_LEADER, FORTRESS,
                FIRST_STRIKE, DUELIST, AERIAL, VENDETTA, NIGHT_STALKER, GIANT_SLAYER,
                SHIELDBREAKER, BRACED, DEFLECTION}) {
            RARITY.put(c, Rarity.RARE);
        }
    }

    public Rarity rarity() { return RARITY.getOrDefault(this, Rarity.COMMON); }
}
