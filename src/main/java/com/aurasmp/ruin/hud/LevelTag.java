package com.aurasmp.ruin.hud;

import com.aurasmp.ruin.RuinPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Floating "Lv N" tag above every player's nametag, carried as a TextDisplay
 * passenger. It disappears while the player is invisible (Vanish, Ghost,
 * potions) and comes back when they reappear.
 */
public final class LevelTag {

    private final RuinPlugin plugin;
    private final Map<UUID, TextDisplay> tags = new HashMap<>();
    private BukkitTask task;

    public LevelTag(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        tags.values().forEach(TextDisplay::remove);
        tags.clear();
    }

    public void remove(UUID id) {
        TextDisplay tag = tags.remove(id);
        if (tag != null) tag.remove();
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean hidden = player.isDead()
                    || player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                    || player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
            TextDisplay tag = tags.get(player.getUniqueId());
            if (hidden) {
                if (tag != null) remove(player.getUniqueId());
                continue;
            }
            if (tag == null || !tag.isValid() || !player.getPassengers().contains(tag)) {
                if (tag != null) tag.remove();
                tag = spawn(player);
                tags.put(player.getUniqueId(), tag);
            }
            tag.text(text(player));
        }
        // sweep tags of players who left
        tags.entrySet().removeIf(e -> {
            if (plugin.getServer().getPlayer(e.getKey()) == null) {
                e.getValue().remove();
                return true;
            }
            return false;
        });
    }

    private Component text(Player player) {
        int level = plugin.data().get(player.getUniqueId()).level();
        return Component.text("Lv " + level, NamedTextColor.GOLD);
    }

    private TextDisplay spawn(Player player) {
        TextDisplay tag = player.getWorld().spawn(player.getLocation(), TextDisplay.class, d -> {
            d.text(text(player));
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setPersistent(false);
            d.setViewRange(0.5f); // ~32 blocks, like nametags
            // lift it just above the vanilla nametag
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.85f, 0f), new org.joml.Quaternionf(),
                    new Vector3f(1f, 1f, 1f), new org.joml.Quaternionf()));
        });
        player.addPassenger(tag);
        return tag;
    }
}
