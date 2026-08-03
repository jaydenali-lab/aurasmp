package com.aurasmp.ruin.gui;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import com.aurasmp.ruin.skilltree.SkillTree;
import com.aurasmp.ruin.skilltree.SkillTree.Branch;
import com.aurasmp.ruin.skilltree.SkillTree.Node;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

/** The /skilltree menu: pick a branch, spend skill points on nodes. */
public final class SkillTreeGui {

    private static final int BACK_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int[] BRANCH_SLOTS = {20, 21, 22, 23, 24};
    private static final Material[] BRANCH_ICONS = {
            Material.IRON_SWORD, Material.BOW, Material.TNT, Material.GOLDEN_APPLE, Material.WITHER_ROSE};

    private final RuinPlugin plugin;

    public SkillTreeGui(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    /** Holder that carries which page (null = branch overview) is open. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        Branch branch; // null = overview
        final Map<Integer, Node> nodeSlots = new HashMap<>();
        final Map<Integer, Branch> branchSlots = new HashMap<>();

        @Override public Inventory getInventory() { return inventory; }
    }

    public void openOverview(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 45,
                Component.text("✦ Skill Tree", NamedTextColor.DARK_AQUA));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(INFO_SLOT, infoItem(data));
        Branch[] branches = Branch.values();
        for (int i = 0; i < branches.length; i++) {
            Branch branch = branches[i];
            holder.branchSlots.put(BRANCH_SLOTS[i], branch);
            inv.setItem(BRANCH_SLOTS[i], branchItem(data, branch, BRANCH_ICONS[i]));
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.3f);
    }

    public void openBranch(Player player, Branch branch) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Holder holder = new Holder();
        holder.branch = branch;
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("✦ " + branch.displayName() + " — " + data.skillPoints() + " SP",
                        NamedTextColor.DARK_AQUA));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(BACK_SLOT, chrome(simple(Material.ARROW, "◀ Back", NamedTextColor.YELLOW,
                List.of("To the branch overview")), "ui_back"));
        inv.setItem(INFO_SLOT, infoItem(data));
        inv.setItem(8, chrome(simple(Material.LADDER, "Tiers", NamedTextColor.AQUA, List.of(
                "The tree grows outward: each tier opens",
                "once you own a node of the previous tier.",
                "Spent here: " + SkillTree.spentInBranch(data, branch) + " SP")), "ui_tiers"));

        int slot = 9;
        for (Node node : SkillTree.branchNodes(branch)) {
            if (slot >= 54) break;
            holder.nodeSlots.put(slot, node);
            inv.setItem(slot, nodeItem(data, node));
            slot++;
        }
        player.openInventory(inv);
    }

    /** Handles a click; re-renders on unlock. */
    public void handleClick(Player player, Holder holder, int slot) {
        if (holder.branch == null) {
            Branch branch = holder.branchSlots.get(slot);
            if (branch != null) openBranch(player, branch);
            return;
        }
        if (slot == BACK_SLOT) {
            openOverview(player);
            return;
        }
        Node node = holder.nodeSlots.get(slot);
        if (node == null) return;

        PlayerData data = plugin.data().get(player.getUniqueId());
        if (SkillTree.owns(data, node)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.2f);
            return;
        }
        if (!SkillTree.tierUnlocked(data, node.branch(), node.tier())) {
            player.sendMessage(Component.text("Tier " + node.tier() + " is locked — unlock a Tier "
                    + (node.tier() - 1) + " node in " + node.branch().displayName()
                    + " first.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        if (data.skillPoints() < node.cost()) {
            player.sendMessage(Component.text("Not enough skill points — " + node.displayName()
                    + " costs " + node.cost() + " SP, you have " + data.skillPoints() + ".",
                    NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }
        if (node.isAbility() && data.abilities().size() >= PlayerData.MAX_ABILITIES) {
            player.sendMessage(Component.text("You already carry " + PlayerData.MAX_ABILITIES
                    + " manifestations — use a Mirror Shard to respec.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            return;
        }

        // Unlock it.
        data.addSkillPoints(-node.cost());
        if (node.isAbility()) {
            if (!data.abilities().contains(node.ability())) data.abilities().add(node.ability());
            plugin.items().refreshCastItems(player, data);
        } else {
            data.cards().add(node.card());
            plugin.cards().recalc(player, data);
        }
        plugin.data().save(player.getUniqueId(), data);
        plugin.treeAdvancements().sync(player);
        player.sendMessage(Component.text("Unlocked ", NamedTextColor.GRAY)
                .append(Component.text(node.displayName(), NamedTextColor.GREEN))
                .append(Component.text("  (-" + node.cost() + " SP, " + data.skillPoints() + " left)",
                        NamedTextColor.DARK_GRAY)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        openBranch(player, node.branch()); // re-render with new state
    }

    // ---- items ----

    /** Applies one of the ruin:ui_* resource-pack models to a menu item. */
    private ItemStack chrome(ItemStack item, String modelKey) {
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("ruin:" + modelKey));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(PlayerData data) {
        return chrome(simple(Material.NETHER_STAR,
                data.skillPoints() + " Skill Points", NamedTextColor.LIGHT_PURPLE, List.of(
                        "Level " + data.level() + "  ·  " + SkillTree.SP_PER_LEVEL + " SP per level-up",
                        "Spent: " + SkillTree.spentPoints(data) + " SP",
                        "Mirror Shard refunds everything.")), "ui_points");
    }

    private ItemStack branchItem(PlayerData data, Branch branch, Material icon) {
        int owned = 0, total = 0;
        for (Node node : SkillTree.branchNodes(branch)) {
            total++;
            if (SkillTree.owns(data, node)) owned++;
        }
        ItemStack item = simple(icon, branch.displayName(), NamedTextColor.AQUA, List.of(
                branch.description(),
                "Unlocked: " + owned + " / " + total,
                "Spent here: " + SkillTree.spentInBranch(data, branch) + " SP",
                "Click to open"));
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("ruin:ui_branch_"
                + branch.name().toLowerCase(java.util.Locale.ROOT)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack nodeItem(PlayerData data, Node node) {
        boolean owned = SkillTree.owns(data, node);
        boolean tierOpen = SkillTree.tierUnlocked(data, node.branch(), node.tier());
        boolean affordable = data.skillPoints() >= node.cost();

        ItemStack item = new ItemStack(node.isAbility() ? node.ability().icon() : node.card().icon());
        ItemMeta meta = item.getItemMeta();
        String modelKey = node.isAbility() ? node.ability().modelKey() : node.card().modelKey();
        if (!owned && !tierOpen) modelKey += "_locked"; // dimmed art with a padlock
        meta.setItemModel(NamespacedKey.fromString("ruin:" + modelKey));

        NamedTextColor titleColor = owned ? NamedTextColor.GREEN
                : !tierOpen ? NamedTextColor.DARK_GRAY
                : affordable ? NamedTextColor.WHITE : NamedTextColor.RED;
        meta.displayName(Component.text(node.displayName(), titleColor)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        String kind = node.isAbility() ? "Manifestation"
                : node.card().rarity().label() + " Talent";
        lore.add(Component.text("Tier " + node.tier() + " · " + kind, NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        for (String line : wrap(node.description(), 34)) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (node.isAbility()) {
            lore.add(Component.text("Cooldown: " + (node.ability().cooldownMillis() / 1000) + "s",
                    NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        if (owned) {
            lore.add(Component.text("✔ Unlocked", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (!tierOpen) {
            lore.add(Component.text("Locked — own a Tier " + (node.tier() - 1)
                    + " node in this branch first", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Cost: " + node.cost() + " SP" + (affordable ? "  — click to unlock" : ""),
                    affordable ? NamedTextColor.YELLOW : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack simple(Material material, String title, NamedTextColor color, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        meta.setItemModel(NamespacedKey.fromString("ruin:ui_fill"));
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
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
