package com.aurasmp.ruin.ability;

import com.aurasmp.ruin.stat.Stat;
import org.bukkit.Material;

/**
 * The 60 manifestations — 10 per stat, drawn from manifestation hands and
 * cast via their own bound cast items. Behaviour lives in AbilityManager.
 *
 * <p>{@code glyphCodepoint} is the private-use code point of this manifestation's
 * icon in the {@code ruin:icons} font; {@code activeDurationMillis} drives the
 * ACTIVE state shown in the HUD (0 for instant casts).
 */
public enum Ability {

    // ================= STRENGTH =================
    SHOCKWAVE("Shockwave", Material.HEAVY_CORE, "Damage + knock back nearby enemies.", 12_000, 0xE002, 0),
    MOOK("Mook", Material.IRON_SWORD, "Dash in and slash rapidly for 8 damage.", 12_000, 0xE023, 0),
    PHASE_STRIKE("Phase Strike", Material.ENDER_EYE, "Blink to the enemy you face and strike.", 12_000, 0xE01D, 0),
    FLAME_GRAB("Flame Grab", Material.BLAZE_ROD, "Lunge, seize a foe in flame and slam them.", 13_000, 0xE015, 0),
    FISSURE("Fissure", Material.COARSE_DIRT, "Crack the ground in a line that erupts under foes.", 15_000, 0xE032, 0),
    GUILLOTINE("Guillotine", Material.IRON_AXE, "Heavy blow: +20% of the target's missing health.", 15_000, 0xE029, 0),
    RIPOSTE("Riposte", Material.SHIELD, "1.5s stance: parry the next hit and counter it.", 16_000, 0xE027, 1_500),
    EARTHSPLITTER("Earthsplitter", Material.MACE, "Smash the ground: a heavy cone slam that launches enemies.", 14_000, 0xE050, 0),
    BERSERK("Berserk", Material.NETHERITE_AXE, "Strength + Speed + Resistance for 6s.", 30_000, 0xE00E, 6_000),
    ZELKOVA("Zelkova", Material.COBWEB, "Two ground slams (true dmg); the 2nd stuns and locks the camera.", 20_000, 0xE022, 0),
    // ================= AGILITY =================
    BLINK("Blink", Material.ENDER_PEARL, "Teleport ~8 blocks where you look.", 9_000, 0xE001, 0),
    DASH("Dash", Material.SUGAR, "Burst-dash in your look direction.", 6_000, 0xE051, 0),
    UPDRAFT("Updraft", Material.WIND_CHARGE, "Ride a gust high into the air; land softly.", 10_000, 0xE052, 0),
    GALE_STEP("Gale Step", Material.FEATHER, "Blink-dash on wind; no fall damage after.", 8_000, 0xE01C, 0),
    WIND_SLAM("Wind Slam", Material.BREEZE_ROD, "Leap up and slam down, blasting foes back.", 13_000, 0xE01B, 0),
    SMOKE_BOMB("Smoke Bomb", Material.GUNPOWDER, "Drop a smoke cloud: blinds enemies, quickens you.", 14_000, 0xE053, 0),
    FLICKER("Flicker", Material.ENDER_PEARL, "Slip instantly behind the enemy you face.", 8_000, 0xE054, 0),
    TEMPO("Tempo", Material.GOLD_NUGGET, "5s rhythm: faster swings, and your next hit deals +3.", 15_000, 0xE056, 5_000),
    VANISH("Vanish", Material.PHANTOM_MEMBRANE, "Invisibility + Speed II for 5s.", 25_000, 0xE003, 5_000),
    AFTERIMAGE("Afterimage", Material.PHANTOM_MEMBRANE, "Dash backward, going briefly invisible in a decoy puff.", 16_000, 0xE055, 1_500),
    // ================= INTELLECT =================
    FIREBALL("Fireball", Material.MAGMA_CREAM, "Hurl a fireball that explodes on impact.", 11_000, 0xE00B, 0),
    VOLLEY("Volley", Material.ARROW, "Loose a fan of five arrows.", 12_000, 0xE030, 0),
    SMITE("Smite", Material.LIGHTNING_ROD, "Strike lightning at the enemy you face.", 14_000, 0xE005, 0),
    SHADOW_LANCE("Shadow Lance", Material.ECHO_SHARD, "Pierce all enemies in a line of shadow.", 14_000, 0xE024, 0),
    EMBER_MINE("Ember Mine", Material.CAMPFIRE, "Bury a fire mine that erupts when stepped on.", 18_000, 0xE038, 0),
    WILDFIRE("Wildfire", Material.FIRE_CHARGE, "Erupt a cone of fire that ignites enemies.", 15_000, 0xE016, 0),
    TRUE_SIGHT("True Sight", Material.SPYGLASS, "Reveal every player within 20 blocks for 5s.", 20_000, 0xE03F, 0),
    METEOR("Meteor", Material.MAGMA_BLOCK, "Call down a meteor where you look after 1s.", 20_000, 0xE013, 0),
    SINGULARITY("Singularity", Material.CRYING_OBSIDIAN, "Collapse a point that drags enemies in for 3s.", 16_000, 0xE02B, 0),
    RAILGUN("Railgun", Material.AMETHYST_SHARD, "Charge 1s, then fire a piercing hyper-beam. Counts as a projectile.", 22_000, 0xE041, 0),
    // ================= FORTITUDE =================
    ICE_BARRIER("Ice Barrier", Material.ICE, "Raise a wall of ice in front of you for 5s.", 15_000, 0xE028, 0),
    STONE_SKIN("Stone Skin", Material.STONE, "Resistance II for 5s, at a slight slow.", 16_000, 0xE057, 5_000),
    SEISMIC_GUARD("Seismic Guard", Material.DEEPSLATE, "4s stance: attackers are hurled away on contact.", 14_000, 0xE05D, 4_000),
    AEGIS("Aegis", Material.SHIELD, "4s ward that stops every projectile aimed at you.", 20_000, 0xE009, 4_000),
    CHALLENGE("Challenge", Material.GOAT_HORN, "Mark nearby enemies with glow and steel yourself.", 15_000, 0xE058, 3_000),
    FORTIFY("Fortify", Material.IRON_INGOT, "Trade 2 hearts for 4 absorption hearts.", 18_000, 0xE059, 0),
    BRACE("Brace", Material.ANVIL, "4s: unmovable and 30% tougher, but you can't sprint.", 16_000, 0xE05B, 4_000),
    CRYOSTASIS("Cryostasis", Material.PACKED_ICE, "Encase yourself in ice: 2.5s invulnerable but frozen.", 25_000, 0xE007, 2_500),
    REFLECT_WARD("Reflect Ward", Material.SHIELD, "3s ward: melee attackers eat their own damage.", 20_000, 0xE05A, 3_000),
    IRON_DOME("Iron Dome", Material.LODESTONE, "5s dome that dissolves incoming projectiles near you.", 22_000, 0xE05C, 5_000),
    // ================= WILLPOWER =================
    BLOODTHIRST("Bloodthirst", Material.REDSTONE, "Instantly heal 3 hearts + Regen II.", 18_000, 0xE004, 4_000),
    PURGE("Purge", Material.MILK_BUCKET, "Cleanse yourself and nearby allies of debuffs.", 16_000, 0xE061, 0),
    COMMUNION("Communion", Material.GLOW_BERRIES, "Mend the ally you look at for 4 hearts.", 12_000, 0xE063, 0),
    LIFEDRAIN("Lifedrain", Material.GHAST_TEAR, "Channel 3s: siphon health from the enemy you face.", 18_000, 0xE033, 3_000),
    VITAL_SURGE("Vital Surge", Material.GLISTERING_MELON_SLICE, "Cleanse your debuffs + Regen II for 5s.", 18_000, 0xE05E, 0),
    VAMPIRIC_BURST("Vampiric Burst", Material.REDSTONE, "Drain all enemies near you; heal for each one hit.", 15_000, 0xE062, 0),
    REWIND("Rewind", Material.CLOCK, "Mark yourself; 3s later snap back, restoring health.", 25_000, 0xE02A, 3_000),
    PACT_OF_PAIN("Pact of Pain", Material.WITHER_ROSE, "Sacrifice 3 hearts for Strength II for 8s.", 20_000, 0xE05F, 8_000),
    BLOOD_PACT("Blood Pact", Material.NETHER_WART, "Sacrifice 2 hearts; reset your other cooldowns.", 45_000, 0xE026, 0),
    SECOND_SOUL("Second Soul", Material.TOTEM_OF_UNDYING, "30s pact: the next fatal blow leaves you at 2 hearts.", 45_000, 0xE060, 30_000),
    // ================= CHARISMA =================
    HEX("Hex", Material.FERMENTED_SPIDER_EYE, "Curse the enemy you face with Weakness II.", 15_000, 0xE012, 0),
    GRASPING_VINES("Grasping Vines", Material.VINE, "Root the enemy you face in place for 2s.", 14_000, 0xE02E, 0),
    SUNDER("Sunder", Material.ANVIL, "Crack their guard: target takes +15% damage for 5s.", 15_000, 0xE03B, 0),
    COCOON("Cocoon", Material.STRING, "Wrap the enemy you face in cobwebs.", 16_000, 0xE03D, 0),
    CHARM("Charm", Material.HONEY_BOTTLE, "Befuddle the enemy you face: nausea, weakness, glowing.", 14_000, 0xE066, 0),
    TUNDRA("Tundra", Material.BLUE_ICE, "Freeze a wide zone, slowing all within.", 18_000, 0xE018, 0),
    TERROR("Terror", Material.SCULK, "Cone of dread: blind and slow everyone in front of you.", 16_000, 0xE065, 0),
    SILENCE("Silence", Material.SCULK_SHRIEKER, "Seal the enemy you face: no manifestations for 4s.", 10_000, 0xE036, 4_000),
    MASS_HEX("Mass Hex", Material.FERMENTED_SPIDER_EYE, "Weakness II on every enemy within 6 blocks.", 18_000, 0xE064, 0),
    WARCRY("Warcry", Material.BELL, "Rally allies within 8 blocks: Strength + Speed for 6s.", 25_000, 0xE067, 0);

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
    public int glyphCodepoint() { return glyphCodepoint; }
    /** Resource-pack item model that shows this manifestation's art as the icon. */
    public String modelKey() { return "glyph_" + name().toLowerCase(java.util.Locale.ROOT); }
    public long activeDurationMillis() { return activeDurationMillis; }
    public boolean hasActiveState() { return activeDurationMillis > 0; }

