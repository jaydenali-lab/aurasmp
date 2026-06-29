package com.aurasmp.ruin.ability;

import org.bukkit.Material;

/**
 * The 10 active abilities. Each is fired through the Ruin Catalyst item
 * (right-click = first ability, shift + right-click = second). The actual
 * behaviour lives in {@link AbilityManager#activate}.
 */
public enum Ability {

    BLINK("Blink", Material.ENDER_PEARL, "Teleport ~8 blocks where you look.", 9_000),
    SHOCKWAVE("Shockwave", Material.HEAVY_CORE, "Damage + knock back nearby enemies.", 12_000),
    VANISH("Vanish", Material.PHANTOM_MEMBRANE, "Invisibility + Speed II for 5s.", 25_000),
    BLOODTHIRST("Bloodthirst", Material.REDSTONE, "Instantly heal 3 hearts + Regen II.", 18_000),
    SMITE("Smite", Material.LIGHTNING_ROD, "Strike lightning at the enemy you face.", 14_000),
    LEAP("Leap", Material.RABBIT_FOOT, "Launch into the air, no fall damage.", 8_000),
    FROST_NOVA("Frost Nova", Material.BLUE_ICE, "Slow + damage all enemies near you.", 15_000),
    WITHER_TOUCH("Wither Touch", Material.WITHER_ROSE, "Wither II to nearby enemies for 6s.", 16_000),
    AURA_BURST("Aura Burst", Material.FIRE_CHARGE, "AoE blast around you (no block damage).", 20_000),
    MAGNETIZE("Magnetize", Material.LODESTONE, "Pull nearby enemies toward you.", 13_000);

    private final String displayName;
    private final Material icon;
    private final String description;
    private final long cooldownMillis;

    Ability(String displayName, Material icon, String description, long cooldownMillis) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.cooldownMillis = cooldownMillis;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String description() { return description; }
    public long cooldownMillis() { return cooldownMillis; }
}
