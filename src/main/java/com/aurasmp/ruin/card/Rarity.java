package com.aurasmp.ruin.card;

import net.kyori.adventure.text.format.NamedTextColor;

/** Talent rarity tiers: name colour + relative draft weight (rarer = lower). */
public enum Rarity {

    COMMON("Common", NamedTextColor.GRAY, 100),
    RARE("Rare", NamedTextColor.BLUE, 45),
    EPIC("Epic", NamedTextColor.DARK_PURPLE, 18),
    LEGENDARY("Legendary", NamedTextColor.YELLOW, 6);

    private final String label;
    private final NamedTextColor color;
    private final int weight;

    Rarity(String label, NamedTextColor color, int weight) {
        this.label = label;
        this.color = color;
        this.weight = weight;
    }

    public String label() { return label; }
    public NamedTextColor color() { return color; }
    public int weight() { return weight; }
}
