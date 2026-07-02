package com.aurasmp.ruin.gui;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;
import com.aurasmp.ruin.card.Rarity;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Builds and drives the level-up draft menu (5 talents / 4 manifestations, pick one). */
public final class SelectionGui {

    private static final int[] CARD_SLOTS = {11, 12, 13, 14, 15};   // 5 talent options
    private static final int[] ABILITY_SLOTS = {10, 12, 14, 16};    // 4 manifestation options
    private static final int REROLL_SLOT = 22;

    private final RuinPlugin plugin;
    private final Map<UUID, Deque<Boolean>> pending = new HashMap<>();   // true = ability pick
    private final Map<UUID, Session> open = new HashMap<>();

    public SelectionGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Holder so we can recognise our own inventories in the listener. */
    public static final class Session implements InventoryHolder {
        private final boolean ability;
        private final long openedAt = System.currentTimeMillis();
        private final Map<Integer, Card> cardSlots = new HashMap<>();
        private final Map<Integer, Ability> abilitySlots = new HashMap<>();
        private Inventory inventory;

        Session(boolean ability) { this.ability = ability; }

        @Override public Inventory getInventory() { return inventory; }
    }

    /**
     * Draft-menu invincibility, capped at 10s per menu so leaving one open
     * cannot be used as on-demand permanent immunity.
     */
    public boolean isProtected(Player player) {
        Session session = open.get(player.getUniqueId());
        return session != null && System.currentTimeMillis() - session.openedAt < 10_000;
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

        int remaining = queue.size() - 1;
        Session session = ability ? buildAbilityMenu(data, remaining) : buildCardMenu(data, remaining);
        if (session == null) {
            // Nothing left to offer (owns everything / ability slots full) — drop the pick.
            queue.pollFirst();
            openNext(player);
            return;
        }
        open.put(player.getUniqueId(), session);
        player.openInventory(session.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
    }

    private Session buildCardMenu(PlayerData data, int remaining) {
        List<Card> pool = new ArrayList<>();
        for (Card card : Card.values()) {
            if (!data.hasCard(card)) pool.add(card);
        }
        if (pool.isEmpty()) return null;
        List<Card> chosen = weightedPick(pool, CARD_SLOTS.length);

        Session session = new Session(false);
        Inventory inv = Bukkit.createInventory(session, 27,
                Component.text("✦ Choose a Talent", NamedTextColor.DARK_AQUA));
        session.inventory = inv;
        fill(inv);
        for (int i = 0; i < chosen.size(); i++) {
            Card card = chosen.get(i);
            session.cardSlots.put(CARD_SLOTS[i], card);
            inv.setItem(CARD_SLOTS[i], cardIcon(card));
            // A rarity-colored pane above each option so the roll reads at a glance.
            inv.setItem(CARD_SLOTS[i] - 9, rarityPane(card.rarity()));
        }
        placeReroll(inv, data);
        placeQueued(inv, remaining);
        return session;
    }

    private Session buildAbilityMenu(PlayerData data, int remaining) {
        if (data.abilities().size() >= PlayerData.MAX_ABILITIES) return null;
        List<Ability> pool = new ArrayList<>();
        for (Ability ability : Ability.values()) {
            if (!data.hasAbility(ability)) pool.add(ability);
        }
        if (pool.isEmpty()) return null;
        Collections.shuffle(pool);

        Session session = new Session(true);
        Inventory inv = Bukkit.createInventory(session, 27,
                Component.text("✦ Choose a Manifestation", NamedTextColor.DARK_PURPLE));
        session.inventory = inv;
        fill(inv);
        int count = Math.min(ABILITY_SLOTS.length, pool.size());
        for (int i = 0; i < count; i++) {
            Ability ability = pool.get(i);
            session.abilitySlots.put(ABILITY_SLOTS[i], ability);
            inv.setItem(ABILITY_SLOTS[i], abilityIcon(ability, data));
            inv.setItem(ABILITY_SLOTS[i] - 9, pane(Material.MAGENTA_STAINED_GLASS_PANE));
        }
        placeReroll(inv, data);
        placeQueued(inv, remaining);
        return session;
    }

    /** Handle a click in one of our menus. Returns true if the event was ours. */
    public boolean handleClick(Player player, Session session, int slot) {
        if (!open.containsValue(session)) return true; // stale menu, just swallow
        if (slot == REROLL_SLOT) {
            handleReroll(player, session);
            return true;
        }
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

    private void handleReroll(Player player, Session session) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (data.rerolls() <= 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        data.setRerolls(data.rerolls() - 1);
        plugin.data().save(player.getUniqueId(), data);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.4f);
        // Invalidate the old session immediately — clicks on it are dead from here,
        // so a same-tick reroll+pick can't double-dip the draft.
        open.remove(player.getUniqueId());
        // Rebuild a fresh roll of the same type next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            Deque<Boolean> queue = pending.get(player.getUniqueId());
            int remaining = queue == null ? 0 : Math.max(0, queue.size() - 1);
            Session fresh = session.ability ? buildAbilityMenu(data, remaining) : buildCardMenu(data, remaining);
            if (fresh == null) return;
            open.put(player.getUniqueId(), fresh);
            player.openInventory(fresh.inventory);
        });
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
        player.sendMessage(Component.text("You picked ", NamedTextColor.GRAY)
                .append(Component.text(card.displayName(), card.rarity().color()))
                .append(Component.text(" [" + card.rarity().label() + "]", card.rarity().color())));
        // Rarer pick, bigger fanfare.
        switch (card.rarity()) {
            case LEGENDARY -> player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.3f);
            case EPIC -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            default -> player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
        }
    }

    private void applyAbility(Player player, Ability ability) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (!data.abilities().contains(ability)) data.abilities().add(ability);
        refreshCatalyst(player, data);
        plugin.data().save(player.getUniqueId(), data);
        player.sendMessage(Component.text("You picked ", NamedTextColor.GRAY)
                .append(Component.text(ability.displayName(), NamedTextColor.LIGHT_PURPLE)));
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.2f);
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

    private void placeReroll(Inventory inv, PlayerData data) {
        int left = data.rerolls();
        ItemStack item = new ItemStack(left > 0 ? Material.ENDER_EYE : Material.BARRIER);
        item.setAmount(Math.max(1, left)); // stack size mirrors rerolls left
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Reroll  " + left + "/" + PlayerData.DEFAULT_REROLLS,
                left > 0 ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(left > 0 ? "Click to reroll these options" : "None left",
                left > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        inv.setItem(REROLL_SLOT, item);
    }

    /** Bottom-left marker showing how many more drafts are waiting after this one. */
    private void placeQueued(Inventory inv, int remaining) {
        if (remaining <= 0) return;
        ItemStack item = new ItemStack(Material.ARROW);
        item.setAmount(Math.min(64, remaining));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("+" + remaining + " more pick" + (remaining == 1 ? "" : "s")
                + " after this", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        inv.setItem(18, item);
    }

    /** Manifestation option: description + cooldown + which click it will bind to. */
    private ItemStack abilityIcon(Ability ability, PlayerData data) {
        ItemStack item = new ItemStack(ability.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ability.displayName(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : wrap(ability.description(), 32)) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Cooldown: " + (ability.cooldownMillis() / 1000) + "s", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        String bind = data.abilities().isEmpty() ? "Right-click" : "Shift + Right-click";
        lore.add(Component.text("Binds to: " + bind, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click to pick", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack rarityPane(Rarity rarity) {
        Material material = switch (rarity) {
            case LEGENDARY -> Material.YELLOW_STAINED_GLASS_PANE;
            case EPIC -> Material.PURPLE_STAINED_GLASS_PANE;
            case RARE -> Material.BLUE_STAINED_GLASS_PANE;
            default -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        };
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(rarity.label(), rarity.color()).decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack cardIcon(Card card) {
        ItemStack item = new ItemStack(card.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(card.displayName(), card.rarity().color()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(card.rarity().label(), card.rarity().color()).decoration(TextDecoration.ITALIC, false));
        for (String line : wrap(card.description(), 32)) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Click to pick", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Pick {@code count} distinct cards from the pool, weighted by rarity (rarer = less likely). */
    private List<Card> weightedPick(List<Card> pool, int count) {
        List<Card> src = new ArrayList<>(pool);
        List<Card> out = new ArrayList<>();
        while (!src.isEmpty() && out.size() < count) {
            int total = 0;
            for (Card c : src) total += c.rarity().weight();
            int r = ThreadLocalRandom.current().nextInt(total);
            Card picked = src.get(src.size() - 1);
            for (Card c : src) {
                r -= c.rarity().weight();
                if (r < 0) { picked = c; break; }
            }
            out.add(picked);
            src.remove(picked);
        }
        return out;
    }

    private void fill(Inventory inv) {
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack inner = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, (i / 9) == 1 ? inner : border);
        }
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
