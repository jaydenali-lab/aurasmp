package com.aurasmp.ruin.ability;

import org.bukkit.Material;

/**
 * The 10 active Manifestations. Each is fired through the Ruin Catalyst item
 * (right-click = first, shift + right-click = second). Behaviour lives in
 * {@link AbilityManager#activate}.
 *
 * <p>{@code glyph} is the private-use character that maps to this manifestation's
 * custom icon in the {@code ruin:icons} resource-pack font (see the HUD).
 * {@code activeDurationMillis} is how long the caster's self-buff lasts (0 for
 * instant manifestations) — used to show the ACTIVE state in the HUD.
 */
public enum Ability {

    BLINK("Blink", Material.ENDER_PEARL, "Teleport ~8 blocks where you look.", 9_000, "", 0),
    SHOCKWAVE("Shockwave", Material.HEAVY_CORE, "Damage + knock back nearby enemies.", 12_000, "", 0),
    VANISH("Vanish", Material.PHANTOM_MEMBRANE, "Invisibility + Speed II for 5s.", 25_000, "", 5_000),
    BLOODTHIRST("Bloodthirst", Material.REDSTONE, "Instantly heal 3 hearts + Regen II.", 18_000, "", 4_000),
    SMITE("Smite", Material.LIGHTNING_ROD, "Strike lightning at the enemy you face.", 14_000, "", 0),
    LEAP("Leap", Material.RABBIT_FOOT, "Launch into the air, no fall damage.", 8_000, "", 5_000),
    FROST_NOVA("Frost Nova", Material.BLUE_ICE, "Slow + damage all enemies near you.", 15_000, "", 0),
    WITHER_TOUCH("Wither Touch", Material.WITHER_ROSE, "Wither II to nearby enemies for 6s.", 16_000, "", 0),
    AURA_BURST("Aura Burst", Material.FIRE_CHARGE, "AoE blast around you (no block damage).", 20_000, "", 0),
    MAGNETIZE("Magnetize", Material.LODESTONE, "Pull nearby enemies toward you.", 13_000, "", 0);

    private final String displayName;
    private final Material icon;
    private final String description;
    private final long cooldownMillis;
    private final String glyph;
    private final long activeDurationMillis;

    Ability(String displayName, Material icon, String description,
            long cooldownMillis, String glyph, long activeDurationMillis) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.cooldownMillis = cooldownMillis;
        this.glyph = glyph;
        this.activeDurationMillis = activeDurationMillis;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String description() { return description; }
    public long cooldownMillis() { return cooldownMillis; }
    public String glyph() { return glyph; }
    public long activeDurationMillis() { return activeDurationMillis; }
    public boolean hasActiveState() { return activeDurationMillis > 0; }
}
