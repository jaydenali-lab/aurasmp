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
