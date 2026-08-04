package com.aurasmp.ruin.card;

import com.aurasmp.ruin.stat.Stat;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

/**
 * The 150 talents — 25 per stat, gated by stat requirements and drawn from
 * level-up talent hands. Attribute cards apply persistent modifiers
 * (CardManager); flag cards resolve in the combat/passive listeners.
 */
public enum Card {

    // ================= STRENGTH =================
    FIRST_STRIKE("First Strike", Material.WOODEN_SWORD, "+6% melee damage to full-health targets."),
    BRUTE_FORCE("Brute Force", Material.STONE_AXE, "+5% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.05),
    HEAVY_HANDS("Heavy Hands", Material.SLIME_BALL, "Your hits knock enemies back harder.",
            Attribute.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_NUMBER, 0.5),
    FENCER("Fencer", Material.IRON_SWORD, "+7% damage while holding a sword."),
    SPEARHEAD("Spearhead", Material.TRIDENT, "+10% damage while holding a spear."),
    HAYMAKER("Haymaker", Material.SLIME_BALL, "Your melee hits knock enemies back much further.",
            Attribute.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_NUMBER, 0.5),
    BULLY("Bully", Material.LEATHER, "+12% damage to targets not holding a weapon."),
    SLUGGER("Slugger", Material.RABBIT_FOOT, "+8% melee damage while sprinting."),
    FRENZY("Frenzy", Material.GOLDEN_SWORD, "+10% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.1),
    COMBO("Combo", Material.CLOCK, "Consecutive hits on the same target stack +2% damage (up to +6%)."),
    SKEWER("Skewer", Material.ARROW, "Spear hits from 3.5+ blocks away deal +1.5 bonus damage."),
    BERSERKER("Berserker", Material.BLAZE_POWDER, "+7% damage while below 30% health."),
    CLEAVE("Cleave", Material.DIAMOND_AXE, "Melee hits splash 30% damage to nearby enemies."),
    BONEBREAKER("Bonebreaker", Material.BONE, "+20% damage to airborne enemies."),
    ONSLAUGHT("Onslaught", Material.DIAMOND_SWORD, "+7% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.07),
    SPINE_CUTTER("Spine Cutter", Material.NETHERITE_SWORD, "Backstabs deal +1.5 damage."),
    RAMPAGE("Rampage", Material.WITHER_SKELETON_SKULL, "Kills grant +2% damage for 20s, stacking 3 times."),
    UNYIELDING_INFERNO("Unyielding Inferno", Material.BLAZE_POWDER, "+1.5 hearts to hits on burning foes."),
    SIEGEBREAKER("Siegebreaker", Material.MACE, "+15% damage to heavily armored targets."),
    TITANS_WRATH("Titan's Wrath", Material.GOLDEN_AXE, "+15% melee damage while above 90% health."),
    GLASS_CANNON("Glass Cannon", Material.TNT, "+15% melee damage, but take 20% more damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.15),
    EXECUTIONER("Executioner", Material.IRON_AXE, "+30% damage to targets below 20% health."),
    CRUSHER("Crusher", Material.ANVIL, "Your hits smash through raised shields, disabling them for 3s."),
    CONCUSSIVE_BLOWS("Concussive Blows", Material.NETHERITE_AXE, "Every 5th axe hit stuns the target for 1.5s."),
    WARLORD("Warlord", Material.NETHERITE_SWORD, "+10% melee damage.",
            Attribute.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_SCALAR, 0.1),
    // ================= FORTITUDE =================
    BULWARK("Bulwark", Material.IRON_CHESTPLATE, "+3 armor.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 3.0),
    TOUGH_SKIN("Tough Skin", Material.LEATHER_CHESTPLATE, "+1 armor.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 1.0),
    PADDING("Padding", Material.WHITE_WOOL, "Take 50% less fall damage."),
    STEADFAST("Steadfast", Material.NETHERITE_INGOT, "+40% knockback resistance.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.4),
    IRONCLAD("Ironclad", Material.IRON_BLOCK, "+2 armor toughness.",
            Attribute.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_NUMBER, 2.0),
    CHAINWEAVE("Chainweave", Material.CHAIN, "Take 10% less projectile damage."),
    DEFLECTION("Deflection", Material.ARROW, "Take 20% less projectile damage."),
    PACK_LEADER("Pack Leader", Material.WOLF_ARMOR, "Resistance while an ally fights beside you."),
    FIREPROOF("Fireproof", Material.MAGMA_CREAM, "Take 50% less fire damage."),
    BLASTGUARD("Blastguard", Material.OBSIDIAN, "Take 40% less explosion damage."),
    STONEWALL("Stonewall", Material.COBBLESTONE_WALL, "+30% knockback resistance.",
            Attribute.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_NUMBER, 0.3),
    ANVIL_STANCE("Anvil Stance", Material.ANVIL, "Take 25% less damage while sneaking."),
    REINFORCED("Reinforced", Material.ARMOR_STAND, "Your armor never loses durability."),
    TITAN("Titan", Material.ENCHANTED_GOLDEN_APPLE, "+4 hearts of max health, but 20% slower.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 8.0),
    SECOND_SKIN("Second Skin", Material.TURTLE_SCUTE, "Take 15% less damage while above 90% health."),
    BULKHEAD("Bulkhead", Material.IRON_DOOR, "+4 armor, but 10% slower.",
            Attribute.ARMOR, AttributeModifier.Operation.ADD_NUMBER, 4.0),
    IMMOVABLE("Immovable", Material.LODESTONE, "Levitation never sticks to you."),
    THORNPLATE("Thornplate", Material.PUFFERFISH, "Reflect 15% of melee damage you take back at the attacker."),
    RISKY_MOVES("Risky Moves", Material.PHANTOM_MEMBRANE, "15% chance to fully negate an incoming hit."),
    LAST_BASTION("Last Bastion", Material.SHIELD, "Take 20% less damage while below 25% health."),
    HARDENED("Hardened", Material.DEEPSLATE, "Every hit you take deals 1 less damage."),
    GUARDIAN("Guardian", Material.SHIELD, "Trusted allies within 8 blocks take 10% less damage."),
    JUGGERNAUT("Juggernaut", Material.NETHERITE_CHESTPLATE, "Take 10% less damage from all sources."),
    COLOSSUS("Colossus", Material.NETHERITE_BLOCK, "+6 hearts of max health, but you deal 15% less damage.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 12.0),
    AEGIS_SOUL("Aegis Soul", Material.NAUTILUS_SHELL, "After 8s without being hit, gain 2 absorption hearts."),
    // ================= AGILITY =================
    SWIFTNESS("Swiftness", Material.SUGAR, "+10% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.1),
    SPRINTER("Sprinter", Material.LEATHER_BOOTS, "+5% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.05),
    SPRING_STEP("Spring Step", Material.SLIME_BLOCK, "Permanent Jump Boost I."),
    FEATHER("Feather", Material.FEATHER, "Immune to fall damage."),
    ACROBAT("Acrobat", Material.LEAD, "Take 70% less fall damage."),
    SWIFT_STRIKES("Swift Strikes", Material.GOLDEN_SWORD, "+7% attack speed.",
            Attribute.ATTACK_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.07),
    LONG_STRIDE("Long Stride", Material.SCAFFOLDING, "Step up full blocks without jumping.",
            Attribute.STEP_HEIGHT, AttributeModifier.Operation.ADD_NUMBER, 0.6),
    BLUR("Blur", Material.SUGAR, "Gain Speed II for 2s after taking a hit."),
    UNTRACKABLE("Untrackable", Material.GLOW_INK_SAC, "Glowing never sticks to you."),
    ADRENALINE("Adrenaline", Material.SUGAR, "Speed II + Regen I for 4s after a kill."),
    PARKOUR("Parkour", Material.RABBIT_FOOT, "Permanent Jump Boost II."),
    HIT_AND_RUN("Hit and Run", Material.FEATHER, "Speed I for 3s after landing a melee hit."),
    SIDESTEP("Sidestep", Material.ARROW, "10% chance to fully dodge projectile damage."),
    BLINDSIDE("Blindside", Material.STONE_SWORD, "Backstabs deal +25% damage."),
    ESCAPE_ARTIST("Escape Artist", Material.LEAD, "Slowness never sticks to you."),
    WIND_SPRINT("Wind Sprint", Material.WHITE_DYE, "Permanent Speed I."),
    TAILWIND("Tailwind", Material.PHANTOM_MEMBRANE, "Speed III for 4s after a kill."),
    EVASIVE_ROLL("Evasive Roll", Material.STRING, "8% chance to fully dodge melee damage."),
    WINDRUNNER("Windrunner", Material.ELYTRA, "+8% movement speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.08),
    VANTAGE("Vantage", Material.SPYGLASS, "+15% damage to enemies below you."),
    DODGE_ROLL("Dodge Roll", Material.PHANTOM_MEMBRANE, "15% chance to dodge melee hits while sprinting."),
    AERIAL_ACE("Aerial Ace", Material.FEATHER, "+20% melee damage while you are airborne."),
    FEATHERWEIGHT("Featherweight", Material.GOLD_NUGGET, "+5% speed and +5% attack speed.",
            Attribute.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_SCALAR, 0.05),
    GHOST("Ghost", Material.PHANTOM_MEMBRANE, "15% chance when hit to fully vanish (armor too) + Speed II for 3s."),
    GALE_FORM("Gale Form", Material.ELYTRA, "Permanent Speed II, but take 10% more damage."),
    // ================= INTELLECT =================
    SHARPSHOOTER("Sharpshooter", Material.BOW, "+5% projectile damage."),
    STUDIOUS("Studious", Material.BOOK, "+25% Ruin XP from kills."),
    KEEN_MIND("Keen Mind", Material.GLASS_BOTTLE, "Nausea and mining fatigue never stick to you."),
    SCAVENGER("Scavenger", Material.EXPERIENCE_BOTTLE, "+50% Ruin XP from kills."),
    ARCANIST("Arcanist", Material.AMETHYST_SHARD, "+5% manifestation damage."),
    CHANNELER("Channeler", Material.END_ROD, "Manifestation cooldowns are 5% shorter."),
    MINDFUL("Mindful", Material.LILY_OF_THE_VALLEY, "Slowly regenerate while no enemies are within 10 blocks."),
    CLARITY("Clarity", Material.GLASS, "Blindness, nausea and darkness never stick to you."),
    SPELLWARD("Spellward", Material.SHULKER_SHELL, "Take 30% less magic damage."),
    ALCHEMIST("Alchemist", Material.BREWING_STAND, "Potions you drink last 30% longer."),
    CALCULATED("Calculated", Material.CROSSBOW, "+10% projectile damage."),
    STARGAZER("Stargazer", Material.NETHER_STAR, "+15% manifestation damage at night."),
    FOCUS("Focus", Material.ENDER_EYE, "Silence on you is 50% shorter."),
    ATTUNEMENT("Attunement", Material.AMETHYST_SHARD, "Your manifestation cooldowns are 15% shorter."),
    SEER("Seer", Material.SPYGLASS, "Invisible players within 8 blocks are revealed to you."),
    WARDING_SIGILS("Warding Sigils", Material.CHISELED_BOOKSHELF, "Debuffs on you are 15% shorter."),
    SPELL_THIEF("Spell Thief", Material.ECHO_SHARD, "Kills refund 3s of every manifestation cooldown."),
    TACTICIAN("Tactician", Material.MAP, "+10% melee damage for 3s after casting a manifestation."),
    BATTLE_RUSH("Battle Rush", Material.RECOVERY_COMPASS, "Player kills refund half your manifestation cooldowns."),
    OVERMIND("Overmind", Material.ENDER_CHEST, "+10% manifestation damage."),
    GLASS_MIND("Glass Mind", Material.TINTED_GLASS, "Cooldowns 10% shorter, but take 10% more damage."),
    BATTLE_MAGE("Battle Mage", Material.BLAZE_ROD, "Melee hits shave 0.5s off your manifestation cooldowns."),
    DAMPEN("Dampen", Material.SCULK_SENSOR, "Take 15% less damage from manifestations."),
    ECHO("Echo", Material.ECHO_SHARD, "10% chance a cast triggers no cooldown at all."),
    PRODIGY("Prodigy", Material.ENCHANTING_TABLE, "+5% manifestation damage and 5% shorter cooldowns."),
    // ================= WILLPOWER =================
    VITALITY("Vitality", Material.RED_DYE, "+2 hearts of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 4.0),
    HEARTY("Hearty", Material.APPLE, "+1 heart of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 2.0),
    SLOW_METABOLISM("Slow Metabolism", Material.BREAD, "Your hunger drains 50% slower."),
    CALM("Calm", Material.BLUE_DYE, "The hunger effect never sticks to you."),
    IRON_WILL("Iron Will", Material.IRON_NUGGET, "Take 50% less wither damage."),
    THICK_BLOOD("Thick Blood", Material.REDSTONE, "Take 50% less poison damage."),
    LIFESTEAL("Lifesteal", Material.GHAST_TEAR, "Heal 10% of melee damage you deal."),
    MEDIC("Medic", Material.CAKE, "Nearby hurt allies slowly regenerate."),
    WELLSPRING("Wellspring", Material.WATER_BUCKET, "Regeneration heals you 20% more."),
    TRANSFUSION("Transfusion", Material.POTION, "Healing potions restore 50% more."),
    STUBBORN("Stubborn", Material.MULE_SPAWN_EGG, "Weakness never sticks to you."),
    FASTING("Fasting", Material.ROTTEN_FLESH, "Take 15% less damage while hungry (under 3.5 food)."),
    EVERGREEN("Evergreen", Material.OAK_SAPLING, "Slowly regenerate while standing in sunlight."),
    REGENERATOR("Regenerator", Material.GOLDEN_APPLE, "Constantly regenerate health."),
    OVERHEAL("Overheal", Material.SPONGE, "Healing past full health becomes absorption (up to 2 hearts)."),
    RESOLUTE("Resolute", Material.WITHER_ROSE, "Wither never sticks to you."),
    DEATHS_DOOR("Death's Door", Material.WITHER_SKELETON_SKULL, "+20% damage while below 10% health."),
    SECOND_HEART("Second Heart", Material.GOLDEN_CARROT, "Keep 1 permanent absorption heart."),
    GRAVE_PACT("Grave Pact", Material.SOUL_SOIL, "Heal 4 hearts when a player dies within 12 blocks."),
    BLOOD_OATH("Blood Oath", Material.CRIMSON_FUNGUS, "Heal an extra 5% of melee damage you deal."),
    SECOND_WIND("Second Wind", Material.TOTEM_OF_UNDYING, "Below 20% HP: Regen II + Absorption (45s cd)."),
    PHOENIX_TEMPER("Phoenix Temper", Material.BLAZE_POWDER, "Falling below 20% health grants Strength I for 8s (20s cd)."),
    GREATER_VITALITY("Greater Vitality", Material.GLISTERING_MELON_SLICE, "+3 hearts of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 6.0),
    UNDYING("Undying", Material.END_CRYSTAL, "Once per 90s, a killing blow leaves you at 1 HP instead."),
    IMMORTAL_RESOLVE("Immortal Resolve", Material.NETHER_STAR, "+4 hearts of max health.",
            Attribute.MAX_HEALTH, AttributeModifier.Operation.ADD_NUMBER, 8.0),
    // ================= CHARISMA =================
    EXPOSE("Expose", Material.GLOWSTONE_DUST, "Your melee hits make targets glow for 5s."),
    INFECTIOUS_WOUND("Infectious Wound", Material.FERMENTED_SPIDER_EYE, "15% chance your hits fatigue the target for 3s."),
    IGNITE("Ignite", Material.FLINT_AND_STEEL, "Melee hits set the target on fire."),
    FROSTBITE("Frostbite", Material.PACKED_ICE, "Melee hits slow the target."),
    VENOMOUS("Venomous", Material.SPIDER_EYE, "20% chance your hits poison for 3s."),
    PLAGUEBEARER("Plaguebearer", Material.SLIME_BALL, "+15% damage to poisoned targets."),
    TORMENTOR("Tormentor", Material.SOUL_SAND, "+15% damage to slowed targets."),
    BANSHEE("Banshee", Material.GHAST_TEAR, "10% chance to blind attackers for 2s when they hit you."),
    CRIPPLE("Cripple", Material.BONE, "20% chance your hits give Slowness II for 2s."),
    BLINDING_STRIKES("Blinding Strikes", Material.INK_SAC, "10% chance your hits blind for 2s."),
    DEMORALIZE("Demoralize", Material.PHANTOM_MEMBRANE, "15% chance your hits inflict Weakness for 3s."),
    OPPORTUNIST("Opportunist", Material.GOLD_NUGGET, "+15% damage to blinded targets."),
    JINX("Jinx", Material.RABBIT_FOOT, "10% chance attackers get Slowness II for 3s when they hit you."),
    MANGLE("Mangle", Material.ROTTEN_FLESH, "Enemies you hit heal 50% less for 5s."),
    HEADHUNTER("Headhunter", Material.PLAYER_HEAD, "Player kills grant 2 absorption hearts for 30s."),
    WITHERING_TOUCH("Withering Touch", Material.WITHER_ROSE, "15% chance your hits wither for 2s."),
    CURSED_BLADE("Cursed Blade", Material.NETHERITE_SCRAP, "+10% damage to withered targets."),
    TERRORIZE("Terrorize", Material.SCULK_SHRIEKER, "Kills inflict Weakness for 5s on enemies within 6 blocks."),
    RINGLEADER("Ringleader", Material.BELL, "Trusted allies within 8 blocks gain Speed I."),
    RETRIBUTION("Retribution", Material.NETHERITE_SCRAP, "Every 5 hits taken, your next melee hit deals +1 heart of true damage."),
    HEXWEAVER("Hexweaver", Material.COBWEB, "Debuffs you inflict last 20% longer."),
    DREAD_AURA("Dread Aura", Material.SCULK, "Enemies within 5 blocks suffer Weakness."),
    PARIAH("Pariah", Material.CHORUS_FRUIT, "+8% damage to players suffering any debuff."),
    LEECHING_CURSE("Leeching Curse", Material.FERMENTED_SPIDER_EYE, "Heal half a heart whenever you inflict a debuff."),
    DOOM_MARK("Doom Mark", Material.WITHER_SKELETON_SKULL, "Every 7th hit on a player marks them: +30% damage taken for 5s.");

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
    /** Resource-pack item model that shows this talent's art as the icon. */
    public String modelKey() { return "talent_" + name().toLowerCase(java.util.Locale.ROOT); }
    public String description() { return description; }
    public boolean isAttribute() { return attribute != null; }
    public Attribute attribute() { return attribute; }
    public AttributeModifier.Operation operation() { return operation; }
    public double amount() { return amount; }

    public Stat stat() { return STAT.get(this); }
    /** Stat investment required before this talent can appear in a hand. */
    public int requirement() { return REQ.getOrDefault(this, 0); }

    private static final java.util.EnumMap<Card, Stat> STAT = new java.util.EnumMap<>(Card.class);
    private static final java.util.EnumMap<Card, Integer> REQ = new java.util.EnumMap<>(Card.class);
    private static final java.util.EnumMap<Card, Rarity> RARITY = new java.util.EnumMap<>(Card.class);
    static {
        STAT.put(FIRST_STRIKE, Stat.STRENGTH); REQ.put(FIRST_STRIKE, 0);
        STAT.put(BRUTE_FORCE, Stat.STRENGTH); REQ.put(BRUTE_FORCE, 0);
        STAT.put(HEAVY_HANDS, Stat.STRENGTH); REQ.put(HEAVY_HANDS, 0);
        STAT.put(FENCER, Stat.STRENGTH); REQ.put(FENCER, 10);
        STAT.put(SPEARHEAD, Stat.STRENGTH); REQ.put(SPEARHEAD, 10);
        STAT.put(HAYMAKER, Stat.STRENGTH); REQ.put(HAYMAKER, 10);
        STAT.put(BULLY, Stat.STRENGTH); REQ.put(BULLY, 10);
        STAT.put(SLUGGER, Stat.STRENGTH); REQ.put(SLUGGER, 10);
        STAT.put(FRENZY, Stat.STRENGTH); REQ.put(FRENZY, 25);
        STAT.put(COMBO, Stat.STRENGTH); REQ.put(COMBO, 25);
        STAT.put(SKEWER, Stat.STRENGTH); REQ.put(SKEWER, 25);
        STAT.put(BERSERKER, Stat.STRENGTH); REQ.put(BERSERKER, 25);
        STAT.put(CLEAVE, Stat.STRENGTH); REQ.put(CLEAVE, 25);
        STAT.put(BONEBREAKER, Stat.STRENGTH); REQ.put(BONEBREAKER, 25);
        STAT.put(ONSLAUGHT, Stat.STRENGTH); REQ.put(ONSLAUGHT, 40);
        STAT.put(SPINE_CUTTER, Stat.STRENGTH); REQ.put(SPINE_CUTTER, 40);
        STAT.put(RAMPAGE, Stat.STRENGTH); REQ.put(RAMPAGE, 40);
        STAT.put(UNYIELDING_INFERNO, Stat.STRENGTH); REQ.put(UNYIELDING_INFERNO, 40);
        STAT.put(SIEGEBREAKER, Stat.STRENGTH); REQ.put(SIEGEBREAKER, 40);
        STAT.put(TITANS_WRATH, Stat.STRENGTH); REQ.put(TITANS_WRATH, 40);
        STAT.put(GLASS_CANNON, Stat.STRENGTH); REQ.put(GLASS_CANNON, 60);
        STAT.put(EXECUTIONER, Stat.STRENGTH); REQ.put(EXECUTIONER, 60);
        STAT.put(CRUSHER, Stat.STRENGTH); REQ.put(CRUSHER, 60);
        STAT.put(CONCUSSIVE_BLOWS, Stat.STRENGTH); REQ.put(CONCUSSIVE_BLOWS, 80);
        STAT.put(WARLORD, Stat.STRENGTH); REQ.put(WARLORD, 80);
        STAT.put(BULWARK, Stat.FORTITUDE); REQ.put(BULWARK, 0);
        STAT.put(TOUGH_SKIN, Stat.FORTITUDE); REQ.put(TOUGH_SKIN, 0);
        STAT.put(PADDING, Stat.FORTITUDE); REQ.put(PADDING, 0);
        STAT.put(STEADFAST, Stat.FORTITUDE); REQ.put(STEADFAST, 10);
        STAT.put(IRONCLAD, Stat.FORTITUDE); REQ.put(IRONCLAD, 10);
        STAT.put(CHAINWEAVE, Stat.FORTITUDE); REQ.put(CHAINWEAVE, 10);
        STAT.put(DEFLECTION, Stat.FORTITUDE); REQ.put(DEFLECTION, 25);
        STAT.put(PACK_LEADER, Stat.FORTITUDE); REQ.put(PACK_LEADER, 25);
        STAT.put(FIREPROOF, Stat.FORTITUDE); REQ.put(FIREPROOF, 25);
        STAT.put(BLASTGUARD, Stat.FORTITUDE); REQ.put(BLASTGUARD, 25);
        STAT.put(STONEWALL, Stat.FORTITUDE); REQ.put(STONEWALL, 25);
        STAT.put(ANVIL_STANCE, Stat.FORTITUDE); REQ.put(ANVIL_STANCE, 25);
        STAT.put(REINFORCED, Stat.FORTITUDE); REQ.put(REINFORCED, 25);
        STAT.put(TITAN, Stat.FORTITUDE); REQ.put(TITAN, 40);
        STAT.put(SECOND_SKIN, Stat.FORTITUDE); REQ.put(SECOND_SKIN, 40);
        STAT.put(BULKHEAD, Stat.FORTITUDE); REQ.put(BULKHEAD, 40);
        STAT.put(IMMOVABLE, Stat.FORTITUDE); REQ.put(IMMOVABLE, 40);
        STAT.put(THORNPLATE, Stat.FORTITUDE); REQ.put(THORNPLATE, 40);
        STAT.put(RISKY_MOVES, Stat.FORTITUDE); REQ.put(RISKY_MOVES, 60);
        STAT.put(LAST_BASTION, Stat.FORTITUDE); REQ.put(LAST_BASTION, 60);
        STAT.put(HARDENED, Stat.FORTITUDE); REQ.put(HARDENED, 60);
        STAT.put(GUARDIAN, Stat.FORTITUDE); REQ.put(GUARDIAN, 60);
        STAT.put(JUGGERNAUT, Stat.FORTITUDE); REQ.put(JUGGERNAUT, 80);
        STAT.put(COLOSSUS, Stat.FORTITUDE); REQ.put(COLOSSUS, 80);
        STAT.put(AEGIS_SOUL, Stat.FORTITUDE); REQ.put(AEGIS_SOUL, 80);
        STAT.put(SWIFTNESS, Stat.AGILITY); REQ.put(SWIFTNESS, 0);
        STAT.put(SPRINTER, Stat.AGILITY); REQ.put(SPRINTER, 0);
        STAT.put(SPRING_STEP, Stat.AGILITY); REQ.put(SPRING_STEP, 0);
        STAT.put(FEATHER, Stat.AGILITY); REQ.put(FEATHER, 10);
        STAT.put(ACROBAT, Stat.AGILITY); REQ.put(ACROBAT, 10);
        STAT.put(SWIFT_STRIKES, Stat.AGILITY); REQ.put(SWIFT_STRIKES, 10);
        STAT.put(LONG_STRIDE, Stat.AGILITY); REQ.put(LONG_STRIDE, 10);
        STAT.put(BLUR, Stat.AGILITY); REQ.put(BLUR, 10);
        STAT.put(UNTRACKABLE, Stat.AGILITY); REQ.put(UNTRACKABLE, 10);
        STAT.put(ADRENALINE, Stat.AGILITY); REQ.put(ADRENALINE, 25);
        STAT.put(PARKOUR, Stat.AGILITY); REQ.put(PARKOUR, 25);
        STAT.put(HIT_AND_RUN, Stat.AGILITY); REQ.put(HIT_AND_RUN, 25);
        STAT.put(SIDESTEP, Stat.AGILITY); REQ.put(SIDESTEP, 25);
        STAT.put(BLINDSIDE, Stat.AGILITY); REQ.put(BLINDSIDE, 25);
        STAT.put(ESCAPE_ARTIST, Stat.AGILITY); REQ.put(ESCAPE_ARTIST, 40);
        STAT.put(WIND_SPRINT, Stat.AGILITY); REQ.put(WIND_SPRINT, 40);
        STAT.put(TAILWIND, Stat.AGILITY); REQ.put(TAILWIND, 40);
        STAT.put(EVASIVE_ROLL, Stat.AGILITY); REQ.put(EVASIVE_ROLL, 40);
        STAT.put(WINDRUNNER, Stat.AGILITY); REQ.put(WINDRUNNER, 40);
        STAT.put(VANTAGE, Stat.AGILITY); REQ.put(VANTAGE, 60);
        STAT.put(DODGE_ROLL, Stat.AGILITY); REQ.put(DODGE_ROLL, 60);
        STAT.put(AERIAL_ACE, Stat.AGILITY); REQ.put(AERIAL_ACE, 60);
        STAT.put(FEATHERWEIGHT, Stat.AGILITY); REQ.put(FEATHERWEIGHT, 60);
        STAT.put(GHOST, Stat.AGILITY); REQ.put(GHOST, 80);
        STAT.put(GALE_FORM, Stat.AGILITY); REQ.put(GALE_FORM, 80);
        STAT.put(SHARPSHOOTER, Stat.INTELLECT); REQ.put(SHARPSHOOTER, 0);
        STAT.put(STUDIOUS, Stat.INTELLECT); REQ.put(STUDIOUS, 0);
        STAT.put(KEEN_MIND, Stat.INTELLECT); REQ.put(KEEN_MIND, 0);
        STAT.put(SCAVENGER, Stat.INTELLECT); REQ.put(SCAVENGER, 10);
        STAT.put(ARCANIST, Stat.INTELLECT); REQ.put(ARCANIST, 10);
        STAT.put(CHANNELER, Stat.INTELLECT); REQ.put(CHANNELER, 10);
        STAT.put(MINDFUL, Stat.INTELLECT); REQ.put(MINDFUL, 10);
        STAT.put(CLARITY, Stat.INTELLECT); REQ.put(CLARITY, 25);
        STAT.put(SPELLWARD, Stat.INTELLECT); REQ.put(SPELLWARD, 25);
        STAT.put(ALCHEMIST, Stat.INTELLECT); REQ.put(ALCHEMIST, 25);
        STAT.put(CALCULATED, Stat.INTELLECT); REQ.put(CALCULATED, 25);
        STAT.put(STARGAZER, Stat.INTELLECT); REQ.put(STARGAZER, 25);
        STAT.put(FOCUS, Stat.INTELLECT); REQ.put(FOCUS, 25);
        STAT.put(ATTUNEMENT, Stat.INTELLECT); REQ.put(ATTUNEMENT, 40);
        STAT.put(SEER, Stat.INTELLECT); REQ.put(SEER, 40);
        STAT.put(WARDING_SIGILS, Stat.INTELLECT); REQ.put(WARDING_SIGILS, 40);
        STAT.put(SPELL_THIEF, Stat.INTELLECT); REQ.put(SPELL_THIEF, 40);
        STAT.put(TACTICIAN, Stat.INTELLECT); REQ.put(TACTICIAN, 40);
        STAT.put(BATTLE_RUSH, Stat.INTELLECT); REQ.put(BATTLE_RUSH, 60);
        STAT.put(OVERMIND, Stat.INTELLECT); REQ.put(OVERMIND, 60);
        STAT.put(GLASS_MIND, Stat.INTELLECT); REQ.put(GLASS_MIND, 60);
        STAT.put(BATTLE_MAGE, Stat.INTELLECT); REQ.put(BATTLE_MAGE, 60);
        STAT.put(DAMPEN, Stat.INTELLECT); REQ.put(DAMPEN, 60);
        STAT.put(ECHO, Stat.INTELLECT); REQ.put(ECHO, 80);
        STAT.put(PRODIGY, Stat.INTELLECT); REQ.put(PRODIGY, 80);
        STAT.put(VITALITY, Stat.WILLPOWER); REQ.put(VITALITY, 0);
        STAT.put(HEARTY, Stat.WILLPOWER); REQ.put(HEARTY, 0);
        STAT.put(SLOW_METABOLISM, Stat.WILLPOWER); REQ.put(SLOW_METABOLISM, 0);
        STAT.put(CALM, Stat.WILLPOWER); REQ.put(CALM, 0);
        STAT.put(IRON_WILL, Stat.WILLPOWER); REQ.put(IRON_WILL, 10);
        STAT.put(THICK_BLOOD, Stat.WILLPOWER); REQ.put(THICK_BLOOD, 10);
        STAT.put(LIFESTEAL, Stat.WILLPOWER); REQ.put(LIFESTEAL, 25);
        STAT.put(MEDIC, Stat.WILLPOWER); REQ.put(MEDIC, 25);
        STAT.put(WELLSPRING, Stat.WILLPOWER); REQ.put(WELLSPRING, 25);
        STAT.put(TRANSFUSION, Stat.WILLPOWER); REQ.put(TRANSFUSION, 25);
        STAT.put(STUBBORN, Stat.WILLPOWER); REQ.put(STUBBORN, 25);
        STAT.put(FASTING, Stat.WILLPOWER); REQ.put(FASTING, 25);
        STAT.put(EVERGREEN, Stat.WILLPOWER); REQ.put(EVERGREEN, 25);
        STAT.put(REGENERATOR, Stat.WILLPOWER); REQ.put(REGENERATOR, 40);
        STAT.put(OVERHEAL, Stat.WILLPOWER); REQ.put(OVERHEAL, 40);
        STAT.put(RESOLUTE, Stat.WILLPOWER); REQ.put(RESOLUTE, 40);
        STAT.put(DEATHS_DOOR, Stat.WILLPOWER); REQ.put(DEATHS_DOOR, 40);
        STAT.put(SECOND_HEART, Stat.WILLPOWER); REQ.put(SECOND_HEART, 40);
        STAT.put(GRAVE_PACT, Stat.WILLPOWER); REQ.put(GRAVE_PACT, 40);
        STAT.put(BLOOD_OATH, Stat.WILLPOWER); REQ.put(BLOOD_OATH, 40);
        STAT.put(SECOND_WIND, Stat.WILLPOWER); REQ.put(SECOND_WIND, 60);
        STAT.put(PHOENIX_TEMPER, Stat.WILLPOWER); REQ.put(PHOENIX_TEMPER, 60);
        STAT.put(GREATER_VITALITY, Stat.WILLPOWER); REQ.put(GREATER_VITALITY, 60);
        STAT.put(UNDYING, Stat.WILLPOWER); REQ.put(UNDYING, 80);
        STAT.put(IMMORTAL_RESOLVE, Stat.WILLPOWER); REQ.put(IMMORTAL_RESOLVE, 80);
        STAT.put(EXPOSE, Stat.CHARISMA); REQ.put(EXPOSE, 0);
        STAT.put(INFECTIOUS_WOUND, Stat.CHARISMA); REQ.put(INFECTIOUS_WOUND, 0);
        STAT.put(IGNITE, Stat.CHARISMA); REQ.put(IGNITE, 10);
        STAT.put(FROSTBITE, Stat.CHARISMA); REQ.put(FROSTBITE, 10);
        STAT.put(VENOMOUS, Stat.CHARISMA); REQ.put(VENOMOUS, 10);
        STAT.put(PLAGUEBEARER, Stat.CHARISMA); REQ.put(PLAGUEBEARER, 10);
        STAT.put(TORMENTOR, Stat.CHARISMA); REQ.put(TORMENTOR, 10);
        STAT.put(BANSHEE, Stat.CHARISMA); REQ.put(BANSHEE, 10);
        STAT.put(CRIPPLE, Stat.CHARISMA); REQ.put(CRIPPLE, 25);
        STAT.put(BLINDING_STRIKES, Stat.CHARISMA); REQ.put(BLINDING_STRIKES, 25);
        STAT.put(DEMORALIZE, Stat.CHARISMA); REQ.put(DEMORALIZE, 25);
        STAT.put(OPPORTUNIST, Stat.CHARISMA); REQ.put(OPPORTUNIST, 25);
        STAT.put(JINX, Stat.CHARISMA); REQ.put(JINX, 25);
        STAT.put(MANGLE, Stat.CHARISMA); REQ.put(MANGLE, 40);
        STAT.put(HEADHUNTER, Stat.CHARISMA); REQ.put(HEADHUNTER, 40);
        STAT.put(WITHERING_TOUCH, Stat.CHARISMA); REQ.put(WITHERING_TOUCH, 40);
        STAT.put(CURSED_BLADE, Stat.CHARISMA); REQ.put(CURSED_BLADE, 40);
        STAT.put(TERRORIZE, Stat.CHARISMA); REQ.put(TERRORIZE, 40);
        STAT.put(RINGLEADER, Stat.CHARISMA); REQ.put(RINGLEADER, 40);
        STAT.put(RETRIBUTION, Stat.CHARISMA); REQ.put(RETRIBUTION, 60);
        STAT.put(HEXWEAVER, Stat.CHARISMA); REQ.put(HEXWEAVER, 60);
        STAT.put(DREAD_AURA, Stat.CHARISMA); REQ.put(DREAD_AURA, 60);
        STAT.put(PARIAH, Stat.CHARISMA); REQ.put(PARIAH, 60);
        STAT.put(LEECHING_CURSE, Stat.CHARISMA); REQ.put(LEECHING_CURSE, 60);
        STAT.put(DOOM_MARK, Stat.CHARISMA); REQ.put(DOOM_MARK, 80);
        for (Card c : new Card[]{ONSLAUGHT, GLASS_CANNON, EXECUTIONER, CONCUSSIVE_BLOWS, WARLORD, RISKY_MOVES, JUGGERNAUT, COLOSSUS, AEGIS_SOUL, GHOST, GALE_FORM, ECHO, PRODIGY, SECOND_WIND, UNDYING, IMMORTAL_RESOLVE, DOOM_MARK}) RARITY.put(c, Rarity.LEGENDARY);
        for (Card c : new Card[]{COMBO, CLEAVE, SPINE_CUTTER, RAMPAGE, UNYIELDING_INFERNO, CRUSHER, TITAN, BULKHEAD, IMMOVABLE, THORNPLATE, LAST_BASTION, HARDENED, GUARDIAN, ESCAPE_ARTIST, VANTAGE, DODGE_ROLL, AERIAL_ACE, FEATHERWEIGHT, ATTUNEMENT, SEER, TACTICIAN, BATTLE_RUSH, OVERMIND, GLASS_MIND, BATTLE_MAGE, DAMPEN, LIFESTEAL, REGENERATOR, OVERHEAL, RESOLUTE, DEATHS_DOOR, PHOENIX_TEMPER, GREATER_VITALITY, MANGLE, HEADHUNTER, TERRORIZE, RETRIBUTION, HEXWEAVER, DREAD_AURA, PARIAH, LEECHING_CURSE}) RARITY.put(c, Rarity.EPIC);
        for (Card c : new Card[]{SPEARHEAD, HAYMAKER, SKEWER, BERSERKER, BONEBREAKER, SIEGEBREAKER, TITANS_WRATH, STEADFAST, DEFLECTION, PACK_LEADER, FIREPROOF, BLASTGUARD, STONEWALL, ANVIL_STANCE, REINFORCED, SECOND_SKIN, FEATHER, ADRENALINE, PARKOUR, HIT_AND_RUN, SIDESTEP, BLINDSIDE, WIND_SPRINT, TAILWIND, EVASIVE_ROLL, WINDRUNNER, SCAVENGER, CLARITY, SPELLWARD, ALCHEMIST, CALCULATED, STARGAZER, FOCUS, WARDING_SIGILS, SPELL_THIEF, VITALITY, MEDIC, WELLSPRING, TRANSFUSION, STUBBORN, FASTING, EVERGREEN, SECOND_HEART, GRAVE_PACT, BLOOD_OATH, CRIPPLE, BLINDING_STRIKES, DEMORALIZE, OPPORTUNIST, JINX, WITHERING_TOUCH, CURSED_BLADE, RINGLEADER}) RARITY.put(c, Rarity.RARE);
    }

    public Rarity rarity() { return RARITY.getOrDefault(this, Rarity.COMMON); }
}