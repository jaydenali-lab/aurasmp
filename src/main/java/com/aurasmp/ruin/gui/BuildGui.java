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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The /ruin build menu: a player's level, talents and manifestations.
 *
 * <p>Read-only for everyone — except the player named {@link #ADMIN_NAME}, for whom it
 * becomes an editor: clicking a talent/manifestation (or the add buttons) opens a
 * {@link PickerGui} to set it. For anyone else the clicks silently do nothing.
 */
public final class BuildGui {

    /** Only this player can edit builds. */
    public static final String ADMIN_NAME = "cheesedisguise";

    private static final int ADD_TALENT_SLOT = 8;
    private static final int ADD_ABILITY_SLOT = 53;

    private final RuinPlugin plugin;

    public BuildGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isAdmin(Player player) {
        return player.getName().equalsIgnoreCase(ADMIN_NAME);
    }

    /** Holder so the listener can recognise the menu and read its edit state. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        UUID target;
        boolean editable;
        final Map<Integer, Card> talentSlots = new HashMap<>();
        final Map<Integer, Ability> abilitySlots = new HashMap<>();
        int addTalentSlot = -1;
        int addAbilitySlot = -1;

        @Override public Inventory getInventory() { return inventory; }
        public UUID target() { return target; }
        public boolean editable() { return editable; }
        public Card talentAt(int slot) { return talentSlots.get(slot); }
        public Ability abilityAt(int slot) { return abilitySlots.get(slot); }
        public boolean isAddTalent(int slot) { return slot == addTalentSlot; }
        public boolean isAddAbility(int slot) { return slot == addAbilitySlot; }
    }

    public void open(Player viewer) {
        open(viewer, viewer.getUniqueId());
    }

    public void open(Player viewer, UUID targetId) {
        PlayerData data = plugin.data().get(targetId);
        Holder holder = new Holder();
        holder.target = targetId;
        holder.editable = isAdmin(viewer);

        String title = targetId.equals(viewer.getUniqueId()) ? "Your Build" : "Build: " + nameOf(targetId);
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(title, NamedTextColor.DARK_PURPLE));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(4, info(data));

        int slot = 9;
        for (Card card : data.cards()) {
            if (slot > 35) break;
            holder.talentSlots.put(slot, card);
            inv.setItem(slot, icon(card.icon(), card.displayName(), card.rarity().color(),
                    card.rarity().label() + " · " + card.description()));
            slot++;
        }

        int aslot = 47;
        for (Ability ability : data.abilities()) {
            holder.abilitySlots.put(aslot, ability);
            inv.setItem(aslot, icon(ability.icon(), ability.displayName(), NamedTextColor.LIGHT_PURPLE,
                    ability.description() + "  (" + (ability.cooldownMillis() / 1000) + "s)"));
            aslot += 2;
        }

        if (holder.editable) {
            holder.addTalentSlot = ADD_TALENT_SLOT;
            holder.addAbilitySlot = ADD_ABILITY_SLOT;
            inv.setItem(ADD_TALENT_SLOT, addButton("Add Talent", "Click to add a talent"));
            inv.setItem(ADD_ABILITY_SLOT, addButton("Add Manifestation", "Click to add a manifestation"));
        }

        viewer.openInventory(inv);
    }

    private String nameOf(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString().substring(0, 8);
    }

    private ItemStack addButton(String title, String hint) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(hint, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack info(PlayerData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Level " + data.level(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        java.util.List<Component> lore = new java.util.ArrayList<>();
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
