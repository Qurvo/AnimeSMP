package com.animesmp.core.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class AbilityLoadoutListener implements Listener {

    private final AbilityLoadoutGui gui;

    public AbilityLoadoutListener(AbilityLoadoutGui gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof AbilityLoadoutHolder)) return;

        Player p = (Player) e.getWhoClicked();
        int raw = e.getRawSlot();

        // Save button (top priority)
        if (raw == 53) {
            e.setCancelled(true);
            gui.validateAndApply(p, e.getInventory());
            return;
        }

        // Block shift-clicking into or out of GUI
        if (e.getClick().isShiftClick()) {
            e.setCancelled(true);
            return;
        }

        // If clicking outside GUI, ignore
        if (raw >= e.getInventory().getSize()) return;

        // Block moving GUI panes / save button
        if (e.getCurrentItem() != null) {
            Material m = e.getCurrentItem().getType();
            if (m == Material.GRAY_STAINED_GLASS_PANE || m == Material.EMERALD) {
                e.setCancelled(true);
                return;
            }
        }

        // Top ability list (0..35): copy-to-cursor instead of moving
        if (raw >= 0 && raw <= 35) {
            String id = gui.readAbilityId(e.getCurrentItem());
            if (id != null) {
                e.setCancelled(true);
                if (e.getCursor() == null || e.getCursor().getType() == Material.AIR) {
                    // Put a clone on cursor
                    p.setItemOnCursor(e.getCurrentItem().clone());
                }
            }
            return;
        }

        // Bottom slots 45..49: allow placing/removing normally, but only abilities with id
        if (raw >= 45 && raw <= 49) {
            // allow normal Bukkit behavior
            return;
        }

        // Everything else in GUI: cancel (keeps UI clean)
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof AbilityLoadoutHolder)) return;

        // Block dragging into player inventory slots
        for (int raw : e.getRawSlots()) {
            if (raw >= e.getInventory().getSize()) {
                e.setCancelled(true);
                return;
            }
        }

        // Block dragging into GUI except bottom loadout slots (45..49)
        for (int raw : e.getRawSlots()) {
            if (raw <= 35 || raw == 53 || (raw >= 36 && raw <= 44) || raw >= 50) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
