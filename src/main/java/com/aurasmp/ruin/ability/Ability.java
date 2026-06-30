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
    DASH("Dash", Material.SUGAR, "Dash forward with a burst of speed.", 7_000, 0xE014, 0);

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
