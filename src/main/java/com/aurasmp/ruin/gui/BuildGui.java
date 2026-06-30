package com.aurasmp.ruin.gui;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Read-only menu (/ruin build) showing the player's level, talents and manifestations. */
public final class BuildGui {

    private final RuinPlugin plugin;

    public BuildGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Marker holder so the listener can recognise (and lock) this menu. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    public void open(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Your Build", NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(4, info(data));

        int slot = 9;
        for (Card card : data.cards()) {
            if (slot > 35) break;
            inv.setItem(slot++, icon(card.icon(), card.displayName(), card.rarity().color(),
                    card.rarity().label() + " · " + card.description()));
        }

        int aslot = 47;
        for (Ability ability : data.abilities()) {
            inv.setItem(aslot, icon(ability.icon(), ability.displayName(), NamedTextColor.LIGHT_PURPLE,
                    ability.description() + "  (" + (ability.cooldownMillis() / 1000) + "s)"));
            aslot += 2;
        }

        player.openInventory(inv);
    }

    private ItemStack info(PlayerData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Level " + data.level(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (data.isMaxLevel()) {
            lore.add(line("XP", "MAX"));
        } else {
            lore.add(line("XP", data.xp() + " / " + plugin.progression().threshold(data.level())));
        }
        lore.add(line("Rerolls", data.rerolls() + " / " + PlayerData.DEFAULT_REROLLS));
        lore.add(line("Talents", String.valueOf(data.cards().size())));
        lore.add(line("Manifestations", String.valueOf(data.abilities().size())));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false);
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private ItemStack icon(Material material, String title, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }
}
