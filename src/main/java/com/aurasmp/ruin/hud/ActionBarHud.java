package com.aurasmp.ruin.hud;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.data.PlayerData;
import com.aurasmp.ruin.util.Glyphs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * Persistent action-bar HUD ("on top of the hotbar"). Shows the player's level,
 * XP, and each Manifestation with a custom icon + name + status:
 * <b>READY</b>, a cooldown countdown (<i>Ns</i>), or <b>ACTIVE</b> while its
 * self-buff is running. Re-sent twice a second so it never fades.
 */
public final class ActionBarHud {

    private final RuinPlugin plugin;
    private BukkitTask task;

    public ActionBarHud(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendActionBar(build(player, plugin.data().get(player.getUniqueId())));
        }
    }

    private Component build(Player player, PlayerData data) {
        List<Ability> abilities = data.abilities();
        if (abilities.isEmpty()) return Component.empty(); // no manifestations -> blank bar

        UUID id = player.getUniqueId();
        Component line = Component.empty();
        for (int i = 0; i < abilities.size(); i++) {
            if (i > 0) {
                // " §8|§r " separator between entries
                line = line.append(text(" ", NamedTextColor.DARK_GRAY))
                        .append(text("|", NamedTextColor.DARK_GRAY))
                        .append(text(" ", NamedTextColor.DARK_GRAY));
            }
            Ability ability = abilities.get(i);
            line = line.append(Glyphs.of(ability.glyph()))
                    .append(text(" " + ability.displayName() + " ", NamedTextColor.WHITE))
                    .append(status(id, ability));
        }
        return line;
    }

    private Component status(UUID id, Ability ability) {
        if (plugin.abilities().isActive(id, ability)) {
            return text("ACTIVE", NamedTextColor.YELLOW);
        }
        long cd = plugin.abilities().cooldowns().remainingMillis(id, ability.name());
        if (cd > 0) {
            return text(((cd + 999) / 1000) + "s", NamedTextColor.RED);
        }
        return text("READY", NamedTextColor.GREEN);
    }

    private Component text(String s, NamedTextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }
}
