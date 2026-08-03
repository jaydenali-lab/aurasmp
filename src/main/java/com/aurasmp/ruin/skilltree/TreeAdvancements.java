package com.aurasmp.ruin.skilltree;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors the skill tree into the vanilla advancements screen — the
 * full-screen, Wynncraft-style node web (press L, "Ruin" tab). Unlocked
 * nodes light up; the /skilltree menu stays the place you spend points.
 *
 * <p>Advancements are injected at runtime via {@code UnsafeValues#loadAdvancement}
 * and persist in the world's datapack storage.
 */
public final class TreeAdvancements {

    private static final String CRITERION = "unlocked";

    /**
     * Bump whenever the tree's shape (parents, tiers, text) changes. Bukkit
     * persists loaded advancements into the world datapack and an existing key
     * can't be replaced while the server runs, so on a revision change we purge
     * the stored files — the fresh layout loads on the next restart.
     */
    private static final int TREE_REVISION = 2;

    private final RuinPlugin plugin;
    private final Map<String, NamespacedKey> nodeKeys = new HashMap<>();
    private boolean available = false;

    public TreeAdvancements(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    private void load(NamespacedKey key, String json) {
        if (Bukkit.getAdvancement(key) == null) {
            Bukkit.getUnsafe().loadAdvancement(key, json);
        }
    }

    private static String esc(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String display(String iconModel, String title, String desc,
                                  String frame, String background) {
        return "\"display\":{"
                + "\"icon\":{\"id\":\"minecraft:paper\",\"components\":{\"minecraft:item_model\":\"" + iconModel + "\"}},"
                + "\"title\":\"" + esc(title) + "\","
                + "\"description\":\"" + esc(desc) + "\","
                + "\"frame\":\"" + frame + "\","
                + "\"show_toast\":false,\"announce_to_chat\":false,\"hidden\":false"
                + (background != null ? ",\"background\":\"" + background + "\"" : "")
                + "}";
    }

    private static String body(String displayJson, String parent) {
        return "{" + (parent != null ? "\"parent\":\"" + parent + "\"," : "")
                + displayJson + ","
                + "\"criteria\":{\"" + CRITERION + "\":{\"trigger\":\"minecraft:impossible\"}}}";
    }

    /** True when the stored tree revision matches; refreshes the marker file. */
    @SuppressWarnings("deprecation")
    private boolean revisionCurrent() {
        java.io.File marker = new java.io.File(plugin.getDataFolder(), "tree-revision.txt");
        try {
            if (marker.isFile()
                    && java.nio.file.Files.readString(marker.toPath()).trim()
                            .equals(String.valueOf(TREE_REVISION))) {
                return true;
            }
            plugin.getDataFolder().mkdirs();
            java.nio.file.Files.writeString(marker.toPath(), String.valueOf(TREE_REVISION));
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Couldn't read/write tree-revision marker: " + e.getMessage());
        }
        return false;
    }

    /** Deletes every stored ruin advancement so the new layout loads next boot. */
    @SuppressWarnings("deprecation")
    private void purgeStored() {
        List<NamespacedKey> keys = new ArrayList<>();
        keys.add(new NamespacedKey("ruin", "root"));
        for (SkillTree.Branch branch : SkillTree.Branch.values()) {
            keys.add(new NamespacedKey("ruin", "branch_" + branch.name().toLowerCase(java.util.Locale.ROOT)));
            for (SkillTree.Node node : SkillTree.branchNodes(branch)) {
                String enumName = node.isAbility() ? node.ability().name() : node.card().name();
                keys.add(new NamespacedKey("ruin", "node_" + enumName.toLowerCase(java.util.Locale.ROOT)));
            }
        }
        for (NamespacedKey key : keys) Bukkit.getUnsafe().removeAdvancement(key);
        plugin.getLogger().warning("Skill-tree layout changed — the advancements tab (press L) shows the "
                + "new paths after ONE more server restart.");
    }

    /** Builds the whole tab: root -> branch entries -> tier chains. */
    public void install() {
        try {
            // Layout changed since this world last saw it: purge the stale stored
            // copy (an existing key can't be replaced live; next boot loads fresh).
            if (!revisionCurrent() && Bukkit.getAdvancement(new NamespacedKey("ruin", "root")) != null) {
                purgeStored();
            }
            NamespacedKey rootKey = new NamespacedKey("ruin", "root");
            load(rootKey, body(display("ruin:ui_points", "Ruin",
                    "The skill tree. Spend points with /skilltree.",
                    "task", "minecraft:block/deepslate_tiles"), null));

            for (SkillTree.Branch branch : SkillTree.Branch.values()) {
                String branchId = "branch_" + branch.name().toLowerCase(java.util.Locale.ROOT);
                NamespacedKey branchKey = new NamespacedKey("ruin", branchId);
                load(branchKey, body(display("ruin:ui_branch_" + branch.name().toLowerCase(java.util.Locale.ROOT),
                        branch.displayName(), branch.description(), "task", null), "ruin:root"));

                // Real paths: every node hangs off its actual path parent
                // (tier-1 nodes hang off the branch entry). branchNodes() is
                // tier-sorted, so parents always load before their children.
                for (SkillTree.Node node : SkillTree.branchNodes(branch)) {
                    String enumName = node.isAbility() ? node.ability().name() : node.card().name();
                    String id = "node_" + enumName.toLowerCase(java.util.Locale.ROOT);
                    String model = "ruin:" + (node.isAbility()
                            ? node.ability().modelKey() : node.card().modelKey());
                    String frame = node.isAbility() ? "challenge"
                            : node.card().rarity() == com.aurasmp.ruin.card.Rarity.LEGENDARY ? "goal" : "task";
                    String desc = node.description() + " (Tier " + node.tier()
                            + ", " + node.cost() + " SP)";
                    String parent = node.parent() == null ? "ruin:" + branchId
                            : "ruin:node_" + (node.parent().isAbility()
                                    ? node.parent().ability().name() : node.parent().card().name())
                                    .toLowerCase(java.util.Locale.ROOT);
                    NamespacedKey key = new NamespacedKey("ruin", id);
                    load(key, body(display(model, node.displayName(), desc, frame, null), parent));
                    nodeKeys.put(enumName, key);
                }
            }
            available = true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Advancement tree unavailable on this server version: " + t.getMessage());
            available = false;
        }
    }

    /** Lights up the player's tab to match their build (root + branches always lit). */
    public void sync(Player player) {
        if (!available) return;
        PlayerData data = plugin.data().get(player.getUniqueId());
        grant(player, new NamespacedKey("ruin", "root"), true);
        for (SkillTree.Branch branch : SkillTree.Branch.values()) {
            grant(player, new NamespacedKey("ruin",
                    "branch_" + branch.name().toLowerCase(java.util.Locale.ROOT)), true);
        }
        for (Map.Entry<String, NamespacedKey> entry : nodeKeys.entrySet()) {
            boolean owned;
            try {
                owned = data.hasCard(com.aurasmp.ruin.card.Card.valueOf(entry.getKey()));
            } catch (IllegalArgumentException notCard) {
                owned = data.hasAbility(com.aurasmp.ruin.ability.Ability.valueOf(entry.getKey()));
            }
            grant(player, entry.getValue(), owned);
        }
    }

    private void grant(Player player, NamespacedKey key, boolean state) {
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (state && !progress.isDone()) {
            progress.awardCriteria(CRITERION);
        } else if (!state && progress.isDone()) {
            progress.revokeCriteria(CRITERION);
        }
    }
}
