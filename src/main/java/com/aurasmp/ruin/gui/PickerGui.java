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

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        UUID target;
        boolean ability;
        Card replaceCard;        // talent being replaced (null = add)
        Ability replaceAbility;  // manifestation being replaced (null = add)
        final Map<Integer, Card> cardSlots = new HashMap<>();
        final Map<Integer, Ability> abilitySlots = new HashMap<>();

        @Override public Inventory getInventory() { return inventory; }
    }

    public void openTalent(Player viewer, UUID target, Card replacing) {
        Holder h = new Holder();
        h.target = target;
        h.ability = false;
        h.replaceCard = replacing;
        Inventory inv = Bukkit.createInventory(h, 54,
                Component.text(replacing == null ? "Add a Talent" : "Replace Talent", NamedTextColor.DARK_AQUA));
        h.inventory = inv;
        int i = 0;
        for (Card card : Card.values()) {
            if (i >= 54) break;
            h.cardSlots.put(i, card);
            inv.setItem(i, icon(card.icon(), card.displayName(), card.rarity().color(),
                    card.rarity().label() + " · " + card.description()));
            i++;
        }
        viewer.openInventory(inv);
    }

    public void openAbility(Player viewer, UUID target, Ability replacing) {
        Holder h = new Holder();
        h.target = target;
        h.ability = true;
        h.replaceAbility = replacing;
        Inventory inv = Bukkit.createInventory(h, 36,
                Component.text(replacing == null ? "Add a Manifestation" : "Replace Manifestation", NamedTextColor.DARK_PURPLE));
        h.inventory = inv;
        int i = 0;
        for (Ability ability : Ability.values()) {
            if (i >= 36) break;
            h.abilitySlots.put(i, ability);
            inv.setItem(i, icon(ability.icon(), ability.displayName(), NamedTextColor.LIGHT_PURPLE,
                    ability.description() + "  (" + (ability.cooldownMillis() / 1000) + "s)"));
            i++;
        }
        viewer.openInventory(inv);
    }

    /** Apply the chosen pick to the target's data, then reopen the build editor. */
    public void handlePick(Player viewer, Holder holder, int slot) {
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
