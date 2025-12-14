package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.training.TrainingManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TrainingTokenListener implements Listener {

    private final TrainingManager trainingManager;

    public TrainingTokenListener(AnimeSMPPlugin plugin) {
        this.trainingManager = plugin.getTrainingManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only main hand to avoid double firing
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        if (!trainingManager.isTrainingToken(item)) return;

        // Handle token usage and cancel normal interaction
        event.setCancelled(true);
        trainingManager.handleTokenUse(player, item);
    }
}
