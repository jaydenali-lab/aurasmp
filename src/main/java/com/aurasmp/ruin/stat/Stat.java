package com.aurasmp.ruin.stat;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

/**
 * The six Deepwoken-style attributes. Every talent and manifestation carries a
 * stat requirement; players invest the points they earn from levelling.
 *
 * <p>Scaling per invested point:
 * <ul>
 *   <li><b>Strength</b> — +0.35% armor penetration (your hits ignore that share
 *       of the victim's armor reduction).</li>
 *   <li><b>Fortitude</b> — +0.5% damage resistance.</li>
 *   <li><b>Agility</b> — +0.25% movement speed.</li>
 *   <li><b>Intellect</b> — +0.3% manifestation damage, 0.2% shorter
 *       manifestation cooldowns.</li>
 *   <li><b>Willpower</b> — debuffs on you are 0.4% shorter.</li>
 *   <li><b>Charisma</b> — debuffs you inflict last 0.5% longer.</li>
 * </ul>
 */
public enum Stat {

    STRENGTH("Strength", NamedTextColor.RED, Material.IRON_SWORD,
            "+0.35% armor penetration per point"),
    FORTITUDE("Fortitude", NamedTextColor.GOLD, Material.IRON_CHESTPLATE,
            "+0.5% damage resistance per point"),
    AGILITY("Agility", NamedTextColor.GREEN, Material.FEATHER,
            "+0.25% movement speed per point"),
    INTELLECT("Intellect", NamedTextColor.AQUA, Material.AMETHYST_SHARD,
            "+0.3% manifestation damage, -0.2% cooldowns per point"),
    WILLPOWER("Willpower", NamedTextColor.LIGHT_PURPLE, Material.END_CRYSTAL,
            "debuffs on you are 0.4% shorter per point"),
    CHARISMA("Charisma", NamedTextColor.YELLOW, Material.GOLD_INGOT,
            "your debuffs last 0.5% longer per point");

    /** Hard cap per stat. */
    public static final int CAP = 100;
    /** Total stat points everybody can earn in a run. */
    public static final int TOTAL_POINTS = 330;
    /** Stat points granted per level-up (until the 330 pool is exhausted). */
    public static final int POINTS_PER_LEVEL = 15;

    private final String displayName;
    private final NamedTextColor color;
    private final Material icon;
    private final String scaling;

    Stat(String displayName, NamedTextColor color, Material icon, String scaling) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.scaling = scaling;
    }

    public String displayName() { return displayName; }
    public NamedTextColor color() { return color; }
    public Material icon() { return icon; }
    public String scaling() { return scaling; }

    /** Total stat points a player has earned by reaching {@code level}. */
    public static int earnedPoints(int level) {
        return Math.min(TOTAL_POINTS, POINTS_PER_LEVEL * (level - 1));
    }
}
