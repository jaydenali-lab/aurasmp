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
    public static final int SP_PER_LEVEL = 3;
    /** Bonus skill points for reaching max level. */
    public static final int SP_MAX_LEVEL_BONUS = 3;

    /** Points that must be spent IN a branch before each tier opens. */
    public static final int[] TIER_THRESHOLDS = {0, 0, 4, 9, 15}; // index = tier

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

    /** A talent or manifestation placed in the tree. */
    public record Node(Card card, Ability ability, Branch branch, int tier) {
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

    private static void t(Card card, Branch branch, int tier) {
        CARDS.put(card, new Node(card, null, branch, tier));
    }

    private static void a(Ability ability, Branch branch, int tier) {
        ABILITIES.put(ability, new Node(null, ability, branch, tier));
    }

    static {
        // ================= MELEE =================
        t(FRENZY, Branch.MELEE, 1);
        t(FIRST_STRIKE, Branch.MELEE, 1);
        t(FENCER, Branch.MELEE, 1);
        t(SPEARHEAD, Branch.MELEE, 1);
        t(ONSLAUGHT, Branch.MELEE, 2);
        t(COMBO, Branch.MELEE, 2);
        t(SPINE_CUTTER, Branch.MELEE, 2);
        t(BERSERKER, Branch.MELEE, 2);
        t(HAYMAKER, Branch.MELEE, 2);
        t(SKEWER, Branch.MELEE, 2);
        t(GLASS_CANNON, Branch.MELEE, 3);
        t(EXECUTIONER, Branch.MELEE, 3);
        t(RAMPAGE, Branch.MELEE, 3);
        t(UNYIELDING_INFERNO, Branch.MELEE, 3);
        t(CONCUSSIVE_BLOWS, Branch.MELEE, 4);
        a(MOOK, Branch.MELEE, 2);
        a(PHASE_STRIKE, Branch.MELEE, 2);
        a(FLAME_GRAB, Branch.MELEE, 2);
        a(GUILLOTINE, Branch.MELEE, 3);
        a(RIPOSTE, Branch.MELEE, 3);
        a(BERSERK, Branch.MELEE, 4);

        // ================= RANGED =================
        t(SHARPSHOOTER, Branch.RANGED, 1);
        t(SCAVENGER, Branch.RANGED, 1);
        t(SWIFTNESS, Branch.RANGED, 1);
        t(DEFLECTION, Branch.RANGED, 2);
        t(ADRENALINE, Branch.RANGED, 2);
        a(VOLLEY, Branch.RANGED, 1);
        a(FIREBALL, Branch.RANGED, 1);
        a(BLINK, Branch.RANGED, 1);
        a(SMITE, Branch.RANGED, 2);
        a(SHADOW_LANCE, Branch.RANGED, 2);
        a(EMBER_MINE, Branch.RANGED, 2);
        a(GALE_STEP, Branch.RANGED, 2);
        a(METEOR, Branch.RANGED, 3);
        a(TRUE_SIGHT, Branch.RANGED, 3);
        a(RAILGUN, Branch.RANGED, 4);

        // ================= AOE =================
        t(IGNITE, Branch.AOE, 1);
        t(CLEAVE, Branch.AOE, 2);
        a(SHOCKWAVE, Branch.AOE, 1);
        a(FISSURE, Branch.AOE, 2);
        a(WILDFIRE, Branch.AOE, 2);
        a(WIND_SLAM, Branch.AOE, 2);
        a(TUNDRA, Branch.AOE, 3);
        a(SINGULARITY, Branch.AOE, 3);
        a(ZELKOVA, Branch.AOE, 4);

        // ================= SUPPORT =================
        t(VITALITY, Branch.SUPPORT, 1);
        t(BULWARK, Branch.SUPPORT, 1);
        t(STEADFAST, Branch.SUPPORT, 1);
        t(PACK_LEADER, Branch.SUPPORT, 2);
        t(FEATHER, Branch.SUPPORT, 2);
        t(LIFESTEAL, Branch.SUPPORT, 2);
        t(MEDIC, Branch.SUPPORT, 2);
        t(CLARITY, Branch.SUPPORT, 2);
        t(TITAN, Branch.SUPPORT, 3);
        t(REGENERATOR, Branch.SUPPORT, 3);
        t(OVERHEAL, Branch.SUPPORT, 3);
        t(JUGGERNAUT, Branch.SUPPORT, 4);
        t(SECOND_WIND, Branch.SUPPORT, 4);
        t(UNDYING, Branch.SUPPORT, 4);
        t(RISKY_MOVES, Branch.SUPPORT, 4);
        t(GHOST, Branch.SUPPORT, 4);
        a(BLOODTHIRST, Branch.SUPPORT, 1);
        a(ICE_BARRIER, Branch.SUPPORT, 1);
        a(VANISH, Branch.SUPPORT, 2);
        a(AEGIS, Branch.SUPPORT, 3);
        a(CRYOSTASIS, Branch.SUPPORT, 3);
        a(REWIND, Branch.SUPPORT, 3);
        a(BLOOD_PACT, Branch.SUPPORT, 3);

        // ================= STATUS =================
        t(FROSTBITE, Branch.STATUS, 1);
        t(MANGLE, Branch.STATUS, 2);
        t(RETRIBUTION, Branch.STATUS, 3);
        t(ESCAPE_ARTIST, Branch.STATUS, 3);
        t(BATTLE_RUSH, Branch.STATUS, 3);
        t(ATTUNEMENT, Branch.STATUS, 3);
        t(HEADHUNTER, Branch.STATUS, 3);
        a(HEX, Branch.STATUS, 1);
        a(SUNDER, Branch.STATUS, 2);
        a(COCOON, Branch.STATUS, 2);
        a(GRASPING_VINES, Branch.STATUS, 2);
        a(SILENCE, Branch.STATUS, 3);
        a(LIFEDRAIN, Branch.STATUS, 3);
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

    public static boolean tierUnlocked(PlayerData data, Branch branch, int tier) {
        return spentInBranch(data, branch) >= TIER_THRESHOLDS[Math.min(tier, TIER_THRESHOLDS.length - 1)];
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
