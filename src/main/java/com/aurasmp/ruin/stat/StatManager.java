package com.aurasmp.ruin.stat;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Turns invested stat points into gameplay. Attribute-backed effects (Agility
 * speed) are applied as modifiers; the rest are multipliers the combat and
 * ability code query per hit.
 */
public final class StatManager {

    private final RuinPlugin plugin;
    private final NamespacedKey agilityKey;

    public StatManager(RuinPlugin plugin) {
        this.plugin = plugin;
        this.agilityKey = new NamespacedKey(plugin, "stat_agility_speed");
    }

    private PlayerData data(Player player) {
        return plugin.data().get(player.getUniqueId());
    }

    /** Re-applies attribute-backed stat effects (called alongside CardManager.recalc). */
    public void recalc(Player player, PlayerData data) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            for (AttributeModifier mod : speed.getModifiers()) {
                if (mod.getKey().equals(agilityKey)) speed.removeModifier(mod);
            }
            int agility = data.stat(Stat.AGILITY);
            if (agility > 0) {
                // +0.25% movement speed per point.
                speed.addModifier(new AttributeModifier(agilityKey,
                        agility * 0.0025, AttributeModifier.Operation.ADD_SCALAR));
            }
        }
    }

    /**
     * Strength: melee damage bonus approximating armor penetration — 0.35% pen
     * per point, scaled by how armored the victim actually is (armor/30).
     */
    public double strengthBonus(Player attacker, LivingEntity victim) {
        int str = data(attacker).stat(Stat.STRENGTH);
        if (str <= 0) return 0;
        AttributeInstance armor = victim.getAttribute(Attribute.ARMOR);
        double armorShare = armor == null ? 0 : Math.min(30, armor.getValue()) / 30.0;
        return str * 0.0035 * armorShare;
    }

    /** Fortitude: flat damage resistance, 0.5% per point (50% at the 100 cap). */
    public double fortitudeResist(Player victim) {
        return data(victim).stat(Stat.FORTITUDE) * 0.005;
    }

    /** Intellect: manifestation damage multiplier (+0.3% per point). */
    public double intellectDamageMult(Player caster) {
        return 1.0 + data(caster).stat(Stat.INTELLECT) * 0.003;
    }

    /** Intellect: manifestation cooldown multiplier (-0.2% per point). */
    public double intellectCooldownMult(Player caster) {
        return 1.0 - data(caster).stat(Stat.INTELLECT) * 0.002;
    }

    /**
     * Debuff duration in ticks after Charisma (caster, +0.5%/pt) and Willpower
     * (victim, -0.4%/pt) have argued about it. Caster or victim may be null.
     */
    public int debuffTicks(Player caster, LivingEntity victim, int baseTicks) {
        double mult = 1.0;
        if (caster != null) mult += data(caster).stat(Stat.CHARISMA) * 0.005;
        if (victim instanceof Player p) {
            mult -= data(p).stat(Stat.WILLPOWER) * 0.004;
            // Warding Sigils talent: a flat 15% on top of Willpower.
            if (data(p).hasCard(com.aurasmp.ruin.card.Card.WARDING_SIGILS)) mult -= 0.15;
        }
        return Math.max(1, (int) Math.round(baseTicks * mult));
    }
}
