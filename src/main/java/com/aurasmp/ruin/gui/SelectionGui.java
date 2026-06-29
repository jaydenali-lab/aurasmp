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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds and drives the level-up draft menu (3 random options, pick one). */
public final class SelectionGui {

    private static final int[] OPTION_SLOTS = {11, 13, 15};

    private final RuinPlugin plugin;
    private final Map<UUID, Deque<Boolean>> pending = new HashMap<>();   // true = ability pick
    private final Map<UUID, Session> open = new HashMap<>();

    public SelectionGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Holder so we can recognise our own inventories in the listener. */
    public static final class Session implements InventoryHolder {
        private final boolean ability;
        private final Map<Integer, Card> cardSlots = new HashMap<>();
        private final Map<Integer, Ability> abilitySlots = new HashMap<>();
        private Inventory inventory;

        Session(boolean ability) { this.ability = ability; }

        @Override public Inventory getInventory() { return inventory; }
    }

    public void queue(Player player, boolean ability) {
        pending.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).add(ability);
    }

    /** Opens the next pending pick if the player has no menu open already. */
    public void openNextIfIdle(Player player) {
        if (open.containsKey(player.getUniqueId())) return;
        openNext(player);
    }

    private void openNext(Player player) {
        Deque<Boolean> queue = pending.get(player.getUniqueId());
        if (queue == null || queue.isEmpty()) return;
        boolean ability = queue.peekFirst();
        PlayerData data = plugin.data().get(player.getUniqueId());

        Session session = ability ? buildAbilityMenu(data) : buildCardMenu(data);
        if (session == null) {
            // Nothing left to offer (owns everything / ability slots full) — drop the pick.
            queue.pollFirst();
            openNext(player);
            return;
        }
        open.put(player.getUniqueId(), session);
        player.openInventory(session.inventory);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
    }

    private Session buildCardMenu(PlayerData data) {
        List<Card> pool = new ArrayList<>();
        for (Card card : Card.values()) {
            if (!data.hasCard(card)) pool.add(card);
        }
        if (pool.isEmpty()) return null;
        Collections.shuffle(pool);

        Session session = new Session(false);
        Inventory inv = Bukkit.createInventory(session, 27,
                Component.text("Draft a Talent", NamedTextColor.DARK_AQUA));
        session.inventory = inv;
        fill(inv);
        int count = Math.min(3, pool.size());
        for (int i = 0; i < count; i++) {
            Card card = pool.get(i);
            int slot = OPTION_SLOTS[i];
            session.cardSlots.put(slot, card);
            NamedTextColor color = card.isAttribute() ? NamedTextColor.AQUA : NamedTextColor.GREEN;
            inv.setItem(slot, icon(card.icon(), card.displayName(), color, card.description()));
        }
        return session;
    }

    private Session buildAbilityMenu(PlayerData data) {
        if (data.abilities().size() >= PlayerData.MAX_ABILITIES) return null;
        List<Ability> pool = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            if (!data.hasAbility(ability)) pool.add(ability);
        }
        if (pool.isEmpty()) return null;
        Collections.shuffle(pool);

        Session session = new Session(true);
        Inventory inv = Bukkit.createInventory(session, 27,
                Component.text("Draft a Mantra", NamedTextColor.DARK_PURPLE));
        session.inventory = inv;
        fill(inv);
        int count = Math.min(3, pool.size());
        for (int i = 0; i < count; i++) {
            Ability ability = pool.get(i);
            int slot = OPTION_SLOTS[i];
            session.abilitySlots.put(slot, ability);
            inv.setItem(slot, icon(ability.icon(), ability.displayName(), NamedTextColor.LIGHT_PURPLE,
                    ability.description() + "  (CD " + (ability.cooldownMillis() / 1000) + "s)"));
        }
        return session;
    }

    /** Handle a click in one of our menus. Returns true if the event was ours. */
    public boolean handleClick(Player player, Session session, int slot) {
        if (!open.containsValue(session)) return true; // stale menu, just swallow
        if (session.ability) {
            Ability picked = session.abilitySlots.get(slot);
            if (picked == null) return true;
            applyAbility(player, picked);
        } else {
            Card picked = session.cardSlots.get(slot);
            if (picked == null) return true;
            applyCard(player, picked);
        }
        // Consume this pick and advance.
        open.remove(player.getUniqueId());
        Deque<Boolean> queue = pending.get(player.getUniqueId());
        if (queue != null) queue.pollFirst();
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> openNextIfIdle(player));
        return true;
    }

    /** If a menu is closed without a choice, re-open it next tick (picks are mandatory). */
    public void handleClose(Player player, Session session) {
        if (open.get(player.getUniqueId()) != session) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && open.get(player.getUniqueId()) == session) {
                player.openInventory(session.inventory);
            }
        });
    }

    public boolean hasOpen(Player player) {
        return open.containsKey(player.getUniqueId());
    }

    public void clear(UUID id) {
        pending.remove(id);
        open.remove(id);
    }

    private void applyCard(Player player, Card card) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        data.cards().add(card);
        plugin.cards().recalc(player, data);
        plugin.data().save(player.getUniqueId(), data);
        player.sendMessage(Component.text("✦ Talent gained: ", NamedTextColor.GRAY)
                .append(Component.text(card.displayName(), NamedTextColor.AQUA))
                .append(Component.text(" — " + card.description(), NamedTextColor.GRAY)));
    }

    private void applyAbility(Player player, Ability ability) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (!data.abilities().contains(ability)) data.abilities().add(ability);
        refreshCatalyst(player, data);
        plugin.data().save(player.getUniqueId(), data);
        player.sendMessage(Component.text("✦ Mantra learned: ", NamedTextColor.GRAY)
                .append(Component.text(ability.displayName(), NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" — " + ability.description(), NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Use the Ruin Catalyst to cast it.", NamedTextColor.DARK_GRAY));
    }

    /** Removes any existing Catalysts and gives a fresh one reflecting current bindings. */
    public void refreshCatalyst(Player player, PlayerData data) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (plugin.items().isCatalyst(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
        if (!data.abilities().isEmpty()) {
            player.getInventory().addItem(plugin.items().catalyst(data.abilities()));
        }
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private ItemStack icon(Material material, String title, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : wrap(description, 32)) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Click to choose", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
