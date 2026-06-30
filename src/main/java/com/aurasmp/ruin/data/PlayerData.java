package com.aurasmp.ruin.data;

import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Mutable per-player progression state. */
public final class PlayerData {

    public static final int MAX_LEVEL = 10;
    public static final int MAX_ABILITIES = 2;

    private int level = 1;
    private int xp = 0;
    private final Set<Card> cards = EnumSet.noneOf(Card.class);
    private final List<Ability> abilities = new ArrayList<>(MAX_ABILITIES);

    public int level() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int xp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public void addXp(int amount) { this.xp += amount; }

    public Set<Card> cards() { return cards; }
    public boolean hasCard(Card card) { return cards.contains(card); }

    public List<Ability> abilities() { return abilities; }
    public boolean hasAbility(Ability ability) { return abilities.contains(ability); }

    public boolean isMaxLevel() { return level >= MAX_LEVEL; }

    /** Wipe everything back to a fresh level-1 slate. */
    public void reset() {
        level = 1;
        xp = 0;
        cards.clear();
        abilities.clear();
    }
}
