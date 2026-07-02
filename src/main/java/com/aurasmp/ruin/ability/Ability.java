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
    LEAP("Leap", Material.RABBIT_FOOT, "Launch into the air, no fall damage.", 8_000, 0xE006, 5_000),
    FROST_NOVA("Frost Nova", Material.BLUE_ICE, "Slow + damage all enemies near you.", 15_000, 0xE007, 0),
    WITHER_TOUCH("Wither Touch", Material.WITHER_ROSE, "Wither II to nearby enemies for 6s.", 16_000, 0xE008, 0),
    AURA_BURST("Aura Burst", Material.FIRE_CHARGE, "AoE blast around you (no block damage).", 20_000, 0xE009, 0),
    MAGNETIZE("Magnetize", Material.LODESTONE, "Pull nearby enemies toward you.", 13_000, 0xE00A, 0),

    // ==== Expansion: 10 more manifestations ====
    FIREBALL("Fireball", Material.MAGMA_CREAM, "Hurl a fireball that explodes on impact.", 11_000, 0xE00B, 0),
    UPDRAFT("Updraft", Material.FEATHER, "Fling nearby enemies into the air.", 13_000, 0xE00C, 0),
    GRAPPLE("Launch", Material.FISHING_ROD, "Fire an arrow and ride it through the air.", 8_000, 0xE00D, 0),
    BERSERK("Berserk", Material.NETHERITE_AXE, "Strength II + Speed + Resistance for 6s.", 30_000, 0xE00E, 6_000),
    SMOKE_BOMB("Smoke Bomb", Material.GUNPOWDER, "Blind nearby foes; turn invisible for 3s.", 22_000, 0xE00F, 3_000),
    LIGHTNING_STORM("Lightning Storm", Material.TRIDENT, "Strike up to 3 nearby enemies with lightning.", 18_000, 0xE010, 0),
    SANCTUARY("Sanctuary", Material.TOTEM_OF_UNDYING, "Regen + Resistance + Absorption for 5s.", 28_000, 0xE011, 5_000),
    PLAGUE("Plague", Material.FERMENTED_SPIDER_EYE, "Poison II + Nausea to nearby enemies for 6s.", 16_000, 0xE012, 0),
    METEOR("Meteor", Material.MAGMA_BLOCK, "Call down a meteor where you look after 1s.", 20_000, 0xE013, 0),
    DASH("Dash", Material.SUGAR, "Dash forward with a burst of speed.", 7_000, 0xE014, 0),

    // ==== Deepwoken mantras (8) ====
    FLAME_GRAB("Flame Grab", Material.BLAZE_ROD, "Lunge, seize a foe in flame and slam them.", 13_000, 0xE015, 0),
    WILDFIRE("Wildfire", Material.FIRE_CHARGE, "Erupt a cone of fire that ignites enemies.", 15_000, 0xE016, 0),
    FROSTDRAW_SPIKES("Frostdraw Spikes", Material.PACKED_ICE, "Ice spikes impale and chill foes ahead.", 12_000, 0xE017, 0),
    TUNDRA("Tundra", Material.BLUE_ICE, "Freeze a wide zone, slowing all within.", 18_000, 0xE018, 0),
    SHOCK_SWORD("Shock Sword", Material.LIGHTNING_ROD, "A sweeping arc slash that slows.", 11_000, 0xE019, 0),
    GALVANIZE("Galvanize", Material.COPPER_INGOT, "Electrify: Haste, Speed & Strength for 6s.", 24_000, 0xE01A, 6_000),
    WIND_SLAM("Wind Slam", Material.BREEZE_ROD, "Leap up and slam down, blasting foes back.", 13_000, 0xE01B, 0),
    GALE_STEP("Gale Step", Material.FEATHER, "Blink-dash on wind; no fall damage after.", 8_000, 0xE01C, 0),

    // ==== More manifestations ====
    PHASE_STRIKE("Phase Strike", Material.ENDER_EYE, "Blink to the enemy you face and strike.", 12_000, 0xE01D, 0),
    ZELKOVA("Zelkova", Material.COBWEB, "Two ground slams (true dmg); the 2nd stuns and locks the camera.", 20_000, 0xE022, 0),
    MOOK("Mook", Material.IRON_SWORD, "Dash in and slash rapidly for 12 damage.", 12_000, 0xE023, 0),

    // ==== 1.9.0: 10 more manifestations ====
    SHADOW_LANCE("Shadow Lance", Material.ECHO_SHARD, "Pierce all enemies in a line of shadow.", 14_000, 0xE024, 0),
    ASTRAL_WIND("Astral Wind", Material.WIND_CHARGE, "A 5s storm aura that shoves enemies away.", 18_000, 0xE025, 5_000),
    BLOOD_PACT("Blood Pact", Material.NETHER_WART, "Sacrifice 2 hearts; reset your other cooldowns.", 45_000, 0xE026, 0),
    RIPOSTE("Riposte", Material.SHIELD, "1.5s stance: parry the next hit and counter it.", 16_000, 0xE027, 1_500),
    ICE_BARRIER("Ice Barrier", Material.ICE, "Raise a wall of ice in front of you for 5s.", 15_000, 0xE028, 0),
    GUILLOTINE("Guillotine", Material.IRON_AXE, "Heavy blow: +25% of the target's missing health.", 15_000, 0xE029, 0),
    REWIND("Rewind", Material.CLOCK, "Mark yourself; 3s later snap back, restoring health.", 25_000, 0xE02A, 3_000),
    SINGULARITY("Singularity", Material.CRYING_OBSIDIAN, "Collapse a point that drags enemies in for 3s.", 16_000, 0xE02B, 0),
    DISPLACE("Displace", Material.CHORUS_FRUIT, "Swap positions with the enemy you face.", 12_000, 0xE02C, 0),
    MIASMA("Miasma", Material.DRAGON_BREATH, "Leave a lingering poison cloud where you look.", 15_000, 0xE02D, 0);

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
    public long activeDurationMillis() { return activeDurationMillis; }
    public boolean hasActiveState() { return activeDurationMillis > 0; }
}
