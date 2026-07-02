package com.aurasmp.ruin.listener;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.gui.SelectionGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/** Routes clicks/closes on the draft menu into {@link SelectionGui}. */
public final class GuiListener implements Listener {

    private final RuinPlugin plugin;

    public GuiListener(RuinPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // Build viewer — read-only, except the admin who edits via clicks.
        if (holder instanceof com.aurasmp.ruin.gui.BuildGui.Holder build) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player p)) return;
            if (!build.editable() || !com.aurasmp.ruin.gui.BuildGui.isAdmin(p)) return; // silent for everyone else
            if (event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getInventory())) return;
            int slot = event.getSlot();
            if (build.talentAt(slot) != null) {
                plugin.pickerGui().openTalent(p, build.target(), build.talentAt(slot));
            } else if (build.abilityAt(slot) != null) {
                plugin.pickerGui().openAbility(p, build.target(), build.abilityAt(slot));
            } else if (build.isAddTalent(slot)) {
                plugin.pickerGui().openTalent(p, build.target(), null);
            } else if (build.isAddAbility(slot)) {
                plugin.pickerGui().openAbility(p, build.target(), null);
            }
            return;
        }

        // Admin picker — choose a talent/manifestation to set.
        if (holder instanceof com.aurasmp.ruin.gui.PickerGui.Holder picker) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player p)) return;
            if (!com.aurasmp.ruin.gui.BuildGui.isAdmin(p)) return;
            if (event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getInventory())) return;
            plugin.pickerGui().handlePick(p, picker, event.getSlot());
            return;
        }

        if (!(holder instanceof SelectionGui.Session session)) return;

        event.setCancelled(true); // draft menus are never editable
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return; // clicked their own inventory, ignore
        }
        plugin.gui().handleClick(player, session, event.getSlot());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SelectionGui.Session session)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.gui().handleClose(player, session);
    }

    // ---- Catalyst containment: it must never leave the owner's inventory ----
    // (stashing it in a chest and letting join/respawn hand out a fresh one = free
    // nether stars; same for crafting it into a beacon).

    @EventHandler(ignoreCancelled = true)
    public void onCatalystStash(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Top inventory CRAFTING = plain player view, nothing foreign open.
        if (event.getView().getTopInventory().getType()
                == org.bukkit.event.inventory.InventoryType.CRAFTING) return;
        boolean touchesCatalyst = plugin.items().isCatalyst(event.getCurrentItem())
                || plugin.items().isCatalyst(event.getCursor())
                || (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                    && plugin.items().isCatalyst(player.getInventory().getItem(event.getHotbarButton())));
        if (touchesCatalyst) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCatalystDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!plugin.items().isCatalyst(event.getOldCursor())) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getView().getTopInventory().getType()
                == org.bukkit.event.inventory.InventoryType.CRAFTING) return;
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) { // dragging into the open container
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCatalystCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        for (org.bukkit.inventory.ItemStack item : event.getInventory().getMatrix()) {
            if (plugin.items().isCatalyst(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
