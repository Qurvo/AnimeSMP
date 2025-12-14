package com.animesmp.core.listeners;

import com.animesmp.core.AnimeSMPPlugin;
import com.animesmp.core.ability.Ability;
import com.animesmp.core.ability.AbilityManager;
import com.animesmp.core.player.PlayerProfile;
import com.animesmp.core.player.PlayerProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AbilityScrollListener implements Listener {

    private final AnimeSMPPlugin plugin;
    private final AbilityManager abilityManager;
    private final PlayerProfileManager profiles;

    public AbilityScrollListener(AnimeSMPPlugin plugin) {
        this.plugin = plugin;
        this.abilityManager = plugin.getAbilityManager();
        this.profiles = plugin.getProfileManager();
    }

    @EventHandler
    public void onScrollUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        // Only right-click actions
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ignore offhand to prevent double firing
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Ask AbilityManager to interpret this item as a scroll
        Ability ability = abilityManager.getAbilityFromScroll(item);
        if (ability == null) {
            return; // not an ability scroll
        }

        event.setCancelled(true); // prevent normal use

        PlayerProfile profile = profiles.getProfile(player);
        String id = ability.getId().toLowerCase();

        // Already unlocked? -> do NOT consume
        if (profile.hasUnlockedAbility(id)) {
            player.sendMessage(ChatColor.RED + "You already know this skill.");
            return;
        }

        // New unlock
        profile.unlockAbility(id);
        consumeOne(item, player);

        player.sendMessage(ChatColor.GREEN + "You have learned a new ability: "
                + ChatColor.AQUA + ability.getDisplayName() + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/bind"
                + ChatColor.YELLOW + " to bind it to a slot.");
    }

    /**
     * Safely remove one item from the player's main hand (or the stack used).
     */
    private void consumeOne(ItemStack item, Player player) {
        int amount = item.getAmount();
        if (amount <= 1) {
            if (player.getInventory().getItemInMainHand().equals(item)) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(0);
            }
        } else {
            item.setAmount(amount - 1);
        }
        player.updateInventory();
    }
}