    public Stat stat() { return STAT.get(this); }
    /** Stat investment required before this manifestation can appear in a hand. */
    public int requirement() { return REQ.getOrDefault(this, 0); }

    private static final java.util.EnumMap<Ability, Stat> STAT = new java.util.EnumMap<>(Ability.class);
    private static final java.util.EnumMap<Ability, Integer> REQ = new java.util.EnumMap<>(Ability.class);
    static {
        STAT.put(SHOCKWAVE, Stat.STRENGTH); REQ.put(SHOCKWAVE, 0);
        STAT.put(MOOK, Stat.STRENGTH); REQ.put(MOOK, 10);
        STAT.put(PHASE_STRIKE, Stat.STRENGTH); REQ.put(PHASE_STRIKE, 25);
        STAT.put(FLAME_GRAB, Stat.STRENGTH); REQ.put(FLAME_GRAB, 25);
        STAT.put(FISSURE, Stat.STRENGTH); REQ.put(FISSURE, 25);
        STAT.put(GUILLOTINE, Stat.STRENGTH); REQ.put(GUILLOTINE, 40);
        STAT.put(RIPOSTE, Stat.STRENGTH); REQ.put(RIPOSTE, 40);
        STAT.put(EARTHSPLITTER, Stat.STRENGTH); REQ.put(EARTHSPLITTER, 60);
        STAT.put(BERSERK, Stat.STRENGTH); REQ.put(BERSERK, 80);
        STAT.put(ZELKOVA, Stat.STRENGTH); REQ.put(ZELKOVA, 80);
        STAT.put(BLINK, Stat.AGILITY); REQ.put(BLINK, 0);
        STAT.put(DASH, Stat.AGILITY); REQ.put(DASH, 10);
        STAT.put(UPDRAFT, Stat.AGILITY); REQ.put(UPDRAFT, 10);
        STAT.put(GALE_STEP, Stat.AGILITY); REQ.put(GALE_STEP, 25);
        STAT.put(WIND_SLAM, Stat.AGILITY); REQ.put(WIND_SLAM, 25);
        STAT.put(SMOKE_BOMB, Stat.AGILITY); REQ.put(SMOKE_BOMB, 25);
        STAT.put(FLICKER, Stat.AGILITY); REQ.put(FLICKER, 40);
        STAT.put(TEMPO, Stat.AGILITY); REQ.put(TEMPO, 40);
        STAT.put(VANISH, Stat.AGILITY); REQ.put(VANISH, 60);
        STAT.put(AFTERIMAGE, Stat.AGILITY); REQ.put(AFTERIMAGE, 60);
        STAT.put(FIREBALL, Stat.INTELLECT); REQ.put(FIREBALL, 0);
        STAT.put(VOLLEY, Stat.INTELLECT); REQ.put(VOLLEY, 0);
        STAT.put(SMITE, Stat.INTELLECT); REQ.put(SMITE, 25);
        STAT.put(SHADOW_LANCE, Stat.INTELLECT); REQ.put(SHADOW_LANCE, 25);
        STAT.put(EMBER_MINE, Stat.INTELLECT); REQ.put(EMBER_MINE, 40);
        STAT.put(WILDFIRE, Stat.INTELLECT); REQ.put(WILDFIRE, 40);
        STAT.put(TRUE_SIGHT, Stat.INTELLECT); REQ.put(TRUE_SIGHT, 40);
        STAT.put(METEOR, Stat.INTELLECT); REQ.put(METEOR, 60);
        STAT.put(SINGULARITY, Stat.INTELLECT); REQ.put(SINGULARITY, 60);
        STAT.put(RAILGUN, Stat.INTELLECT); REQ.put(RAILGUN, 80);
        STAT.put(ICE_BARRIER, Stat.FORTITUDE); REQ.put(ICE_BARRIER, 0);
        STAT.put(STONE_SKIN, Stat.FORTITUDE); REQ.put(STONE_SKIN, 10);
        STAT.put(SEISMIC_GUARD, Stat.FORTITUDE); REQ.put(SEISMIC_GUARD, 10);
        STAT.put(AEGIS, Stat.FORTITUDE); REQ.put(AEGIS, 25);
        STAT.put(CHALLENGE, Stat.FORTITUDE); REQ.put(CHALLENGE, 25);
        STAT.put(FORTIFY, Stat.FORTITUDE); REQ.put(FORTIFY, 40);
        STAT.put(BRACE, Stat.FORTITUDE); REQ.put(BRACE, 40);
        STAT.put(CRYOSTASIS, Stat.FORTITUDE); REQ.put(CRYOSTASIS, 60);
        STAT.put(REFLECT_WARD, Stat.FORTITUDE); REQ.put(REFLECT_WARD, 60);
        STAT.put(IRON_DOME, Stat.FORTITUDE); REQ.put(IRON_DOME, 80);
        STAT.put(BLOODTHIRST, Stat.WILLPOWER); REQ.put(BLOODTHIRST, 0);
        STAT.put(PURGE, Stat.WILLPOWER); REQ.put(PURGE, 10);
        STAT.put(COMMUNION, Stat.WILLPOWER); REQ.put(COMMUNION, 10);
        STAT.put(LIFEDRAIN, Stat.WILLPOWER); REQ.put(LIFEDRAIN, 25);
        STAT.put(VITAL_SURGE, Stat.WILLPOWER); REQ.put(VITAL_SURGE, 25);
        STAT.put(VAMPIRIC_BURST, Stat.WILLPOWER); REQ.put(VAMPIRIC_BURST, 25);
        STAT.put(REWIND, Stat.WILLPOWER); REQ.put(REWIND, 40);
        STAT.put(PACT_OF_PAIN, Stat.WILLPOWER); REQ.put(PACT_OF_PAIN, 40);
        STAT.put(BLOOD_PACT, Stat.WILLPOWER); REQ.put(BLOOD_PACT, 60);
        STAT.put(SECOND_SOUL, Stat.WILLPOWER); REQ.put(SECOND_SOUL, 80);
        STAT.put(HEX, Stat.CHARISMA); REQ.put(HEX, 0);
        STAT.put(GRASPING_VINES, Stat.CHARISMA); REQ.put(GRASPING_VINES, 10);
        STAT.put(SUNDER, Stat.CHARISMA); REQ.put(SUNDER, 25);
        STAT.put(COCOON, Stat.CHARISMA); REQ.put(COCOON, 25);
        STAT.put(CHARM, Stat.CHARISMA); REQ.put(CHARM, 25);
        STAT.put(TUNDRA, Stat.CHARISMA); REQ.put(TUNDRA, 40);
        STAT.put(TERROR, Stat.CHARISMA); REQ.put(TERROR, 40);
        STAT.put(SILENCE, Stat.CHARISMA); REQ.put(SILENCE, 60);
        STAT.put(MASS_HEX, Stat.CHARISMA); REQ.put(MASS_HEX, 60);
        STAT.put(WARCRY, Stat.CHARISMA); REQ.put(WARCRY, 80);
    }
}