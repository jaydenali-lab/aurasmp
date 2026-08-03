package com.aurasmp.ruin.skilltree;

import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.aurasmp.ruin.ability.Ability.*;
import static com.aurasmp.ruin.card.Card.*;

/**
 * The stat-based skill tree that replaces the old RNG drafts.
 *
 * <p>Every talent and manifestation is a node in one of five branches
 * (Melee, Ranged, AoE, Support, Status), placed in a tier (1-4). Nodes are
 * bought with skill points earned from levelling; better nodes cost more,
 * and deeper tiers unlock only once you've invested enough points into
 * that branch.
 */
public final class SkillTree {

    /** Skill points granted per level gained. */
    public static final int SP_PER_LEVEL = 2;
    /** Bonus skill points for reaching max level. */
    public static final int SP_MAX_LEVEL_BONUS = 2;

    // Wynncraft-style progression: the tree grows outward — a tier opens
    // once you own at least one node of the previous tier in that branch.

    public enum Branch {
        MELEE("Melee", "Close-up damage, weapon mastery and duels"),
        RANGED("Ranged", "Projectiles, precision and mobility"),
        AOE("AoE", "Area damage and battlefield control"),
        SUPPORT("Support", "Defense, healing, allies and survival"),
        STATUS("Status", "Debuffs, curses and disables");

        private final String displayName;
        private final String description;

