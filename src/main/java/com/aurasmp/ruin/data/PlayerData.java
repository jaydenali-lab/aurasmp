package com.aurasmp.ruin.data;

import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Mutable per-player progression state. */
public final class PlayerData {

    public static final int MAX_LEVEL = 25;
    public static final int MAX_ABILITIES = 4;
    public static final int DEFAULT_REROLLS = 5;

    private int level = 1;
    private int xp = 0;
    private int rerolls = DEFAULT_REROLLS;
    private int skillPoints = 0;
    private final Set<Card> cards = EnumSet.noneOf(Card.class);
    private final List<Ability> abilities = new ArrayList<>(MAX_ABILITIES);
    // Players this player considers allies (one-way; affects only their own effects).
    private final Set<UUID> trusted = new HashSet<>();

    public int level() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int xp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public void addXp(int amount) { this.xp += amount; }

    public int rerolls() { return rerolls; }
    public void setRerolls(int rerolls) { this.rerolls = rerolls; }

    public int skillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = Math.max(0, skillPoints); }
    public void addSkillPoints(int amount) { this.skillPoints = Math.max(0, this.skillPoints + amount); }

    public Set<Card> cards() { return cards; }
    public boolean hasCard(Card card) { return cards.contains(card); }

    public List<Ability> abilities() { return abilities; }
    public boolean hasAbility(Ability ability) { return abilities.contains(ability); }

    public boolean isMaxLevel() { return level >= MAX_LEVEL; }

    public Set<UUID> trusted() { return trusted; }
    public boolean isTrusted(UUID id) { return trusted.contains(id); }

    /** Wipe everything back to a fresh level-1 slate. */
    public void reset() {
        level = 1;
        xp = 0;
        rerolls = DEFAULT_REROLLS;
        skillPoints = 0;
        cards.clear();
        abilities.clear();
    }
}
