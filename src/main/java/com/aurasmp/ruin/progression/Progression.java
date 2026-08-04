package com.aurasmp.ruin.progression;

import com.aurasmp.ruin.RuinConfig;
import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Set;

/** XP values, level thresholds and the level-up pipeline. */
public final class Progression {

    private static final Set<EntityType> WEAK = Set.of(
            EntityType.CHICKEN, EntityType.RABBIT, EntityType.BAT, EntityType.COD, EntityType.SALMON,
            EntityType.PUFFERFISH, EntityType.TROPICAL_FISH, EntityType.SILVERFISH, EntityType.ENDERMITE,
            EntityType.PARROT, EntityType.FROG, EntityType.TADPOLE, EntityType.BEE);

    private static final Set<EntityType> PASSIVE = Set.of(
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.MOOSHROOM, EntityType.GOAT,
            EntityType.HORSE, EntityType.DONKEY, EntityType.LLAMA, EntityType.FOX, EntityType.WOLF,
            EntityType.PANDA, EntityType.TURTLE, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.AXOLOTL,
            EntityType.DOLPHIN, EntityType.CAMEL, EntityType.SNIFFER, EntityType.ARMADILLO);

    private static final Set<EntityType> STRONG = Set.of(
            EntityType.ENDERMAN, EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR,
            EntityType.EVOKER, EntityType.RAVAGER, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN,
            EntityType.ZOGLIN, EntityType.BLAZE, EntityType.GHAST, EntityType.GUARDIAN,
            EntityType.SHULKER, EntityType.WITHER_SKELETON, EntityType.IRON_GOLEM, EntityType.BREEZE,
            EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.PHANTOM, EntityType.VEX,
            EntityType.STRAY, EntityType.HUSK, EntityType.BOGGED);

    private static final Set<EntityType> BOSS = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);

    private final RuinPlugin plugin;

    public Progression(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Base XP for killing a given entity type (players handled separately). */
    public int xpFor(EntityType type) {
        RuinConfig config = plugin.config();
        if (type == EntityType.PLAYER) return config.xpPlayer();
        if (BOSS.contains(type)) return config.xpBoss();
        if (STRONG.contains(type)) return config.xpStrong();
        if (WEAK.contains(type)) return config.xpWeak();
        if (PASSIVE.contains(type)) return config.xpPassive();
        return config.xpCommon(); // common hostiles (zombie, skeleton, creeper, …) + uncategorised
    }

    public int threshold(int level) {
        if (level < 1 || level >= PlayerData.MAX_LEVEL) return -1;
        return plugin.config().threshold(level);
    }

    /** Everything that happens on gaining one level: stat points + hands. */
    private void onLevelUp(Player player, PlayerData data) {
        int level = data.level();
        int gainedPoints = com.aurasmp.ruin.stat.Stat.earnedPoints(level)
                - com.aurasmp.ruin.stat.Stat.earnedPoints(level - 1);
        data.addStatPoints(gainedPoints);
        // Quiet, Deepwoken-style: a chat line, no title in your face.
        player.sendMessage(Component.text("You leveled up to level " + level
                + " and gained " + gainedPoints + " stat points.", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("  (/stats to invest)", NamedTextColor.DARK_GRAY)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);

        // Hands: a talent hand every level (first 20), plus a manifestation hand
        // every 2nd level through 20. Stash them or open them on the spot.
        if (level >= 2 && level <= 21) {
            player.getInventory().addItem(plugin.items().talentHand());
            player.sendMessage(Component.text("You received a Talent Hand.", NamedTextColor.AQUA)
                    .append(Component.text("  Right-click it to draw 3 talents.", NamedTextColor.DARK_GRAY)));
        }
        if (level % 2 == 0 && level <= 20) {
            player.getInventory().addItem(plugin.items().manifestHand());
            player.sendMessage(Component.text("You received a Manifestation Hand.", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("  Right-click it to draw 3 manifestations.", NamedTextColor.DARK_GRAY)));
        }
    }

    /** Award XP and resolve any level-ups. Returns the number of levels gained. */
    public int award(Player player, PlayerData data, int amount) {
        amount = Math.max(0, amount); // never drive XP negative (e.g. /ruin xp -999)
        if (data.isMaxLevel()) {
            return 0;
        }
        if (data.hasCard(Card.SCAVENGER)) {
            amount = (int) Math.round(amount * plugin.config().scavengerMultiplier());
        }
        if (data.hasCard(Card.STUDIOUS)) {
            amount = (int) Math.round(amount * 1.25);
        }
        data.addXp(amount);

        int gained = 0;
        while (!data.isMaxLevel() && threshold(data.level()) > 0 && data.xp() >= threshold(data.level())) {
            data.setXp(data.xp() - threshold(data.level()));
            data.setLevel(data.level() + 1);
            gained++;
            onLevelUp(player, data);
        }
        // Pop the XP boss bar (auto-hides after 5s) on every gain.
        plugin.xpBar().show(player, data);
        // Persist immediately — a server crash must not eat XP or levels.
        plugin.data().save(player.getUniqueId(), data);
        return gained;
    }

    /** Directly grant N levels (admin/testing), stat points + hands roll in per level. */
    public int awardLevels(Player player, PlayerData data, int levels) {
        int gained = 0;
        for (int i = 0; i < levels && !data.isMaxLevel(); i++) {
            data.setLevel(data.level() + 1);
            data.setXp(0);
            gained++;
            onLevelUp(player, data);
        }
        plugin.data().save(player.getUniqueId(), data);
        return gained;
    }
}