        Branch(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    /** A talent or manifestation placed in the tree, chained to the node it grows from. */
    public record Node(Card card, Ability ability, Branch branch, int tier, Node parent) {
        public boolean isAbility() { return ability != null; }

        public String displayName() {
            return isAbility() ? ability.displayName() : card.displayName();
        }

        public String description() {
            return isAbility() ? ability.description() : card.description();
        }

        /** Better nodes cost more: talents by rarity, manifestations by tier. */
        public int cost() {
            if (isAbility()) return 2 + tier; // T1=3 ... T4=6
            return switch (card.rarity()) {
                case LEGENDARY -> 5;
                case EPIC -> 3;
                case RARE -> 2;
                default -> 1;
            };
        }
    }

    private static final Map<Card, Node> CARDS = new EnumMap<>(Card.class);
    private static final Map<Ability, Node> ABILITIES = new EnumMap<>(Ability.class);

    private static Node parentNode(Object parent) {
        if (parent == null) return null;
        Node node = parent instanceof Card c ? CARDS.get(c) : ABILITIES.get((Ability) parent);
        if (node == null) throw new IllegalStateException("Parent registered after child: " + parent);
        return node;
    }

    private static void t(Card card, Branch branch, int tier) { t(card, branch, tier, null); }

    private static void t(Card card, Branch branch, int tier, Object parent) {
        CARDS.put(card, new Node(card, null, branch, tier, parentNode(parent)));
    }

    private static void a(Ability ability, Branch branch, int tier) { a(ability, branch, tier, null); }

    private static void a(Ability ability, Branch branch, int tier, Object parent) {
        ABILITIES.put(ability, new Node(null, ability, branch, tier, parentNode(parent)));
    }

    static {
        // Every node chains off a specific parent, so each talent sits on a
        // themed path and manifestations cap the path that builds toward them.

        // ================= MELEE =================
        t(FRENZY, Branch.MELEE, 1);
        t(FIRST_STRIKE, Branch.MELEE, 1);
        t(FENCER, Branch.MELEE, 1);
        t(SPEARHEAD, Branch.MELEE, 1);
        // Fury path: attack speed -> raw damage -> kill streaks -> Berserk.
        t(ONSLAUGHT, Branch.MELEE, 2, FRENZY);
        t(RAMPAGE, Branch.MELEE, 3, ONSLAUGHT);
        a(BERSERK, Branch.MELEE, 4, RAMPAGE);
        // Reckless offshoot of the fury path.
        t(BERSERKER, Branch.MELEE, 2, FRENZY);
        // Burning-hands path: grab them in flame, then punish burning foes.
        a(FLAME_GRAB, Branch.MELEE, 2, FRENZY);
        t(UNYIELDING_INFERNO, Branch.MELEE, 3, FLAME_GRAB);
        // Assassin path: open hard, backstab, go all-in.
        t(SPINE_CUTTER, Branch.MELEE, 2, FIRST_STRIKE);
        t(GLASS_CANNON, Branch.MELEE, 3, SPINE_CUTTER);
        // Ambush path: blink behind them, finish the wounded.
        a(PHASE_STRIKE, Branch.MELEE, 2, FIRST_STRIKE);
        t(EXECUTIONER, Branch.MELEE, 3, PHASE_STRIKE);
        t(HAYMAKER, Branch.MELEE, 2, FIRST_STRIKE);
        // Duelist path: swordplay -> combos -> the parry.
        t(COMBO, Branch.MELEE, 2, FENCER);
        a(RIPOSTE, Branch.MELEE, 3, COMBO);
        a(MOOK, Branch.MELEE, 2, FENCER);
        // Impale path: spear reach -> executions -> the stunning axe.
        t(SKEWER, Branch.MELEE, 2, SPEARHEAD);
        a(GUILLOTINE, Branch.MELEE, 3, SKEWER);
        t(CONCUSSIVE_BLOWS, Branch.MELEE, 4, GUILLOTINE);

        // ================= RANGED =================
        t(SHARPSHOOTER, Branch.RANGED, 1);
        t(SCAVENGER, Branch.RANGED, 1);
        t(SWIFTNESS, Branch.RANGED, 1);
        a(VOLLEY, Branch.RANGED, 1);
        a(FIREBALL, Branch.RANGED, 1);
        a(BLINK, Branch.RANGED, 1);
        // Marksman path: precision -> piercing line -> spotting -> the Railgun.
        a(SHADOW_LANCE, Branch.RANGED, 2, SHARPSHOOTER);
        a(TRUE_SIGHT, Branch.RANGED, 3, SHADOW_LANCE);
        a(RAILGUN, Branch.RANGED, 4, TRUE_SIGHT);
        // Called shots from the sky.
        a(SMITE, Branch.RANGED, 2, SHARPSHOOTER);
        // Arrow-duelist path: master arrows, shrug them off.
        t(DEFLECTION, Branch.RANGED, 2, VOLLEY);
        // Fire path: fireball -> traps -> the meteor.
        a(EMBER_MINE, Branch.RANGED, 2, FIREBALL);
        a(METEOR, Branch.RANGED, 3, EMBER_MINE);
        // Mobility paths.
        a(GALE_STEP, Branch.RANGED, 2, BLINK);
        t(ADRENALINE, Branch.RANGED, 2, SWIFTNESS);

        // ================= AOE =================
        t(IGNITE, Branch.AOE, 1);
        a(SHOCKWAVE, Branch.AOE, 1);
        // Wildfire path: sparks -> the blaze; Cleave rides the same flame.
        a(WILDFIRE, Branch.AOE, 2, IGNITE);
        t(CLEAVE, Branch.AOE, 2, IGNITE);
        // Earthbreaker path: shockwave -> fissure -> singularity -> Zelkova.
        a(FISSURE, Branch.AOE, 2, SHOCKWAVE);
        a(SINGULARITY, Branch.AOE, 3, FISSURE);
        a(ZELKOVA, Branch.AOE, 4, SINGULARITY);
        // Storm path: wind slam -> the blizzard.
        a(WIND_SLAM, Branch.AOE, 2, SHOCKWAVE);
        a(TUNDRA, Branch.AOE, 3, WIND_SLAM);

        // ================= SUPPORT =================
        t(VITALITY, Branch.SUPPORT, 1);
        t(BULWARK, Branch.SUPPORT, 1);
        t(STEADFAST, Branch.SUPPORT, 1);
        a(BLOODTHIRST, Branch.SUPPORT, 1);
        a(ICE_BARRIER, Branch.SUPPORT, 1);
        // Healer path: health -> mending -> overheal -> the clutch.
        t(MEDIC, Branch.SUPPORT, 2, VITALITY);
        t(OVERHEAL, Branch.SUPPORT, 3, MEDIC);
        t(SECOND_WIND, Branch.SUPPORT, 4, OVERHEAL);
        // Evasion path: light feet -> vanish -> the Ghost.
        t(FEATHER, Branch.SUPPORT, 2, VITALITY);
        a(VANISH, Branch.SUPPORT, 3, FEATHER);
        t(GHOST, Branch.SUPPORT, 4, VANISH);
        // Blood path: drink deep -> steal life -> pact / endless regen -> Undying.
        t(LIFESTEAL, Branch.SUPPORT, 2, BLOODTHIRST);
        a(BLOOD_PACT, Branch.SUPPORT, 3, LIFESTEAL);
        t(REGENERATOR, Branch.SUPPORT, 3, LIFESTEAL);
        t(UNDYING, Branch.SUPPORT, 4, REGENERATOR);
        // Tank path: armor -> allies -> titan -> the Juggernaut.
        t(PACK_LEADER, Branch.SUPPORT, 2, BULWARK);
        t(TITAN, Branch.SUPPORT, 3, PACK_LEADER);
        t(JUGGERNAUT, Branch.SUPPORT, 4, TITAN);
        // Clear-mind path: unshakable -> unclouded -> rewind time.
        t(CLARITY, Branch.SUPPORT, 2, STEADFAST);
        a(REWIND, Branch.SUPPORT, 3, CLARITY);
        // Barrier path: ice wall -> projectile ward -> full encasement -> untouchable.
        a(AEGIS, Branch.SUPPORT, 2, ICE_BARRIER);
        a(CRYOSTASIS, Branch.SUPPORT, 3, AEGIS);
        t(RISKY_MOVES, Branch.SUPPORT, 4, CRYOSTASIS);

        // ================= STATUS =================
        t(FROSTBITE, Branch.STATUS, 1);
        a(HEX, Branch.STATUS, 1);
        // Binding path: slow -> root -> and slipping every bind yourself.
        a(GRASPING_VINES, Branch.STATUS, 2, FROSTBITE);
        t(ESCAPE_ARTIST, Branch.STATUS, 3, GRASPING_VINES);
        a(COCOON, Branch.STATUS, 2, FROSTBITE);
        // Wither path: curse -> anti-heal -> siphon / trophy hunting.
        t(MANGLE, Branch.STATUS, 2, HEX);
        a(LIFEDRAIN, Branch.STATUS, 3, MANGLE);
        t(HEADHUNTER, Branch.STATUS, 3, MANGLE);
        // Breaker path: crack their guard -> seal their power / punish theirs.
        a(SUNDER, Branch.STATUS, 2, HEX);
        a(SILENCE, Branch.STATUS, 3, SUNDER);
        t(RETRIBUTION, Branch.STATUS, 3, SUNDER);
        // Arcane path: attune -> chain your manifestations off kills.
        t(ATTUNEMENT, Branch.STATUS, 2, HEX);
        t(BATTLE_RUSH, Branch.STATUS, 3, ATTUNEMENT);
    }

    private SkillTree() {}

    /** Fails loudly on enable if a talent/manifestation was never placed in the tree. */
    public static void validate() {
        for (Card card : Card.values()) {
            if (!CARDS.containsKey(card)) {
                throw new IllegalStateException("Card not in skill tree: " + card);
            }
        }
        for (Ability ability : Ability.values()) {
            if (!ABILITIES.containsKey(ability)) {
                throw new IllegalStateException("Ability not in skill tree: " + ability);
            }
        }
        // Path invariants: tier-1 nodes are roots; everything else chains one
        // tier down within its own branch.
        List<Node> all = new ArrayList<>(CARDS.values());
        all.addAll(ABILITIES.values());
        for (Node node : all) {
            if (node.tier() == 1) {
                if (node.parent() != null) {
                    throw new IllegalStateException("Tier-1 node has a parent: " + node.displayName());
                }
                continue;
            }
            if (node.parent() == null) {
                throw new IllegalStateException("Node has no path parent: " + node.displayName());
            }
            if (node.parent().branch() != node.branch() || node.parent().tier() != node.tier() - 1) {
                throw new IllegalStateException("Bad path parent for " + node.displayName()
                        + " (parent " + node.parent().displayName() + ")");
            }
        }
    }

    public static Node node(Card card) { return CARDS.get(card); }
    public static Node node(Ability ability) { return ABILITIES.get(ability); }

    /** All nodes of a branch, sorted tier-first (stable within a tier). */
    public static List<Node> branchNodes(Branch branch) {
        List<Node> out = new ArrayList<>();
        for (Node n : CARDS.values()) if (n.branch() == branch) out.add(n);
        for (Node n : ABILITIES.values()) if (n.branch() == branch) out.add(n);
        out.sort(java.util.Comparator.comparingInt(Node::tier)
                .thenComparing(n -> n.isAbility() ? 1 : 0)
                .thenComparing(Node::displayName));
        return out;
    }

    /** Total skill points a player has earned from their level. */
    public static int earnedPoints(PlayerData data) {
        int earned = SP_PER_LEVEL * (data.level() - 1);
        if (data.level() >= PlayerData.MAX_LEVEL) earned += SP_MAX_LEVEL_BONUS;
        return earned;
    }

    /** Points already spent on everything the player owns. */
    public static int spentPoints(PlayerData data) {
        int spent = 0;
        for (Card card : data.cards()) spent += node(card).cost();
        for (Ability ability : data.abilities()) spent += node(ability).cost();
        return spent;
    }

    /** Points spent inside one branch (gates the deeper tiers). */
    public static int spentInBranch(PlayerData data, Branch branch) {
        int spent = 0;
        for (Card card : data.cards()) {
            Node n = node(card);
            if (n.branch() == branch) spent += n.cost();
        }
        for (Ability ability : data.abilities()) {
            Node n = node(ability);
            if (n.branch() == branch) spent += n.cost();
        }
        return spent;
    }

    /** Wynncraft-style paths: tier-1 nodes are open; deeper nodes need the node they chain from. */
    public static boolean pathOpen(PlayerData data, Node node) {
        return node.parent() == null || owns(data, node.parent());
    }

    public static boolean owns(PlayerData data, Node node) {
        return node.isAbility() ? data.hasAbility(node.ability()) : data.hasCard(node.card());
    }

    /**
     * Reconciles a player's spendable points with what their level has earned
     * and what their build already owns. Heals old data (pre-skill-tree builds
     * keep everything; leftover points become spendable).
     */
    public static void reconcile(PlayerData data) {
        data.setSkillPoints(Math.max(0, earnedPoints(data) - spentPoints(data)));
    }
}
