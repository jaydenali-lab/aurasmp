package com.aurasmp.ruin.ability;

import org.bukkit.Material;

/**
 * The active Manifestations. Each is fired through the Ruin Catalyst item
 * (right-click = first, shift + right-click = second). Behaviour lives in
 * {@link AbilityManager#activate}.
 *
 * <p>{@code glyphCodepoint} is the private-use code point of this manifestation's
 * custom icon in the {@code ruin:icons} resource-pack font. {@code activeDurationMillis}
 * is how long the caster's self-buff lasts (0 for instant manifestations) — used to
 * show the ACTIVE state in the HUD.
 */
public enum Ability {

    BLINK("Blink", Material.ENDER_PEARL, "Teleport ~8 blocks where you look.", 9_000, 0xE001, 0),
    SHOCKWAVE("Shockwave", Material.HEAVY_CORE, "Damage + knock back nearby enemies.", 12_000, 0xE002, 0),
    VANISH("Vanish", Material.PHANTOM_MEMBRANE, "Invisibility + Speed II for 5s.", 25_000, 0xE003, 5_000),
    BLOODTHIRST("Bloodthirst", Material.REDSTONE, "Instantly heal 3 hearts + Regen II.", 18_000, 0xE004, 4_000),
    SMITE("Smite", Material.LIGHTNING_ROD, "Strike lightning at the enemy you face.", 14_000, 0xE005, 0),

    // ==== Expansion: 10 more manifestations ====
    FIREBALL("Fireball", Material.MAGMA_CREAM, "Hurl a fireball that explodes on impact.", 11_000, 0xE00B, 0),
    BERSERK("Berserk", Material.NETHERITE_AXE, "Strength + Speed + Resistance for 6s.", 30_000, 0xE00E, 6_000),
    METEOR("Meteor", Material.MAGMA_BLOCK, "Call down a meteor where you look after 1s.", 20_000, 0xE013, 0),

    // ==== Deepwoken mantras (8) ====
    FLAME_GRAB("Flame Grab", Material.BLAZE_ROD, "Lunge, seize a foe in flame and slam them.", 13_000, 0xE015, 0),
    WILDFIRE("Wildfire", Material.FIRE_CHARGE, "Erupt a cone of fire that ignites enemies.", 15_000, 0xE016, 0),
    TUNDRA("Tundra", Material.BLUE_ICE, "Freeze a wide zone, slowing all within.", 18_000, 0xE018, 0),
    WIND_SLAM("Wind Slam", Material.BREEZE_ROD, "Leap up and slam down, blasting foes back.", 13_000, 0xE01B, 0),
    GALE_STEP("Gale Step", Material.FEATHER, "Blink-dash on wind; no fall damage after.", 8_000, 0xE01C, 0),

    // ==== More manifestations ====
    PHASE_STRIKE("Phase Strike", Material.ENDER_EYE, "Blink to the enemy you face and strike.", 12_000, 0xE01D, 0),
    ZELKOVA("Zelkova", Material.COBWEB, "Two ground slams (true dmg); the 2nd stuns and locks the camera.", 20_000, 0xE022, 0),
    MOOK("Mook", Material.IRON_SWORD, "Dash in and slash rapidly for 8 damage.", 12_000, 0xE023, 0),

    // ==== 1.9.0: 10 more manifestations ====
    SHADOW_LANCE("Shadow Lance", Material.ECHO_SHARD, "Pierce all enemies in a line of shadow.", 14_000, 0xE024, 0),
    BLOOD_PACT("Blood Pact", Material.NETHER_WART, "Sacrifice 2 hearts; reset your other cooldowns.", 45_000, 0xE026, 0),
    RIPOSTE("Riposte", Material.SHIELD, "1.5s stance: parry the next hit and counter it.", 16_000, 0xE027, 1_500),
    ICE_BARRIER("Ice Barrier", Material.ICE, "Raise a wall of ice in front of you for 5s.", 15_000, 0xE028, 0),
    GUILLOTINE("Guillotine", Material.IRON_AXE, "Heavy blow: +20% of the target's missing health.", 15_000, 0xE029, 0),
    REWIND("Rewind", Material.CLOCK, "Mark yourself; 3s later snap back, restoring health.", 25_000, 0xE02A, 3_000),
    SINGULARITY("Singularity", Material.CRYING_OBSIDIAN, "Collapse a point that drags enemies in for 3s.", 16_000, 0xE02B, 0),

    // ==== 1.11.0: replacements for near-duplicates ====
    CRYOSTASIS("Cryostasis", Material.PACKED_ICE, "Encase yourself in ice: 2.5s invulnerable but frozen.", 25_000, 0xE007, 2_500),
    AEGIS("Aegis", Material.SHIELD, "4s ward that stops every projectile aimed at you.", 20_000, 0xE009, 4_000),
    HEX("Hex", Material.FERMENTED_SPIDER_EYE, "Curse the enemy you face with Weakness II.", 15_000, 0xE012, 0),

    // ==== 1.11.0: 19 more manifestations ====
    GRASPING_VINES("Grasping Vines", Material.VINE, "Root the enemy you face in place for 2s.", 14_000, 0xE02E, 0),
    VOLLEY("Volley", Material.ARROW, "Loose a fan of five arrows.", 12_000, 0xE030, 0),
    FISSURE("Fissure", Material.COARSE_DIRT, "Crack the ground in a line that erupts under foes.", 15_000, 0xE032, 0),
    LIFEDRAIN("Lifedrain", Material.GHAST_TEAR, "Channel 3s: siphon health from the enemy you face.", 18_000, 0xE033, 3_000),
    SILENCE("Silence", Material.SCULK_SHRIEKER, "Seal the enemy you face: no manifestations for 4s.", 10_000, 0xE036, 4_000),
    EMBER_MINE("Ember Mine", Material.CAMPFIRE, "Bury a fire mine that erupts when stepped on.", 18_000, 0xE038, 0),
    SUNDER("Sunder", Material.ANVIL, "Crack their guard: target takes +15% damage for 5s.", 15_000, 0xE03B, 0),
    COCOON("Cocoon", Material.STRING, "Wrap the enemy you face in cobwebs.", 16_000, 0xE03D, 0),
    TRUE_SIGHT("True Sight", Material.SPYGLASS, "Reveal every player within 20 blocks for 5s.", 20_000, 0xE03F, 0),
    RAILGUN("Railgun", Material.AMETHYST_SHARD, "Charge 1s, then fire a piercing hyper-beam. Counts as a projectile.", 22_000, 0xE041, 0);
    private final String displayName;
    private final Material icon;
    private final String description;
    private final long cooldownMillis;
    private final int glyphCodepoint;
    private final long activeDurationMillis;

    Ability(String displayName, Material icon, String description,
            long cooldownMillis, int glyphCodepoint, long activeDurationMillis) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.cooldownMillis = cooldownMillis;
        this.glyphCodepoint = glyphCodepoint;
        this.activeDurationMillis = activeDurationMillis;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String description() { return description; }
    public long cooldownMillis() { return cooldownMillis; }
    public String glyph() { return new String(Character.toChars(glyphCodepoint)); }
    /** Resource-pack item model that shows this manifestation's glyph as the icon. */
    public String modelKey() { return "glyph_" + name().toLowerCase(java.util.Locale.ROOT); }
    public long activeDurationMillis() { return activeDurationMillis; }
    public boolean hasActiveState() { return activeDurationMillis > 0; }
}
