package com.aurasmp.ruin.progression;

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

    /** XP required to advance FROM level (index+1). Five steps: 1→2 … 5→6. */
    private static final int[] THRESHOLDS = {100, 250, 500, 900, 1500};

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
        if (type == EntityType.PLAYER) return 50;
        if (BOSS.contains(type)) return 150;
        if (STRONG.contains(type)) return 20;
        if (WEAK.contains(type)) return 1;
        if (PASSIVE.contains(type)) return 2;
        return 8; // common hostiles (zombie, skeleton, creeper, spider, …)
    }

    public int threshold(int level) {
        if (level < 1 || level >= PlayerData.MAX_LEVEL) return -1;
        return THRESHOLDS[level - 1];
    }

    /** Award XP and resolve any level-ups. Returns the number of levels gained. */
    public int award(Player player, PlayerData data, int amount) {
        if (data.isMaxLevel()) {
            actionBarMaxed(player);
            return 0;
        }
        if (data.hasCard(Card.SCAVENGER)) {
            amount = (int) Math.round(amount * 1.5);
        }
        data.addXp(amount);

        int gained = 0;
        while (!data.isMaxLevel() && data.xp() >= threshold(data.level())) {
            data.setXp(data.xp() - threshold(data.level()));
            data.setLevel(data.level() + 1);
            gained++;
            // Level 3 and 6 grant abilities, every other level grants a card.
            boolean ability = data.level() % 3 == 0;
            plugin.gui().queue(player, ability);
        }

        if (gained > 0) {
            player.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text("Level " + data.level(), NamedTextColor.LIGHT_PURPLE),
                    Component.text("Ruin grows within you", NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            plugin.gui().openNextIfIdle(player);
        } else {
            actionBarProgress(player, data);
        }
        return gained;
    }

    /** Directly grant N levels (admin/testing), queuing one draft per level gained. */
    public int awardLevels(Player player, PlayerData data, int levels) {
        int gained = 0;
        for (int i = 0; i < levels && !data.isMaxLevel(); i++) {
            data.setLevel(data.level() + 1);
            data.setXp(0);
            gained++;
            boolean ability = data.level() % 3 == 0;
            plugin.gui().queue(player, ability);
        }
        if (gained > 0) {
            player.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text("Level " + data.level(), NamedTextColor.LIGHT_PURPLE),
                    Component.text("Ruin grows within you", NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            // Drafts open one after another (the GUI advances to the next on each pick).
            plugin.gui().openNextIfIdle(player);
        }
        return gained;
    }

    public void actionBarProgress(Player player, PlayerData data) {
        if (data.isMaxLevel()) {
            actionBarMaxed(player);
            return;
        }
        player.sendActionBar(Component.text(
                "✦ Ruin  Lv " + data.level() + "  " + data.xp() + "/" + threshold(data.level()),
                NamedTextColor.LIGHT_PURPLE));
    }

    private void actionBarMaxed(Player player) {
        player.sendActionBar(Component.text("✦ Ruin  Lv " + PlayerData.MAX_LEVEL + "  (MAX)", NamedTextColor.GOLD));
    }
}
