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

    private final RuinPlugin plugin;
    private final NamespacedKey idKey;

    public RuinItems(RuinPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    /** Reads our custom-item tag, or null for vanilla/foreign items. */
    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean isMirrorShard(ItemStack item) { return MIRROR_SHARD.equals(idOf(item)); }
    public boolean isCatalyst(ItemStack item) { return CATALYST.equals(idOf(item)); }

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
                line("Warning:", NamedTextColor.RED, " consumes all talents & manifestations.")));
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
