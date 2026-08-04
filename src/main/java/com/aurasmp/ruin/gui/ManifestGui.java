package com.aurasmp.ruin.gui;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.data.PlayerData;
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

/**
 * The /manifest menu: equip and unequip learned manifestations, Deepwoken
 * mantra-style. Up to {@link PlayerData#MAX_EQUIPPED} carried at once; the
 * rest stay stashed and can be swapped in anytime.
 */
public final class ManifestGui {

    private static final int INFO_SLOT = 4;

    private final RuinPlugin plugin;

    public ManifestGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        final Map<Integer, Ability> slots = new HashMap<>();

        @Override public Inventory getInventory() { return inventory; }
    }

    public void open(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("✦ Manifestations — " + data.abilities().size()
                        + " / " + PlayerData.MAX_EQUIPPED + " equipped", NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fill(inv);
        inv.setItem(INFO_SLOT, infoItem(data));

        int slot = 9;
        List<Ability> learned = new ArrayList<>(data.unlocked());
        learned.sort(java.util.Comparator
                .comparing((Ability a) -> a.stat().ordinal())
                .thenComparingInt(Ability::requirement)
                .thenComparing(Ability::displayName));
        for (Ability ability : learned) {
            if (slot >= 54) break;
            holder.slots.put(slot, ability);
            inv.setItem(slot, abilityItem(data, ability));
            slot++;
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.1f);
    }

    public void handleClick(Player player, Holder holder, int slot) {
        Ability ability = holder.slots.get(slot);
        if (ability == null) return;
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (!data.hasUnlocked(ability)) return;

        if (data.hasAbility(ability)) {
            data.abilities().remove(ability);
            player.sendMessage(Component.text("Unequipped ", NamedTextColor.GRAY)
                    .append(Component.text(ability.displayName(), NamedTextColor.LIGHT_PURPLE)));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
        } else {
            if (data.abilities().size() >= PlayerData.MAX_EQUIPPED) {
                player.sendMessage(Component.text("You already carry " + PlayerData.MAX_EQUIPPED
                        + " manifestations — unequip one first.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
                return;
            }
            data.abilities().add(ability);
            player.sendMessage(Component.text("Equipped ", NamedTextColor.GRAY)
                    .append(Component.text(ability.displayName(), NamedTextColor.LIGHT_PURPLE)));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.2f);
        }
        plugin.items().refreshCastItems(player, data);
        plugin.data().save(player.getUniqueId(), data);
        open(player); // re-render
    }

    private ItemStack infoItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Learned: " + data.unlocked().size()
                        + "  ·  Equipped: " + data.abilities().size() + " / " + PlayerData.MAX_EQUIPPED,
                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("Click a manifestation to equip or unequip it."),
                line("Equipped ones ride in your inventory as cast items."),
                line("Learn more from Manifestation Hands.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack abilityItem(PlayerData data, Ability ability) {
        boolean equipped = data.hasAbility(ability);
        ItemStack item = new ItemStack(ability.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(org.bukkit.NamespacedKey.fromString("ruin:" + ability.modelKey()));
        meta.displayName(Component.text(ability.displayName(),
                        equipped ? NamedTextColor.GREEN : NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ability.stat().displayName() + " " + ability.requirement(),
                ability.stat().color()).decoration(TextDecoration.ITALIC, false));
        for (String text : wrap(ability.description(), 34)) lore.add(line(text));
        lore.add(Component.text("Cooldown: " + (ability.cooldownMillis() / 1000) + "s",
                NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(line(""));
        lore.add(Component.text(equipped ? "✔ Equipped — click to unequip" : "Click to equip",
                        equipped ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        if (equipped) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
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
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > width && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }
}
