package com.aurasmp.ruin.gui;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import com.aurasmp.ruin.stat.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The /stats menu: invest earned stat points into the six attributes. */
public final class StatsGui {

    private static final int INFO_SLOT = 4;
    private static final int[] STAT_SLOTS = {10, 11, 12, 14, 15, 16};

    private final RuinPlugin plugin;

    public StatsGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        final Map<Integer, Stat> statSlots = new HashMap<>();

        @Override public Inventory getInventory() { return inventory; }
    }

    public void open(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("✦ Stats — " + data.statPoints() + " points",
                        NamedTextColor.DARK_AQUA));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(INFO_SLOT, infoItem(data));
        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            holder.statSlots.put(STAT_SLOTS[i], stats[i]);
            inv.setItem(STAT_SLOTS[i], statItem(data, stats[i]));
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.3f);
    }

    /** Left-click: +1. Shift-click: +10. */
    public void handleClick(Player player, Holder holder, int slot, boolean shift) {
        Stat stat = holder.statSlots.get(slot);
        if (stat == null) return;
        PlayerData data = plugin.data().get(player.getUniqueId());

        int amount = Math.min(shift ? 10 : 1,
                Math.min(data.statPoints(), Stat.CAP - data.stat(stat)));
        if (amount <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            if (data.statPoints() <= 0) {
                player.sendMessage(Component.text("No stat points left — level up to earn more.",
                        NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text(stat.displayName() + " is capped at "
                        + Stat.CAP + ".", NamedTextColor.RED));
            }
            return;
        }
        data.setStat(stat, data.stat(stat) + amount);
        data.addStatPoints(-amount);
        plugin.data().save(player.getUniqueId(), data);
        plugin.stats().recalc(player, data);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        open(player); // re-render with new totals
    }

    private ItemStack infoItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(data.statPoints() + " unspent stat points",
                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line("Level " + data.level() + "  ·  " + Stat.POINTS_PER_LEVEL
                + " points per level-up"));
        lore.add(line("Everyone earns " + Stat.TOTAL_POINTS + " points total."));
        lore.add(line("Each stat caps at " + Stat.CAP + "."));
        lore.add(line(""));
        lore.add(line("Talents and manifestations need stats"));
        lore.add(line("to appear in your hands."));
        lore.add(line("A Mirror Shard refunds everything."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack statItem(PlayerData data, Stat stat) {
        ItemStack item = new ItemStack(stat.icon());
        int value = data.stat(stat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(stat.displayName() + "  " + value + " / " + Stat.CAP,
                stat.color()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line(stat.scaling()));
        lore.add(line(""));
        lore.add(Component.text("Click: +1   Shift-click: +10", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }
}
