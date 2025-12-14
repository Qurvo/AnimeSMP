package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.training.TrainingManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;

public class TrainingGuiListener implements Listener {

    private final TrainingManager trainingManager;

    public TrainingGuiListener(AnimeSMPPlugin plugin) {
        this.trainingManager = plugin.getTrainingManager();
    }

    private boolean isTrainingView(InventoryView view) {
        if (view == null) return false;
        String title = view.getTitle();
        if (title == null) return false;
        String stripped = ChatColor.stripColor(title);
        return stripped != null && stripped.startsWith("Training:");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (!isTrainingView(event.getView())) return;

        // prevent moving panes etc.
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();

        // Only clicks in top inventory matter
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        trainingManager.handleGuiClick(player, rawSlot);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        if (!isTrainingView(event.getView())) return;

        Player player = (Player) event.getPlayer();
        trainingManager.handleGuiClose(player);
    }
}
