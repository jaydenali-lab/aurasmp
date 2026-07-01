package com.aurasmp.ruin.card;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Applies attribute-based cards to players as persistent {@link AttributeModifier}s,
 * keyed so they can be cleanly removed/re-applied on login, draft and reset.
 */
public final class CardManager {

    private final RuinPlugin plugin;

    public CardManager(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    private NamespacedKey keyFor(Card card) {
        return new NamespacedKey(plugin, "card_" + card.name().toLowerCase(Locale.ROOT));
    }

    /** Re-derive all of our attribute modifiers from the player's owned cards. */
    public void recalc(Player player, PlayerData data) {
        // Every attribute any talent touches (derived from the registry so new
        // attribute talents work automatically).
        Set<Attribute> touched = new HashSet<>();
        for (Card card : Card.values()) {
            if (card.isAttribute()) touched.add(card.attribute());
        }

        // 1. Strip every modifier we previously added.
        for (Attribute attribute : touched) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getKey().getNamespace().equals(plugin.getName().toLowerCase(Locale.ROOT))) {
                    instance.removeModifier(modifier);
                }
            }
        }

        // 2. Re-apply from currently owned attribute cards.
        for (Card card : data.cards()) {
            if (!card.isAttribute()) continue;
            AttributeInstance instance = player.getAttribute(card.attribute());
            if (instance == null) continue;
            instance.addModifier(new AttributeModifier(keyFor(card), card.amount(), card.operation()));
        }

        // Titan's trade-off: +4 hearts but 20% slower. The health is its attribute card;
        // the slow is a separate movement-speed modifier applied here.
        if (data.hasCard(Card.TITAN)) {
            AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.addModifier(new AttributeModifier(
                        new NamespacedKey(plugin, "titan_slow"), -0.20,
                        AttributeModifier.Operation.ADD_SCALAR));
            }
        }

        // Clamp health if max health shrank (e.g. after a reset).
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }
}
