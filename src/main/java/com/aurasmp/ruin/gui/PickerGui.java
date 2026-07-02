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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Admin picker: choose any talent / manifestation to set on a target's build. */
public final class PickerGui {

    private final RuinPlugin plugin;

    public PickerGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Options per page — bottom row is reserved for the pager buttons. */
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        UUID target;
        boolean ability;
        int page;
        Card replaceCard;        // talent being replaced (null = add)
        Ability replaceAbility;  // manifestation being replaced (null = add)
        final Map<Integer, Card> cardSlots = new HashMap<>();
        final Map<Integer, Ability> abilitySlots = new HashMap<>();

        @Override public Inventory getInventory() { return inventory; }
    }

    public void openTalent(Player viewer, UUID target, Card replacing) {
        openTalent(viewer, target, replacing, 0);
    }

    private void openTalent(Player viewer, UUID target, Card replacing, int page) {
        Card[] all = Card.values();
        int pages = (all.length + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, pages - 1));

        Holder h = new Holder();
        h.target = target;
        h.ability = false;
        h.replaceCard = replacing;
        h.page = page;
        Inventory inv = Bukkit.createInventory(h, 54,
                Component.text((replacing == null ? "Add a Talent" : "Replace Talent")
                        + "  (" + (page + 1) + "/" + pages + ")", NamedTextColor.DARK_AQUA));
        h.inventory = inv;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= all.length) break;
            Card card = all[index];
            h.cardSlots.put(i, card);
            inv.setItem(i, icon(card.icon(), card.displayName(), card.rarity().color(),
                    card.rarity().label() + " · " + card.description()));
        }
        placePager(inv, page, pages);
        viewer.openInventory(inv);
    }

    public void openAbility(Player viewer, UUID target, Ability replacing) {
        openAbility(viewer, target, replacing, 0);
    }

    private void openAbility(Player viewer, UUID target, Ability replacing, int page) {
        Ability[] all = Ability.values();
        int pages = (all.length + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, pages - 1));

        Holder h = new Holder();
        h.target = target;
        h.ability = true;
        h.replaceAbility = replacing;
        h.page = page;
        Inventory inv = Bukkit.createInventory(h, 54,
                Component.text((replacing == null ? "Add a Manifestation" : "Replace Manifestation")
                        + "  (" + (page + 1) + "/" + pages + ")", NamedTextColor.DARK_PURPLE));
        h.inventory = inv;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= all.length) break;
            Ability ability = all[index];
            h.abilitySlots.put(i, ability);
            inv.setItem(i, icon(ability.icon(), ability.displayName(), NamedTextColor.LIGHT_PURPLE,
                    ability.description() + "  (" + (ability.cooldownMillis() / 1000) + "s)"));
        }
        placePager(inv, page, pages);
        viewer.openInventory(inv);
    }

    private void placePager(Inventory inv, int page, int pages) {
        if (page > 0) {
            inv.setItem(PREV_SLOT, icon(Material.ARROW, "◀ Previous page", NamedTextColor.YELLOW,
                    "Page " + page + " of " + pages));
        }
        if (page < pages - 1) {
            inv.setItem(NEXT_SLOT, icon(Material.ARROW, "Next page ▶", NamedTextColor.YELLOW,
                    "Page " + (page + 2) + " of " + pages));
        }
    }

    /** Apply the chosen pick to the target's data, then reopen the build editor. */
    public void handlePick(Player viewer, Holder holder, int slot) {
        // Pager buttons flip the page instead of picking.
        if (slot == PREV_SLOT || slot == NEXT_SLOT) {
            int page = holder.page + (slot == NEXT_SLOT ? 1 : -1);
            if (holder.ability) openAbility(viewer, holder.target, holder.replaceAbility, page);
            else openTalent(viewer, holder.target, holder.replaceCard, page);
            return;
        }

        PlayerData data = plugin.data().get(holder.target);
        Player target = Bukkit.getPlayer(holder.target);

        if (holder.ability) {
            Ability chosen = holder.abilitySlots.get(slot);
            if (chosen == null) return;
            List<Ability> list = data.abilities();
            if (holder.replaceAbility != null) {
                int idx = list.indexOf(holder.replaceAbility);
                list.remove(holder.replaceAbility); // drop the one being replaced
                list.remove(chosen);                // and any duplicate of the chosen
                int insert = Math.max(0, Math.min(idx < 0 ? list.size() : idx, list.size()));
                list.add(insert, chosen);
            } else if (!list.contains(chosen) && list.size() < PlayerData.MAX_ABILITIES) {
                list.add(chosen);
            }
            if (target != null) plugin.gui().refreshCatalyst(target, data);
        } else {
            Card chosen = holder.cardSlots.get(slot);
            if (chosen == null) return;
            if (holder.replaceCard != null) data.cards().remove(holder.replaceCard);
            data.cards().add(chosen);
            if (target != null) plugin.cards().recalc(target, data);
        }

        plugin.data().save(holder.target, data);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.buildGui().open(viewer, holder.target));
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
