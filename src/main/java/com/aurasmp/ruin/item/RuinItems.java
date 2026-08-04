package com.aurasmp.ruin.item;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Factory + identity for Ruin's custom items (Mirror Shard, Ruin Catalyst). */
public final class RuinItems {

    public static final String MIRROR_SHARD = "mirror_shard";
    public static final String CATALYST = "catalyst";
    public static final String TALENT_HAND = "hand_talent";
    public static final String MANIFEST_HAND = "hand_manifest";

    private final RuinPlugin plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey castKey;

    public RuinItems(RuinPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
        this.castKey = new NamespacedKey(plugin, "cast_ability");
    }

    /** Reads our custom-item tag, or null for vanilla/foreign items. */
    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean isMirrorShard(ItemStack item) { return MIRROR_SHARD.equals(idOf(item)); }
    public boolean isCatalyst(ItemStack item) { return CATALYST.equals(idOf(item)); }
    public boolean isTalentHand(ItemStack item) { return TALENT_HAND.equals(idOf(item)); }
    public boolean isManifestHand(ItemStack item) { return MANIFEST_HAND.equals(idOf(item)); }
    public boolean isHand(ItemStack item) { return isTalentHand(item) || isManifestHand(item); }

    /** The ability a cast item fires, or null for anything else. */
    public com.aurasmp.ruin.ability.Ability castAbility(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(castKey, PersistentDataType.STRING);
        if (name == null) return null;
        try {
            return com.aurasmp.ruin.ability.Ability.valueOf(name);
        } catch (IllegalArgumentException gone) {
            return null;
        }
    }

    public boolean isCastItem(ItemStack item) { return castAbility(item) != null; }

    /** Soulbound items (hands, cast items, legacy Catalyst) that must never leave the owner. */
    public boolean isBoundItem(ItemStack item) {
        return isCatalyst(item) || isCastItem(item) || isHand(item);
    }

