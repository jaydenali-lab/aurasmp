package com.aurasmp.ruin.listener;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Login/logout state handling and custom-item interactions. */
public final class PlayerListener implements Listener {

    private final RuinPlugin plugin;

    public PlayerListener(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.data().get(player.getUniqueId());
        plugin.cards().recalc(player, data);
        // Hand back a Catalyst if they have abilities but lost the item.
        if (!data.abilities().isEmpty() && !hasCatalyst(player)) {
            player.getInventory().addItem(plugin.items().catalyst(data.abilities()));
        }
        // Resume any draft they had queued when they left.
        plugin.gui().openNextIfIdle(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.gui().clear(event.getPlayer().getUniqueId());
        plugin.abilities().cooldowns().clear(event.getPlayer().getUniqueId());
        plugin.abilities().clearActive(event.getPlayer().getUniqueId());
        plugin.xpBar().cleanup(event.getPlayer().getUniqueId());
        plugin.data().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        Player player = event.getPlayer();

        if (plugin.items().isMirrorShard(item)) {
            event.setCancelled(true);
            useMirrorShard(player, item);
            return;
        }

        if (plugin.items().isCatalyst(item)) {
            event.setCancelled(true);
            List<Ability> abilities = plugin.data().get(player.getUniqueId()).abilities();
            int index = player.isSneaking() ? 1 : 0;
            if (abilities.size() > index) {
                plugin.abilities().tryActivate(player, abilities.get(index));
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        // The Catalyst can't be dropped.
        if (plugin.items().isCatalyst(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Don't drop the Catalyst on death — it's restored on respawn instead.
        event.getDrops().removeIf(item -> plugin.items().isCatalyst(item));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            PlayerData data = plugin.data().get(player.getUniqueId());
            if (!data.abilities().isEmpty() && !hasCatalyst(player)) {
                player.getInventory().addItem(plugin.items().catalyst(data.abilities()));
            }
        });
    }

    private void useMirrorShard(Player player, ItemStack item) {
        plugin.resetPlayer(player);
        item.setAmount(item.getAmount() - 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 0.7f);
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("Reset", NamedTextColor.AQUA),
                Component.text("Build wiped", NamedTextColor.GRAY)));
    }

    private boolean hasCatalyst(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.items().isCatalyst(item)) return true;
        }
        return false;
    }
}
