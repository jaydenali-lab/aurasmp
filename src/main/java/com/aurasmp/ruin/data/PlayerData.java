package com.aurasmp.ruin.data;

import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.stat.Stat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mutable per-player progression state. */
public final class PlayerData {

    public static final int MAX_LEVEL = 25;
    /** How many manifestations can be equipped (carried as cast items) at once. */
    public static final int MAX_EQUIPPED = 4;
    public static final int DEFAULT_REROLLS = 5;
    /** Hard caps on hand use per run — even smuggled hands can't push past these. */
    public static final int MAX_TALENT_HANDS = 20;
    public static final int MAX_MANIFEST_HANDS = 10;

    private int level = 1;
    private int xp = 0;
    private int rerolls = DEFAULT_REROLLS;
    /** Unspent stat points (earned 15/level from the 330 pool). */
    private int statPoints = 0;
    private final Map<Stat, Integer> stats = new EnumMap<>(Stat.class);
    /** Talent hands opened this run (caps at MAX_TALENT_HANDS). */
    private int talentHandsUsed = 0;
    /** Manifestation hands opened this run (caps at MAX_MANIFEST_HANDS). */
    private int manifestHandsUsed = 0;

    private final Set<Card> cards = EnumSet.noneOf(Card.class);
    /** Every manifestation learned this run. */
    private final Set<Ability> unlocked = EnumSet.noneOf(Ability.class);
    /** The (up to MAX_EQUIPPED) manifestations currently carried as cast items. */
    private final List<Ability> abilities = new ArrayList<>(MAX_EQUIPPED);
    // Players this player considers allies (one-way; affects only their own effects).
    private final Set<UUID> trusted = new HashSet<>();

    public int level() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int xp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public void addXp(int amount) { this.xp += amount; }

    public int rerolls() { return rerolls; }
    public void setRerolls(int rerolls) { this.rerolls = rerolls; }

    public int statPoints() { return statPoints; }
    public void setStatPoints(int points) { this.statPoints = Math.max(0, points); }
    public void addStatPoints(int amount) { this.statPoints = Math.max(0, this.statPoints + amount); }

    public int stat(Stat stat) { return stats.getOrDefault(stat, 0); }
    public void setStat(Stat stat, int value) {
        stats.put(stat, Math.max(0, Math.min(Stat.CAP, value)));
    }
    public int allocatedPoints() {
        int total = 0;
        for (Stat stat : Stat.values()) total += stat(stat);
        return total;
    }
    public boolean meetsRequirement(Stat stat, int req) { return stat(stat) >= req; }

    public int talentHandsUsed() { return talentHandsUsed; }
    public void setTalentHandsUsed(int used) { this.talentHandsUsed = Math.max(0, used); }
    public int manifestHandsUsed() { return manifestHandsUsed; }
    public void setManifestHandsUsed(int used) { this.manifestHandsUsed = Math.max(0, used); }

    public Set<Card> cards() { return cards; }
    public boolean hasCard(Card card) { return cards.contains(card); }

    /** Equipped manifestations (cast items follow this list). */
    public List<Ability> abilities() { return abilities; }
    public boolean hasAbility(Ability ability) { return abilities.contains(ability); }

    /** Manifestations learned this run (equipped or stashed). */
    public Set<Ability> unlocked() { return unlocked; }
    public boolean hasUnlocked(Ability ability) { return unlocked.contains(ability); }

    public boolean isMaxLevel() { return level >= MAX_LEVEL; }

    public Set<UUID> trusted() { return trusted; }
    public boolean isTrusted(UUID id) { return trusted.contains(id); }

    /** Wipe everything back to a fresh level-1 slate. */
    public void reset() {
        level = 1;
        xp = 0;
        rerolls = DEFAULT_REROLLS;
        statPoints = 0;
        stats.clear();
        talentHandsUsed = 0;
        manifestHandsUsed = 0;
        cards.clear();
        unlocked.clear();
        abilities.clear();
    }
}
