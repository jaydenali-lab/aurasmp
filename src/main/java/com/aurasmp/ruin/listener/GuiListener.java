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
}