    /** A stashable talent hand: right-click to draw 3 talents and pick one. */
    public ItemStack talentHand() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name("Talent Hand", NamedTextColor.AQUA));
        meta.lore(List.of(
                lore("A fan of possible selves."),
                lore(""),
                line("Right-click", NamedTextColor.YELLOW, " to draw 3 talents and pick one."),
                lore("Only talents your stats qualify for appear."),
                lore(""),
                line("Soulbound.", NamedTextColor.DARK_GRAY, " Stash it, but it can't leave you.")));
        meta.setItemModel(NamespacedKey.fromString("ruin:hand_talent"));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, TALENT_HAND);
        item.setItemMeta(meta);
        return item;
    }

    /** A stashable manifestation hand: right-click to draw 3 manifestations. */
    public ItemStack manifestHand() {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name("Manifestation Hand", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                lore("A fan of latent power."),
                lore(""),
                line("Right-click", NamedTextColor.YELLOW, " to draw 3 manifestations and learn one."),
                lore("Only manifestations your stats qualify for appear."),
                lore(""),
                line("Soulbound.", NamedTextColor.DARK_GRAY, " Stash it, but it can't leave you.")));
        meta.setItemModel(NamespacedKey.fromString("ruin:hand_manifest"));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, MANIFEST_HAND);
        item.setItemMeta(meta);
        return item;
    }

    /** Strips every hand from an inventory (mirror shard wipe). */
    public void removeHands(org.bukkit.entity.Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isHand(contents[i])) player.getInventory().setItem(i, null);
        }
    }

    /** One cast item per manifestation — right-click it to cast, shows its own glyph icon. */
    public ItemStack castItem(Ability ability) {
        ItemStack item = new ItemStack(ability.icon());
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(castKey, PersistentDataType.STRING, ability.name());
        meta.setItemModel(org.bukkit.NamespacedKey.fromString("ruin:" + ability.modelKey()));
        meta.displayName(net.kyori.adventure.text.Component.text(ability.displayName(),
                        net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                net.kyori.adventure.text.Component.text(ability.description(),
                                net.kyori.adventure.text.format.NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                net.kyori.adventure.text.Component.text("Right-click to cast · "
                                + (ability.cooldownMillis() / 1000) + "s cooldown",
                                net.kyori.adventure.text.format.NamedTextColor.GOLD)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Syncs the player's inventory with their unlocked manifestations: strips
     * stale cast items and legacy Catalysts, then hands out one cast item per
     * owned ability that's missing.
     */
    public void refreshCastItems(org.bukkit.entity.Player player, com.aurasmp.ruin.data.PlayerData data) {
        java.util.Set<Ability> present = java.util.EnumSet.noneOf(Ability.class);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isCatalyst(item)) {
                player.getInventory().setItem(i, null); // Catalyst retired in 2.0
                continue;
            }
            Ability ability = castAbility(item);
            if (ability == null) continue;
            if (!data.hasAbility(ability) || !present.add(ability)) {
                player.getInventory().setItem(i, null); // no longer owned, or duplicate
            }
        }
        for (Ability ability : data.abilities()) {
            if (!present.contains(ability)) player.getInventory().addItem(castItem(ability));
        }
    }

    public ItemStack create(String id) {
        return switch (id) {
            case MIRROR_SHARD -> mirrorShard();
            case CATALYST -> catalyst(List.of());
            default -> null;
        };
    }

    public ItemStack mirrorShard() {
        ItemStack item = new ItemStack(Material.PRISMARINE_CRYSTALS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name("Mirror Shard", NamedTextColor.AQUA));
        meta.lore(List.of(
                lore("A reflection of what could have been."),
                lore(""),
                line("Right-click", NamedTextColor.YELLOW, " to reset your Ruin"),
                line("level", NamedTextColor.GRAY, " and re-draft your build."),
                lore(""),
                line("Warning:", NamedTextColor.RED, " wipes your build.")));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // Points the client at the custom model "ruin:mirror_shard" supplied by the
        // resource pack. Without the pack it renders the base material instead.
        meta.setItemModel(new NamespacedKey(plugin, MIRROR_SHARD));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, MIRROR_SHARD);
        item.setItemMeta(meta);
        return item;
    }

    /** Builds the Catalyst, embedding the player's current ability bindings into the lore. */
    public ItemStack catalyst(List<Ability> abilities) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name("Ruin Catalyst", NamedTextColor.LIGHT_PURPLE));

        List<Component> lore = new ArrayList<>();
        lore.add(lore("Cast your learned manifestations."));
        lore.add(lore(""));
        Ability primary = abilities.isEmpty() ? null : abilities.get(0);
        Ability secondary = abilities.size() > 1 ? abilities.get(1) : null;
        lore.add(line("Right-click:", NamedTextColor.YELLOW,
                " " + (primary != null ? primary.displayName() : "—")));
        lore.add(line("Shift + Right-click:", NamedTextColor.YELLOW,
                " " + (secondary != null ? secondary.displayName() : "—")));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setItemModel(new NamespacedKey(plugin, CATALYST));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, CATALYST);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Registers a default Mirror Shard recipe so the item is obtainable in survival.
     * Swap this out for your own recipe — the only thing that matters is that the
     * crafted result is {@link #mirrorShard()} (it carries the identifying tag).
     */
    public void registerRecipes() {
        NamespacedKey key = new NamespacedKey(plugin, "mirror_shard");
        if (plugin.getServer().getRecipe(key) != null) return;
        ShapedRecipe recipe = new ShapedRecipe(key, mirrorShard());
        recipe.shape("AEA", "ENE", "AEA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        plugin.getServer().addRecipe(recipe);
    }

    private Component name(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private Component lore(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private Component line(String head, NamedTextColor headColor, String tail) {
        return Component.text(head, headColor)
                .append(Component.text(tail, NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false);
    }
}
